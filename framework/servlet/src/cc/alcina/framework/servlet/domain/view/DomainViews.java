package cc.alcina.framework.servlet.domain.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.view.DomainView;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request.Element;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform.Type;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.servlet.domain.view.DomainViews.ViewsTask.HandlerData;

@RegistryLocation(registryPoint = DomainViews.class, implementationType = ImplementationType.SINGLETON)
/**
 * TODO - logic of happens-before (task sequencing means any query executed
 * before a domain commit will be sequentially correct)
 * 
 * @author nick@alcina.cc
 *
 */
public abstract class DomainViews {
	public static DomainViews get() {
		return Registry.impl(DomainViews.class);
	}

	protected LiveListener liveListener;

	private Map<Key, LiveView> views = new ConcurrentHashMap<>();

	private Map<DomainTransformPersistenceEvent, Transaction> preCommitTransactions = new ConcurrentHashMap<>();

	private BlockingQueue<ViewsTask> tasks = new LinkedBlockingQueue<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean finished;

	private TaskProcessorThread thread;

	private ReentrantLock addTaskLock = new ReentrantLock();

	// runs on the DTR eventqueue thread
	private TopicListener<DomainTransformPersistenceEvent> beforeDomainCommittedListener = (
			k, e) -> {
		if (isIndexableTransformRequest(e)) {
			ViewsTask task = new ViewsTask();
			addTaskLock.lock();
			preCommitTransactions.put(e,
					Transaction.createSnapshotTransaction());
		}
	};

	private TopicListener<DomainTransformPersistenceEvent> afterDomainCommittedListener = (
			k, e) -> {
		if (isIndexableTransformRequest(e)) {
			ViewsTask task = new ViewsTask();
			task.modelChange.preCommit = preCommitTransactions.get(e);
			task.modelChange.postCommit = Transaction
					.createSnapshotTransaction();
			tasks.add(task);
			addTaskLock.unlock();
		}
	};

	public DomainViews() {
		liveListener = new LiveListener();
		DomainStore.writableStore().topicBeforeDomainCommitted()
				.add(beforeDomainCommittedListener);
		DomainStore.writableStore().topicAfterDomainCommitted()
				.add(afterDomainCommittedListener);
		thread = new TaskProcessorThread();
		thread.start();
	}

	// does *not* on the DTR eventqueue thread
	public Response handleRequest(
			Request<? extends DomainViewSearchDefinition> request) {
		ViewsTask task = new ViewsTask();
		task.type = ViewsTask.Type.HANDLE_REQUEST;
		addTaskLock.lock();
		task.handlerData.transaction = Transaction.createSnapshotTransaction();
		task.handlerData.request = request;
		tasks.add(task);
		addTaskLock.unlock();
		task.handlerData.await();
		return task.handlerData.response;
	}

	private void processRequest(ViewsTask task) {
		HandlerData handlerData = task.handlerData;
		Transaction.join(handlerData.transaction);
		Key key = new Key(handlerData.request);
		LiveView view = views.computeIfAbsent(key, LiveView::new);
		handlerData.response = view.generateResponse(handlerData.request);
		handlerData.response.setPosition(Transaction.current().getPosition());
		Transaction.end();
		handlerData.latch.countDown();
	}

	protected abstract Class<? extends Entity>[] getIndexableEntityClasses();

	protected boolean
			isIndexableTransformRequest(DomainTransformPersistenceEvent event) {
		return event.getTransformPersistenceToken().getTransformCollation()
				.has((Class[]) getIndexableEntityClasses());
	}

	void processEvent(ViewsTask task) {
		switch (task.type) {
		case MODEL_CHANGE:
			throw new UnsupportedOperationException();
		case HANDLE_REQUEST:
			processRequest(task);
			break;
		}
	}

	public static class GeneratingContext {
		public Map<TreePath, DomainViewNode> byPath;

		public Map<TreePath, Object> byPathModel;

		public Predicate<?> modelFilter = o -> true;

		DomainViewNode cursor = null;

		public DomainViewNode generate(Object object) {
			DomainViewNode node = Registry
					.impl(NodeGenerator.class, object.getClass())
					.generate(object, this);
			TreePath path = new TreePath();
			path.setParent(cursor == null ? null : cursor.getTreePath());
			if (object instanceof Entity) {
				path.setLocator(((Entity) object).toLocator());
			} else {
				path.putDiscriminator(object);
			}
			node.setTreePath(path);
			node.setParent(cursor);
			byPath.put(path, node);
			byPathModel.put(path, object);
			cursor = node;
			List<Object> childModels = Registry
					.impl(NodeGenerator.class, object.getClass())
					.getChildModels(node, this);
			for (Object childModel : childModels) {
				DomainViewNode child = generate(childModel);
				node.getChildren().add(child);
			}
			cursor = cursor.getParent();
			return node;
		}
	}

	public interface NodeGenerator<I, N extends DomainViewNode> {
		public N generate(I in, GeneratingContext context);

		public List<? extends Object> getChildModels(DomainViewNode node,
				GeneratingContext generatingContext);
	}

	public class TaskProcessorThread extends Thread {
		@Override
		public void run() {
			setName("DomainViews-task-queue-"
					+ EntityLayerUtils.getLocalHostName());
			while (!finished) {
				try {
					ViewsTask task = tasks.take();
					Transaction.begin();
					processEvent(task);
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					Transaction.end();
				}
			}
		}
	}

	private static class LiveListener implements DomainListener {
		@Override
		public Class<? extends Entity> getListenedClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void insert(Entity o) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove(Entity o) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException();
		}
	}

	static class Key {
		private Request request;

		private String stringKey;

		public Key(Request request) {
			this.request = request;
			this.stringKey = FlatTreeSerializer
					.serializeElided(request.getSearchDefinition());
		}

		@Override
		public boolean equals(Object anObject) {
			return this.stringKey.equals(anObject);
		}

		@Override
		public int hashCode() {
			return this.stringKey.hashCode();
		}
	}

	// TODO - sync get/modify
	static class LiveView {
		private DomainTransformCommitPosition earliestPosition;

		private DomainViewNode rootNode;

		Map<TreePath, DomainViewNode> byPath = new LinkedHashMap<>();

		Map<TreePath, Object> byPathModel = new LinkedHashMap<>();

		public LiveView(Key key) {
			earliestPosition = DomainStore.writableStore()
					.getTransformCommitPosition();
		}

		public Response generateResponse(
				Request<? extends DomainViewSearchDefinition> request) {
			Response response = new Response();
			response.setClearExisting(request.getSince() == null
					|| request.getSince().compareTo(earliestPosition) < 0);
			if (rootNode == null) {
				generateTree(request.getRoot().find(),
						new SearchPredicate(request.getSearchDefinition()));
			}
			for (Element element : request.getElements()) {
				response.getTransforms()
						.addAll(elementToTransform(element, request));
			}
			return response;
		}

		private List<Transform> elementToTransform(Element element,
				Request<? extends DomainViewSearchDefinition> request) {
			List<Transform> result = new ArrayList<>();
			TreePath path = element.getPath();
			DomainViewNode<?> node = byPath.get(path);
			if (node != null) {
				{
					Transform transform = new Transform();
					transform.setPath(path);
					transform.setNode(node);
					transform.setType(Type.APPEND);
					result.add(transform);
				}
				switch (element.getChildren()) {
				case IMMEDIATE_ONLY:
					for (DomainViewNode child : node.getChildren()) {
						{
							Transform transform = new Transform();
							transform.setPath(child.getTreePath());
							transform.setNode(child);
							transform.setType(Type.APPEND);
							result.add(transform);
						}
					}
				}
			}
			return result;
		}

		private void generateTree(DomainView rootEntity,
				Predicate searchPredicate) {
			GeneratingContext context = new GeneratingContext();
			context.modelFilter = searchPredicate;
			context.byPath = byPath;
			context.byPathModel = byPathModel;
			rootNode = context.generate(rootEntity);
		}

		public static class SearchPredicate implements Predicate {
			private DomainViewSearchDefinition searchDefinition;

			public SearchPredicate(
					DomainViewSearchDefinition searchDefinition) {
				this.searchDefinition = searchDefinition;
			}

			@Override
			public boolean test(Object t) {
				return true;
			}
		}
	}

	static class ViewsDeltaEvent {
		ModelChange modelChange = new ModelChange();

		PathChange pathChange = new PathChange();

		Type type;

		static class ModelChange {
			Transaction preCommit;

			Transaction postCommit;

			Request request;
		}

		static class PathChange {
			public Response response;// =view.generateResponse(request);

			public Request<? extends DomainViewSearchDefinition> request;

			public Transaction transaction;

			TreePath path;

			Type type;

			private CountDownLatch latch = new CountDownLatch(1);

			public void await() {
				// TODO Auto-generated method stub
			}

			static enum Type {
				ADD, REMOVE, CHANGE;
			}
		}

		static enum Type {
			MODEL_CHANGE, PATH_CHANGE, REQUEST;
		}
	}

	static class ViewsTask {
		ModelChange modelChange = new ModelChange();

		HandlerData handlerData = new HandlerData();

		Type type;

		static class HandlerData {
			public Response response;// =view.generateResponse(request);

			public Request<? extends DomainViewSearchDefinition> request;

			public Transaction transaction;

			private CountDownLatch latch = new CountDownLatch(1);

			public void await() {
				try {
					latch.await();
				} catch (Exception e) {
					return;
				}
			}
		}

		static class ModelChange {
			Transaction preCommit;

			Transaction postCommit;
		}

		static enum Type {
			MODEL_CHANGE, HANDLE_REQUEST;
		}
	}
}

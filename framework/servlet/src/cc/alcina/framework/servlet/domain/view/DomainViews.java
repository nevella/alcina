package cc.alcina.framework.servlet.domain.view;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
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
import cc.alcina.framework.servlet.domain.view.DomainViews.ViewsTask.Type;

@RegistryLocation(registryPoint = DomainViews.class, implementationType = ImplementationType.SINGLETON)
/**
 * TODO - document logic of happens-before (task sequencing means any query
 * executed before a domain commit will be sequentially correct)
 * 
 * @author nick@alcina.cc
 *
 */
public abstract class DomainViews {
	public static DomainViews get() {
		return Registry.impl(DomainViews.class);
	}

	protected LiveListener liveListener;

	private Map<Key, LiveTree> views = new ConcurrentHashMap<>();

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
			task.type = Type.MODEL_CHANGE;
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

	// does *not* run on the DTR eventqueue thread
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
		if (task.handlerData.exception == null) {
			return task.handlerData.response;
		} else {
			throw task.handlerData.exception;
		}
	}

	private void processRequest(ViewsTask task) {
		HandlerData handlerData = task.handlerData;
		try {
			Transaction.join(handlerData.transaction);
			Key key = new Key(handlerData.request);
			LiveTree view = views.computeIfAbsent(key, LiveTree::new);
			handlerData.response = view.generateResponse(handlerData.request);
			handlerData.response.setRequest(handlerData.request);
			handlerData.response
					.setPosition(Transaction.current().getPosition());
		} catch (RuntimeException e) {
			handlerData.exception = e;
		} finally {
			Transaction.end();
			handlerData.latch.countDown();
		}
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
			// throw new UnsupportedOperationException();
		case HANDLE_REQUEST:
			processRequest(task);
			break;
		}
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
					Transaction.ensureEnded();
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
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove(Entity o) {
		}

		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException();
		}
	}

	static class Key {
		Request<?> request;

		private String stringKey;

		public Key(Request<?> request) {
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

	static class ViewsTask {
		ModelChange modelChange = new ModelChange();

		HandlerData handlerData = new HandlerData();

		Type type;

		static class HandlerData {
			public RuntimeException exception;

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

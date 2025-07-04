package cc.alcina.framework.servlet.domain.view;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.view.DomainView;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.WaitPolicy;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.QueryResult;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.Permissions.LoginState;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.servlet.domain.view.DomainViews.ViewsTask.HandlerData;
import cc.alcina.framework.servlet.domain.view.DomainViews.ViewsTask.Type;

/**
 * Notes: adjunct transforms are processed on current thread (and cleaned up)
 *
 * FIXME - livetree - make that a little prettier
 *
 * 
 *
 */
@Registration.Singleton
public abstract class DomainViews {
	public static DomainViews get() {
		return Registry.impl(DomainViews.class);
	}

	protected LiveListener liveListener;

	private ConcurrentHashMap<Key, LiveTree> trees = new ConcurrentHashMap<>();

	private ConcurrentHashMap<DomainTransformPersistenceEvent, Transaction> preCommitTransactions = new ConcurrentHashMap<>();

	private BlockingQueue<ViewsTask> tasks = new LinkedBlockingQueue<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean finished;

	private TaskProcessorThread thread;

	private ReentrantLock addTaskLock = new ReentrantLock();

	boolean addTaskLockLockedByDomainCommit;

	// runs on the DTR eventqueue thread
	private TopicListener<DomainTransformPersistenceEvent> beforeDomainCommittedListener = e -> {
		if (isIndexableTransformRequest(e)) {
			ViewsTask task = new ViewsTask();
			addTaskLock.lock();
			// to workaround not explicitly controlling the try/finally of
			// addTaskLock.lock();
			addTaskLockLockedByDomainCommit = true;
			preCommitTransactions.put(e,
					Transaction.createSnapshotTransaction());
		}
	};

	private TopicListener<DomainTransformPersistenceEvent> afterDomainCommittedListener = e -> {
		Transaction preCommit = preCommitTransactions.remove(e);
		boolean indexableTransformRequest = isIndexableTransformRequest(e);
		if (indexableTransformRequest
				&& Transaction.current().isToDomainCommitted()) {
			ViewsTask task = new ViewsTask();
			task.type = Type.MODEL_CHANGE;
			task.modelChange.preCommit = preCommit;
			task.modelChange.event = e;
			task.modelChange.postCommit = Transaction
					.createSnapshotTransaction();
			tasks.add(task);
		}
		if (addTaskLockLockedByDomainCommit) {
			addTaskLock.unlock();
			addTaskLockLockedByDomainCommit = false;
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

	public void clearTree(EntityLocator viewLocator) {
		trees.keySet()
				.removeIf(k -> Objects.equals(k.viewLocator, viewLocator));
	}

	public void clearTrees() {
		trees.clear();
	}

	protected boolean filterViewTransformCollation(QueryResult qr) {
		return true;
	}

	protected abstract Class<? extends Entity>[] getIndexableEntityClasses();

	// does *not* run on the DTR eventqueue thread
	public Response handleRequest(
			Request<? extends DomainViewSearchDefinition> request) {
		ViewsTask task = new ViewsTask();
		task.type = ViewsTask.Type.HANDLE_PATH_REQUEST;
		task.handlerData.request = request;
		if (Transaction.current().isNonCommital()) {
			processEvent(task);
		} else {
			queueTask(task);
		}
		task.await();
		if (task.handlerData.exception == null) {
			if (task.handlerData.response != null
					&& task.handlerData.response.isDelayBeforeReturn()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					//
				}
			}
			return task.handlerData.response;
		} else {
			throw task.handlerData.exception;
		}
	}

	protected boolean
			isIndexableTransformRequest(DomainTransformPersistenceEvent event) {
		return Configuration.is("indexTransformRequests")
				&& event.getTransformPersistenceToken().getTransformCollation()
						.has((Class[]) getIndexableEntityClasses());
	}

	protected void onViewModified(DomainView view) {
		Iterator<Entry<Key, LiveTree>> itr = trees.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<Key, LiveTree> entry = itr.next();
			if (entry.getKey().request.getRoot().equals(view.toLocator())) {
				entry.getValue().onInvalidateTree();
				itr.remove();
				logger.info("Invalidated tree for view {}", view);
			}
		}
	}

	private void processCheckWaits(ViewsTask task) {
		trees.values().forEach(LiveTree::checkChangeListeners);
	}

	synchronized void processEvent(ViewsTask task) {
		switch (task.type) {
		case MODEL_CHANGE:
			logger.info(
					"Processing domainviews request - {} - queue length: {}",
					task.modelChange.event.getMaxPersistedRequestId(),
					tasks.size());
			Collection<LiveTree> liveTrees = trees.values();
			Transaction.end();
			Transaction.join(task.modelChange.preCommit);
			/*
			 * Ordering - we have to invalidate trees *first* - so they fire a
			 * full invalidation event rather than partial
			 *
			 * In particular, the client relies on dtr position to know if it
			 * should refresh - but the dtr pos of freshly generated,
			 * invalidated tree will be the same as that of transforms affecting
			 * the 'old' tree. So the client can't use logic 'my dtrpos is
			 * earlier than the earliest of the new tree' - because it isn't.
			 *
			 * It could use 'my tree first dtr pos is earlier than the server
			 * tree earliest pos' - but that would require sending two over the
			 * wire. So this way is better (invalidate trees first).
			 */
			task.modelChange.event.getTransformPersistenceToken()
					.getTransformCollation()
					.query(PersistentImpl.getImplementation(DomainView.class))
					.stream().
					// don't bother processing if about to delete
					filter(QueryResult::hasNoDeleteTransform)
					// the entity will not exist, yet, if the entitycollation
					// contains a creation (i.e. the entity was created during
					// the modelChange.event)
					.filter(QueryResult::hasNoCreateTransform)
					.filter(this::filterViewTransformCollation)
					.forEach(qr -> onViewModified(qr.<DomainView> getEntity()));
			liveTrees
					.forEach(tree -> tree.index(task.modelChange.event, false));
			Transaction.end();
			Transaction.join(task.modelChange.postCommit);
			liveTrees.forEach(tree -> tree.index(task.modelChange.event, true));
			Transaction.end();
			break;
		// throw new UnsupportedOperationException();
		case HANDLE_PATH_REQUEST:
			processRequest(task);
			break;
		case HANDLE_LAMBDA:
			processLambda(task);
			break;
		case EVICT_LISTENERS:
			processCheckWaits(task);
			break;
		}
	}

	private void processLambda(ViewsTask task) {
		HandlerData handlerData = task.handlerData;
		try {
			Transaction.end();
			Transaction.join(handlerData.transaction);
			LiveTree view = null;
			if (handlerData.request != null) {
				Key key = new Key(handlerData.request);
				view = trees.computeIfAbsent(key, LiveTree::new);
			}
			task.handlerData.lambdaResult = task.handlerData.lambda.apply(view);
		} catch (RuntimeException e) {
			handlerData.exception = e;
		} finally {
			Transaction.end();
			task.latch.countDown();
		}
	}

	private void processRequest(ViewsTask task) {
		HandlerData handlerData = task.handlerData;
		boolean awaitingTask = false;
		Key key = null;
		try {
			if (!Transaction.current().isNonCommital()) {
				Transaction.end();
				Transaction.join(handlerData.transaction);
			}
			key = new Key(handlerData.request);
			LiveTree tree = trees.get(key);
			if (handlerData.request
					.getWaitPolicy() == WaitPolicy.CANCEL_WAITS) {
				handlerData.response = new Response();
				handlerData.response.setRequest(handlerData.request);
				handlerData.response.setNoChangeListener(true);
				if (tree == null) {
					return;
				} else {
					tree.cancelChangeListeners(handlerData.clientInstanceId,
							handlerData.request.getWaitId());
				}
				return;
			}
			if (handlerData.request
					.getWaitPolicy() == WaitPolicy.WAIT_FOR_DELTAS) {
				Preconditions.checkNotNull(handlerData.request.getSince());
			}
			// if waitpolicy == wait_for_deltas but tree does not exist or
			// already has deltas, fall straight through
			if (handlerData.request
					.getWaitPolicy() == WaitPolicy.WAIT_FOR_DELTAS
					&& tree != null
					&& !tree.hasDeltasSince(handlerData.request.getSince())) {
				LiveTree f_tree = tree;
				tree.addChangeListener(task);
				awaitingTask = true;
				return;
			}
			tree = trees.computeIfAbsent(key, LiveTree::new);
			handlerData.response = tree.generateResponse(handlerData.request);
		} catch (RuntimeException e) {
			handlerData.exception = e;
		} finally {
			if (!Transaction.current().isNonCommital()) {
				Transaction.end();
			} else {
				if (key != null) {
					trees.remove(key);
				}
			}
			if (!awaitingTask) {
				task.latch.countDown();
			}
		}
	}

	void queueTask(ViewsTask task) {
		try {
			addTaskLock.lock();
			Transaction currentTransaction = Transaction.current();
			task.handlerData.transaction = currentTransaction.isNonCommital()
					? currentTransaction
					: Transaction.createSnapshotTransaction();
			tasks.add(task);
		} finally {
			addTaskLock.unlock();
		}
	}

	public void shutdown() {
		finished = true;
		tasks.add(new ViewsTask());
	}

	public <T> T submitLambda(
			Request<? extends DomainViewSearchDefinition> request,
			Function<LiveTree, T> lambda) {
		ViewsTask task = new ViewsTask();
		task.type = Type.HANDLE_LAMBDA;
		task.handlerData.request = request;
		task.handlerData.lambda = (Function<LiveTree, Object>) lambda;
		queueTask(task);
		task.await();
		if (task.handlerData.exception == null) {
			return (T) task.handlerData.lambdaResult;
		} else {
			throw task.handlerData.exception;
		}
	}

	public void submitLambda(Runnable withViewsModelLambda) {
		ViewsTask task = new ViewsTask();
		task.type = Type.HANDLE_LAMBDA;
		task.handlerData.lambda = tree -> {
			withViewsModelLambda.run();
			return null;
		};
		queueTask(task);
		task.await();
		if (task.handlerData.exception == null) {
			return;
		} else {
			throw task.handlerData.exception;
		}
	}

	public <V extends DomainView> List<V> viewsContaining(Entity e) {
		// Concurrency - only allowed from event queue thread
		Preconditions.checkState(Thread.currentThread() == thread);
		return (List) trees.values().stream().filter(t -> t.containsEntity(e))
				.map(t -> t.rootEntity).collect(Collectors.toList());
	}

	public void waitForEmptyQueue() {
		try {
			Thread.sleep(30);
			// FIXME - index
			// can't get more naive than that now...
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static class Key {
		Request<?> request;

		private String stringKey;

		EntityLocator viewLocator;

		public Key(Request<?> request) {
			this.request = request;
			this.viewLocator = request.getRoot();
			this.stringKey = request.getRoot().toString();
		}

		@Override
		public boolean equals(Object anObject) {
			return anObject instanceof Key
					? this.stringKey.equals(((Key) anObject).stringKey)
					: false;
		}

		@Override
		public int hashCode() {
			return this.stringKey.hashCode();
		}

		@Override
		public String toString() {
			return this.stringKey;
		}
	}

	private static class LiveListener implements DomainListener {
		@Override
		public Class<?> getListenedClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void insert(Object o) {
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove(Object o) {
		}

		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException();
		}
	}

	public class TaskProcessorThread extends Thread {
		@Override
		public void run() {
			setName("DomainViews-task-queue-"
					+ EntityLayerUtils.getLocalHostName());
			long lastEvictSubmit = 0;
			while (true) {
				try {
					LooseContext.pushWithTrue(
							JPAImplementation.CONTEXT_USE_DOMAIN_QUERIES);
					ViewsTask task = tasks.poll(1, TimeUnit.SECONDS);
					if (finished) {
						return;
					}
					if (task != null) {
						try {
							Permissions.pushUser(task.user, task.loginState);
							Transaction.begin();
							processEvent(task);
						} finally {
							Transaction.ensureEnded();
							Permissions.popContext();
						}
					}
					long now = System.currentTimeMillis();
					if (now - lastEvictSubmit > 1
							* TimeConstants.ONE_SECOND_MS) {
						lastEvictSubmit = now;
						ViewsTask submit = new ViewsTask();
						submit.type = ViewsTask.Type.EVICT_LISTENERS;
						// ordering of addTaskLock not required
						tasks.add(submit);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					try {
						LooseContext.pop();
					} catch (RuntimeException e) {
						if (!finished) {
							throw e;
						}
					}
				}
			}
		}
	}

	static class ViewsTask {
		public LoginState loginState;

		ModelChange modelChange = new ModelChange();

		HandlerData handlerData = new HandlerData();

		Type type;

		IUser user;

		CountDownLatch latch = new CountDownLatch(1);

		public ViewsTask() {
			loginState = Permissions.get().getLoginState();
			user = Permissions.get().getUser();
		}

		void await() {
			try {
				latch.await();
			} catch (Exception e) {
				return;
			}
		}

		@Override
		public String toString() {
			return type.toString();
		}

		static class HandlerData {
			long clientInstanceId;

			Function<LiveTree, Object> lambda;

			Object lambdaResult;

			RuntimeException exception;

			Response response;

			Request<? extends DomainViewSearchDefinition> request;

			Transaction transaction;

			boolean noChangeListeners;

			boolean clearExisting;
		}

		static class ModelChange {
			DomainTransformPersistenceEvent event;

			Transaction preCommit;

			Transaction postCommit;
		}

		static enum Type {
			MODEL_CHANGE, HANDLE_PATH_REQUEST, HANDLE_LAMBDA, EVICT_LISTENERS
		}
	}
}

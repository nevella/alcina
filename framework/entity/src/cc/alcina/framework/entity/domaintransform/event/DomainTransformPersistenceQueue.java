package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPositionProvider;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreTransformSequencer;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.GraphProjection;

/**
 * Improvement: rather than a strict dtrp-id queue, use 'happens after' field of
 * dtrp to allow out-of-sequence publishing
 * 
 * @author nick@alcina.cc
 *
 */
public class DomainTransformPersistenceQueue {
	public static final String CONTEXT_WAIT_TIMEOUT_MS = DomainTransformPersistenceQueue.class
			.getName() + ".CONTEXT_WAIT_TIMEOUT_MS";

	final Logger logger = LoggerFactory.getLogger(getClass());

	Set<Long> firingLocalToVm = new LinkedHashSet<>();

	// most recent event
	Set<Long> lastFired = new LinkedHashSet<>();

	Set<Long> appLifetimeEventsFired = new LinkedHashSet<>();

	Set<Long> firedOrQueued = new LinkedHashSet<>();

	LinkedList<Long> toFire = new LinkedList<>();

	Object queueModificationLock = new Object();

	AtomicInteger waiterCounter = new AtomicInteger();

	AtomicBoolean closed = new AtomicBoolean(false);

	CountDownLatch waiterLatch;

	private FireEventsThread eventQueue;

	private DomainTransformPersistenceEvents persistenceEvents;

	private Thread firingThread = null;

	public DomainTransformPersistenceQueue(
			DomainTransformPersistenceEvents persistenceEvents) {
		this.persistenceEvents = persistenceEvents;
	}

	public void appShutdown() {
		closed.set(true);
		synchronized (queueModificationLock) {
			queueModificationLock.notifyAll();
		}
		synchronized (toFire) {
			toFire.notifyAll();
		}
	}

	public Thread getFireEventsThread() {
		return eventQueue;
	}

	public Thread getFiringThread() {
		return this.firingThread;
	}

	public int getToFireQueueLength() {
		synchronized (queueModificationLock) {
			return toFire.size();
		}
	}

	public DomainTransformCommitPosition getTransformLogPosition() {
		synchronized (queueModificationLock) {
			return new DomainTransformCommitPosition(
					CommonUtils.first(lastFired), lastFired.size(), null);
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		synchronized (queueModificationLock) {
			firingLocalToVm.add(dtrp.getId());
		}
	}

	public void sequencedTransformRequestPublished() {
		List<Long> sequentialUnpublishedTransformIds = persistenceEvents.domainStore
				.getTransformSequencer().getSequentialUnpublishedTransformIds();
		fireSequentialUnpublishedTransformIds(
				sequentialUnpublishedTransformIds);
	}

	public void startEventQueue() {
		eventQueue = new FireEventsThread();
		eventQueue.start();
	}

	public String toDebugString() {
		synchronized (queueModificationLock) {
			return GraphProjection.fieldwiseToString(this);
		}
	}

	public void transformRequestPublished(Long id) {
		if (persistenceEvents.isUseTransformDbCommitSequencing()) {
			sequencedTransformRequestPublished();
		} else {
			transformRequestPublishedSequential(id);
		}
	}

	public void waitUntilAllQueuedEventsProcessed() {
		Optional<Long> lastToFireId = getLastToFireId();
		if (!lastToFireId.isPresent()) {
			return;
		} else {
			new QueueWaiter().pauseUntilProcessed(
					5 * TimeConstants.ONE_MINUTE_MS, lastToFireId);
		}
	}

	public void waitUntilCurrentRequestsProcessed() {
		waitUntilCurrentRequestsProcessed(60 * TimeConstants.ONE_SECOND_MS);
	}

	public void waitUntilCurrentRequestsProcessed(long timeoutMs) {
		new QueueWaiter().pauseUntilProcessed(timeoutMs, Optional.empty());
	}

	public void waitUntilRequestProcessed(String logOffset) {
		long timeoutMs = 60 * TimeConstants.ONE_SECOND_MS;
		if (LooseContext.has(CONTEXT_WAIT_TIMEOUT_MS)) {
			timeoutMs = LooseContext.get(CONTEXT_WAIT_TIMEOUT_MS);
		}
		/*
		 * 'event' in this class means
		 * "domaintransformrequest of id x were fired" - eventId is the dtrp id
		 */
		long eventId = Long.parseLong(logOffset);
		long startTime = System.currentTimeMillis();
		while (true) {
			long timeRemaining = -System.currentTimeMillis() + startTime
					+ timeoutMs;
			synchronized (queueModificationLock) {
				try {
					if (eventId == 0 || appLifetimeEventsFired.contains(eventId)
							|| timeRemaining <= 0) {
						break;
					}
					queueModificationLock.wait(timeRemaining);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}

	public void waitUntilToFireQueueEmpty() {
		while (true) {
			int size = 0;
			synchronized (queueModificationLock) {
				size = toFire.size();
				if (size < 10) {
					return;
				}
			}
			try {
				Thread.sleep(1000);
				logger.warn("Waiting for toFire queue to (mostly) empty [{}]",
						size);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private DomainTransformPersistenceEvent
			createPersistenceEventFromPersistedRequest(
					DomainTransformRequestPersistent dtrp) {
		// create an "event" to publish in the queue
		TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
				dtrp, null, Registry.impl(TransformLoggingPolicy.class), false,
				false, false, null, true);
		DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper(
				persistenceToken);
		List<DomainTransformEventPersistent> events = new ArrayList<DomainTransformEventPersistent>(
				(List) dtrp.getEvents());
		wrapper.persistentEvents = events;
		wrapper.persistentRequests = new ArrayList<>(Arrays.asList(dtrp));
		DomainTransformResponse dtr = new DomainTransformResponse();
		dtr.setRequestId(persistenceToken.getRequest().getRequestId());
		dtr.setTransformsProcessed(events.size());
		dtr.setResult(DomainTransformResponseResult.OK);
		dtr.setRequest(persistenceToken.getRequest());
		wrapper.response = dtr;
		DomainTransformPersistenceEvent persistenceEvent = new DomainTransformPersistenceEvent(
				persistenceToken, wrapper, false);
		return persistenceEvent;
	}

	private void fireSequentialUnpublishedTransformIds(
			List<Long> sequentialUnpublishedTransformIds) {
		for (Long sequentialId : sequentialUnpublishedTransformIds) {
			transformRequestPublishedSequential(sequentialId);
		}
	}

	private Optional<Long> getLastToFireId() {
		synchronized (queueModificationLock) {
			return Optional.<Long> ofNullable(CommonUtils.last(toFire));
		}
	}

	private Logger getLogger(boolean localToVm) {
		return localToVm ? logger : eventQueue.fireEventThreadLogger;
	}

	// private <T> T
	// runWithDisabledObjectPermissions(ThrowingSupplier<T> supplier) {
	// try {
	// // this prevents a deadlock where we might have a waiting write
	// // preventing us from getting the lock
	// LooseContext.pushWithTrue(DomainStore.CONTEXT_NO_LOCKS);
	// ThreadedPermissionsManager.cast().pushSystemUser();
	// PermissibleFieldFilter
	// .setDisabledPerThreadPerObjectPermissions(true);
	// return supplier.get();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// PermissibleFieldFilter
	// .setDisabledPerThreadPerObjectPermissions(false);
	// ThreadedPermissionsManager.cast().popSystemUser();
	// LooseContext.pop();
	// }
	// }
	private void transformRequestPublishedSequential(long id) {
		synchronized (queueModificationLock) {
			if (firedOrQueued.contains(id)) {
				return;
			} else {
				firedOrQueued.add(id);
				synchronized (toFire) {
					toFire.add(id);
					toFire.notify();
				}
			}
		}
	}

	protected CommonPersistenceLocal getCommonPersistence() {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence();
	}

	void logFired(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return;
		}
		getLogger(event.isLocalToVm()).info("fired - {} - {} events - range {}",
				event.getTransformPersistenceToken().getRequest().shortId(),
				event.getTransformPersistenceToken().getRequest().getEvents()
						.size(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
		synchronized (queueModificationLock) {
			lastFired = new LinkedHashSet<>(event.getPersistedRequestIds());
			appLifetimeEventsFired.addAll(lastFired);
			waiterLatch = new CountDownLatch(waiterCounter.get());
			queueModificationLock.notifyAll();
		}
		try {
			waiterLatch.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
		firingThread = null;
	}

	void logFiring(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return;
		}
		firingThread = Thread.currentThread();
		Logger logger = getLogger(event.isLocalToVm());
		logger.info("firing - {} - {} - {} events - range {}",
				Ax.friendly(event.getPersistenceEventType()),
				event.getTransformPersistenceToken().getRequest().shortId(),
				event.getTransformPersistenceToken().getRequest().getEvents()
						.size(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
	}

	void transformRequestFinishedFiring(long id) {
		synchronized (queueModificationLock) {
			firedOrQueued.add(id);
			lastFired.add(id);
			firingLocalToVm.remove(id);
		}
	}

	void transformRequestQueued(long id) {
		synchronized (queueModificationLock) {
			firedOrQueued.add(id);
		}
	}

	@RegistryLocation(registryPoint = DomainTransformCommitPositionProvider.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
	public static class DomainTransformCommitPositionProvider_EventsQueue
			extends DomainTransformCommitPositionProvider {
		private DomainTransformPersistenceQueue queue;

		@Override
		public DomainTransformCommitPosition getPosition() {
			if (queue == null) {
				queue = DomainStore.writableStore().getPersistenceEvents()
						.getQueue();
			}
			return queue.getTransformLogPosition();
		}
	}

	public class FireEventsThread extends Thread {
		Logger fireEventThreadLogger = LoggerFactory.getLogger(getClass());

		Map<Long, DomainTransformRequestPersistent> loadedRequests = new LinkedHashMap<>();

		@Override
		public void run() {
			setName(Ax.format("DomainTransformPersistenceQueue-fire::%s",
					persistenceEvents.domainStore.name));
			while (!closed.get()) {
				try {
					Long id = null;
					synchronized (toFire) {
						// double-check
						if (closed.get()) {
							break;
						}
						if (toFire.isEmpty()) {
							toFire.wait();
						}
						if (!toFire.isEmpty()) {
							id = toFire.pop();
						}
					}
					if (id != null && !closed.get()) {
						try {
							Transaction.ensureBegun();
							ThreadedPermissionsManager.cast().pushSystemUser();
							publishTransformEvent(id);
						} finally {
							Transaction.ensureBegun();
							ThreadedPermissionsManager.cast().popSystemUser();
							Transaction.ensureEnded();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void publishTransformEvent(long id) {
			boolean local = false;
			fireEventThreadLogger.info("publishTransformEvent - dtr {}", id);
			synchronized (queueModificationLock) {
				local = firingLocalToVm.contains(id);
			}
			if (local) {
				DomainStoreTransformSequencer transformSequencer = DomainStore
						.writableStore().getTransformSequencer();
				transformSequencer.removePreLocalNonFireEventsThreadBarrier(id);
				// transforming thread will now fire...
				// following call may happen after transformingthread has
				// removed the barrier, so check there
				transformSequencer.waitForPostLocalFireEventsThreadBarrier(id);
				return;
			} else {
				try {
					DomainTransformRequestPersistent request = null;
					ensureRequestsLoaded(id);
					request = loadedRequests.get(id);
					if (request != null) {
						if (Ax.isTest() && request.getClientInstance() != null
								&& request.getClientInstance()
										.getId() == PermissionsManager.get()
												.getClientInstanceId()) {
							// local persisted via server
							DomainStoreTransformSequencer transformSequencer = DomainStore
									.writableStore().getTransformSequencer();
							transformSequencer
									.removePreLocalNonFireEventsThreadBarrier(
											id);
							transformSequencer
									.waitForPostLocalFireEventsThreadBarrier(
											id);
							return;
						}
						Transaction.current().toNoActiveTransaction();
						DomainTransformPersistenceEvent event = createPersistenceEventFromPersistedRequest(
								request);
						event.ensureTransformsValidForVm();
						persistenceEvents
								.fireDomainTransformPersistenceEvent(event);
						loadedRequests.remove(id);
					} else {
						fireEventThreadLogger.warn(
								"publishTransformEvent - missed (no transforms?) dtr {}",
								id);
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				} finally {
					Transaction.ensureEnded();
				}
			}
		}

		void ensureRequestsLoaded(long ensureId) throws Exception {
			Set<Long> idsRequired = new LinkedHashSet<>();
			idsRequired.add(ensureId);
			synchronized (toFire) {
				toFire.stream().filter(id -> !loadedRequests.containsKey(id))
						.forEach(idsRequired::add);
			}
			if (!idsRequired.isEmpty()) {
				List<DomainTransformRequestPersistent> requests = null;
				requests = persistenceEvents.domainStore.loadTransformRequests(
						idsRequired, fireEventThreadLogger);
				if (Ax.isTest()) {
					List<DomainTransformRequestPersistent> extraRequestData = CommonPersistenceProvider
							.get().getCommonPersistence()
							.getPersistentTransformRequests(0, 0, idsRequired,
									false, true, null);
					List<DomainTransformRequestPersistent> f_requests = requests;
					extraRequestData.forEach(r -> {
						f_requests.stream()
								.filter(r2 -> r2.getId() == r.getId())
								.forEach(r2 -> {
									r2.setRequestId(r.getRequestId());
									r2.setClientInstance(r.getClientInstance());
								});
					});
				}
				requests.forEach(r -> loadedRequests.put(r.getId(), r));
			}
		}
	}

	class QueueWaiter {
		private Set<Long> waiting;

		public void pauseUntilProcessed(long timeoutMs,
				Optional<Long> requestId) {
			if (requestId.isPresent()) {
				waiting = Stream.of(requestId.get())
						.collect(Collectors.toSet());
			} else {
				synchronized (queueModificationLock) {
					waiting = new LinkedHashSet<>(firingLocalToVm);
				}
			}
			long startTime = System.currentTimeMillis();
			while (true) {
				long timeRemaining = -System.currentTimeMillis() + startTime
						+ timeoutMs;
				synchronized (queueModificationLock) {
					try {
						if (waiting.isEmpty() || timeRemaining <= 0) {
							break;
						}
						waiterCounter.incrementAndGet();
						queueModificationLock.wait(timeRemaining);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
				waiting.removeAll(lastFired);
				waiterCounter.decrementAndGet();
				waiterLatch.countDown();
			}
		}
	}
}

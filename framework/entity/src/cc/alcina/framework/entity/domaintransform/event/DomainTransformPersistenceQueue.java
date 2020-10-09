package cc.alcina.framework.entity.domaintransform.event;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreTransformSequencer;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreTransformSequencer.TransformSequenceEntry;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.GraphProjection;

/**
 * Improvement: rather than a strict dtrp-id queue, use 'happens after' field of
 * dtrp to allow out-of-sequence publishing
 * 
 * 
 * TODO: doc :
 * 
 * how this interacts with ClusterTransformListener how the threading works
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

	AtomicBoolean closed = new AtomicBoolean(false);

	private FireEventsThread eventQueue;

	private DomainTransformPersistenceEvents persistenceEvents;

	private Thread firingThread = null;

	private Map<Long, DomainTransformRequestPersistent> loadedRequests = new ConcurrentHashMap<>();

	private Timestamp muteEventsOnOrBefore;

	private Map<Long, TransformSequenceEntry> requestIdSequenceEntry = new ConcurrentHashMap<>();

	// used to signal this event to a possibly waiting fire events thread
	private Object persistentRequestCached = new Object();

	private Set<QueueWaiter> waiters = Collections
			.synchronizedSet(new LinkedHashSet<>());

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

	public void
			cachePersistedRequest(DomainTransformRequestPersistent request) {
		long requestId = request.getId();
		// we used to check if this request had already been fired - but the
		// interaction with publish event was complex, it's *very unlikely* that
		// the request won't make it within the 10 sec timeout, and the memory
		// effects of the (improbable) unused requests are very small. So no
		// checking
		logger.info("Cached request: {}", requestId);
		loadedRequests.put(requestId, request);
		synchronized (persistentRequestCached) {
			persistentRequestCached.notifyAll();
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

	public synchronized void sequencedTransformRequestPublished(Long id) {
		List<TransformSequenceEntry> unpublishedRequests = persistenceEvents.domainStore
				.getTransformSequencer().getSequentialUnpublishedRequests();
		logger.debug("Received dtr published: {} ==> sequenced: {} ", id,
				unpublishedRequests);
		unpublishedRequests.forEach(entry -> requestIdSequenceEntry
				.put(entry.persistentRequestId, entry));
		fireSequentialUnpublishedTransformRequests(unpublishedRequests.stream()
				.map(e -> e.persistentRequestId).collect(Collectors.toList()));
	}

	public void setMuteEventsOnOrBefore(
			Timestamp highestVisibleTransactionTimestamp) {
		this.muteEventsOnOrBefore = highestVisibleTransactionTimestamp;
	}

	public void startEventQueue() {
		synchronized (queueModificationLock) {
			firedOrQueued.forEach(id -> {
				if (loadedRequests.get(id) == null) {
					logger.warn("Loading request from db: {}", id);
					DomainTransformRequestPersistent persistentRequest = persistenceEvents.domainStore
							.loadTransformRequest(id);
					if (persistentRequest != null) {
						loadedRequests.put(id, persistentRequest);
					}
				}
			});
		}
		eventQueue = new FireEventsThread();
		eventQueue.start();
	}

	public String toDebugString() {
		synchronized (queueModificationLock) {
			return GraphProjection.fieldwiseToString(this, true, false, 9999,
					"loadedRequests");
		}
	}

	public void transformRequestPublished(Long id) {
		if (persistenceEvents.isUseTransformDbCommitSequencing()) {
			sequencedTransformRequestPublished(id);
		} else {
			transformRequestPublishedSequential(id);
		}
	}

	public void waitUntilCurrentRequestsProcessed() {
		waitUntilCurrentRequestsProcessed(60 * TimeConstants.ONE_SECOND_MS);
	}

	public void waitUntilCurrentRequestsProcessed(long timeoutMs) {
		new QueueWaiter(timeoutMs, Optional.empty()).waitForProcessedRequests();
	}

	public void waitUntilEventQueueIsEmpty() {
		while (true) {
			QueueWaiter queueWaiter = null;
			Optional<Long> lastToFireId = getLastToFireId();
			if (!lastToFireId.isPresent()) {
				return;
			} else {
				new QueueWaiter(365 * TimeConstants.ONE_DAY_MS, lastToFireId)
						.waitForProcessedRequests();
			}
		}
	}

	public void waitUntilRequestProcessed(long requestId, long timeoutMs) {
		new QueueWaiter(timeoutMs, Optional.of(requestId))
				.waitForProcessedRequests();
	}

	private DomainTransformPersistenceEvent
			createPersistenceEventFromPersistedRequest(
					DomainTransformRequestPersistent dtrp) {
		// create an "event" to publish in the queue
		TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
				dtrp, null, false, false, false, null, true);
		DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper(
				persistenceToken);
		List<DomainTransformEventPersistent> events = new ArrayList<DomainTransformEventPersistent>(
				(List) dtrp.getEvents());
		// cloning issue?
		wrapper.persistentEvents = new ArrayList<>(events);
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

	private void fireSequentialUnpublishedTransformRequests(
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

	private Timestamp
			getTransactionCommitTime(DomainTransformRequestPersistent request) {
		if (persistenceEvents.isUseTransformDbCommitSequencing()) {
			// Guaranteed to exist at this point. Not needed after first get -
			// in for tmp debugging (FIXME mvcc.4)
			return requestIdSequenceEntry.get(request.getId()).commitTimestamp;
		} else {
			// more or less, more or les...(ensure the callee doesn't block)
			return new Timestamp(System.currentTimeMillis());
		}
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
			for (QueueWaiter waiter : waiters) {
				persistedRequestIds.forEach(waiter::notifyFired);
			}
			queueModificationLock.notifyAll();
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

		@Override
		public void run() {
			setName(Ax.format("DomainTransformPersistenceQueue-fire::%s",
					persistenceEvents.domainStore.name));
			while (true) {
				try {
					Long id = null;
					if (getToFireQueueLength() == 0) {
						synchronized (toFire) {
							toFire.wait();
							if (closed.get()) {
								return;
							}
						}
					}
					if (getToFireQueueLength() > 0) {
						synchronized (queueModificationLock) {
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
					request = loadedRequests.get(id);
					if (request == null) {
						/*
						 * 'Exists' seems ... to return false when should be
						 * true, in a few cases
						 * 
						 * SQL tx issue? In any case, just advisory for now
						 */
						boolean exists = persistenceEvents.domainStore
								.checkTransformRequestExists(id);
						fireEventThreadLogger.warn(
								"publishTransformEvent - no loaded request -  dtr {} - exists {}",
								id, exists);
						// if (exists) {
						// Using cluster queueing - an event is visible in
						// the db for the cluster propagation notification
						// (kafka) has arrived.
						//
						// Wait a maximum of 10 seconds
						long endWaitLoop = System.currentTimeMillis()
								+ 10 * TimeConstants.ONE_SECOND_MS;
						while (true) {
							request = loadedRequests.get(id);
							if (request != null) {
								fireEventThreadLogger.warn(
										"publishTransformEvent - loaded request during wait - dtr {} ",
										id);
							}
							long now = System.currentTimeMillis();
							if (request == null && now < endWaitLoop) {
								synchronized (persistentRequestCached) {
									persistentRequestCached
											.wait(endWaitLoop - now);
								}
							} else {
								break;
							}
						}
						if (request == null) {
							fireEventThreadLogger.warn(
									"publishTransformEvent - no loaded request - last try -  dtr {} - exists {}",
									id, exists);
							request = loadedRequests.get(id);
						}
						// }
						if (request == null) {
							fireEventThreadLogger.warn(
									"publishTransformEvent - loading request not received via cluster listener -  dtr {}",
									id);
							DomainTransformRequestPersistent loadedRequest = persistenceEvents.domainStore
									.loadTransformRequest(id);
							if (loadedRequest != null) {
								loadedRequests.put(id, loadedRequest);
								request = loadedRequest;
							}
						}
					}
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
						Timestamp transactionCommitTime = getTransactionCommitTime(
								request);
						if (transactionCommitTime.before(muteEventsOnOrBefore)
								|| transactionCommitTime
										.equals(muteEventsOnOrBefore)) {
							loadedRequests.remove(id);
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
	}

	class QueueWaiter {
		private Set<Long> waiting;

		private long timeoutMs;

		QueueWaiter(long timeoutMs, Optional<Long> requestId) {
			synchronized (queueModificationLock) {
				this.timeoutMs = timeoutMs;
				if (requestId.isPresent()) {
					waiting = Stream.of(requestId.get())
							.collect(Collectors.toSet());
					waiting.removeAll(appLifetimeEventsFired);
				} else {
					waiting = new LinkedHashSet<>(toFire);
					waiting.addAll(firingLocalToVm);
				}
				waiters.add(this);
			}
		}

		void notifyFired(Long requestId) {
			waiting.remove(requestId);
		}

		void waitForProcessedRequests() {
			long startTime = System.currentTimeMillis();
			while (true) {
				long timeRemaining = -System.currentTimeMillis() + startTime
						+ timeoutMs;
				synchronized (queueModificationLock) {
					try {
						/*
						 * requestId, if defined, may have been fired before we
						 * got here -
						 */
						if (waiting.isEmpty() || timeRemaining <= 0) {
							waiters.remove(this);
							return;
						}
						queueModificationLock.wait(timeRemaining);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}
}

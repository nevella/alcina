package cc.alcina.framework.entity.transform.event;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPositionProvider;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.DomainStoreTransformSequencer;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

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

	BlockingDeque<DomainTransformCommitPosition> persistedEvents = new LinkedBlockingDeque<>();

	Object queueModificationLock = new Object();

	AtomicBoolean closed = new AtomicBoolean(false);

	private FireEventsThread eventQueue;

	private DomainTransformPersistenceEvents persistenceEvents;

	private Thread firingThread = null;

	private Map<Long, DomainTransformRequestPersistent> loadedRequests = new ConcurrentHashMap<>();

	private Timestamp muteEventsOnOrBefore;

	private Map<Long, DomainTransformCommitPosition> requestIdSequenceEntry = new ConcurrentHashMap<>();

	// used to signal this event to a possibly waiting fire events thread
	private Object persistentRequestCached = new Object();

	private DomainTransformCommitPosition transformLogPosition;

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
		persistedEvents.add(new DomainTransformCommitPosition());
	}

	public void
			cachePersistedRequest(DomainTransformRequestPersistent request) {
		long requestId = request.getId();
		if (loadedRequests.containsKey(requestId)) {
			// local request coming in from clustertransformlistener - ignore
			logger.debug("Did not cache already loaded request: {}", requestId);
			return;
		}
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

	public DomainTransformCommitPosition getTransformLogPosition() {
		return this.transformLogPosition;
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		synchronized (queueModificationLock) {
			firingLocalToVm.add(dtrp.getId());
		}
	}

	public synchronized void sequencedTransformRequestPublished(Long id) {
		List<DomainTransformCommitPosition> unpublishedRequests = persistenceEvents.domainStore
				.getTransformSequencer().getSequentialUnpublishedRequests();
		logger.info("Received dtr published: {} ==> sequenced: {} ", id,
				unpublishedRequests);
		unpublishedRequests.forEach(entry -> requestIdSequenceEntry
				.put(entry.commitRequestId, entry));
		fireSequentialUnpublishedTransformRequests(unpublishedRequests);
	}

	public void setMuteEventsOnOrBefore(
			Timestamp highestVisibleTransactionTimestamp) {
		this.muteEventsOnOrBefore = highestVisibleTransactionTimestamp;
	}

	public void setTransformLogPosition(
			DomainTransformCommitPosition transformLogPosition) {
		this.transformLogPosition = transformLogPosition;
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

	public void transformRequestAborted(long id) {
		loadedRequests.remove(id);
	}

	public void transformRequestPublished(Long id) {
		if (persistenceEvents.isUseTransformDbCommitSequencing()) {
			sequencedTransformRequestPublished(id);
		} else {
			DomainTransformCommitPosition position = new DomainTransformCommitPosition(
					new Timestamp(System.currentTimeMillis()), id);
			transformRequestPublishedSequential(position);
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
			Optional<Long> lastToFireId = getLastToFireId()
					.map(position -> position.commitRequestId);
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

	public void waitUntilRequestTimestampProcessed(Timestamp timestamp,
			long timeoutMs) {
		new QueueWaiter(timeoutMs, timestamp).waitForProcessedRequests();
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
		DomainTransformResponse domainTransformResponse = new DomainTransformResponse();
		domainTransformResponse
				.setRequestId(persistenceToken.getRequest().getRequestId());
		domainTransformResponse.setTransformsProcessed(events.size());
		domainTransformResponse.setResult(DomainTransformResponseResult.OK);
		domainTransformResponse.setRequest(persistenceToken.getRequest());
		wrapper.response = domainTransformResponse;
		DomainTransformPersistenceEvent persistenceEvent = new DomainTransformPersistenceEvent(
				persistenceToken, wrapper,
				wrapper.providePersistenceEventType(), false);
		return persistenceEvent;
	}

	private void fireSequentialUnpublishedTransformRequests(
			List<DomainTransformCommitPosition> unpublishedRequests) {
		for (DomainTransformCommitPosition position : unpublishedRequests) {
			transformRequestPublishedSequential(position);
		}
	}

	private Optional<DomainTransformCommitPosition> getLastToFireId() {
		synchronized (queueModificationLock) {
			return Optional.<DomainTransformCommitPosition> ofNullable(
					persistedEvents.peekLast());
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

	private void transformRequestPublishedSequential(
			DomainTransformCommitPosition position) {
		synchronized (queueModificationLock) {
			if (firedOrQueued.contains(position.commitRequestId)) {
				logger.info("Ignoring already fired/queued: {} ",
						position.commitRequestId);
				return;
			} else {
				firedOrQueued.add(position.commitRequestId);
				logger.info("Adding to toFire queue: {} ",
						position.commitRequestId);
				persistedEvents.add(position);
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

	void transformRequestFinishedFiring(long requestId) {
		synchronized (queueModificationLock) {
			firedOrQueued.add(requestId);
			lastFired.add(requestId);
			firingLocalToVm.remove(requestId);
			for (QueueWaiter waiter : waiters) {
				waiter.notifyFired(requestId);
			}
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
					DomainTransformCommitPosition position = persistedEvents
							.poll(5, TimeUnit.SECONDS);
					if (closed.get()) {
						return;
					}
					if (position == null) {
						continue;
					}
					logger.debug("Removed id from toFire: {}",
							position.commitRequestId);
					try {
						Transaction.ensureBegun();
						ThreadedPermissionsManager.cast().pushSystemUser();
						publishTransformEvent(position);
					} finally {
						Transaction.ensureBegun();
						ThreadedPermissionsManager.cast().popSystemUser();
						Transaction.ensureEnded();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void
				publishTransformEvent(DomainTransformCommitPosition position) {
			boolean local = false;
			Long id = position.commitRequestId;
			fireEventThreadLogger.info("publishTransformEvent - dtr {}", id);
			synchronized (queueModificationLock) {
				local = firingLocalToVm.contains(id);
			}
			if (local) {
				// only the writable store can emit local events
				DomainStoreTransformSequencer transformSequencer = DomainStore
						.writableStore().getTransformSequencer();
				transformSequencer
						.unblockPreLocalNonFireEventsThreadBarrier(id);
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
						if (ResourceUtilities.is(
								DomainTransformPersistenceQueue.class,
								"noDbRequestWait")) {
							endWaitLoop = 0;
						}
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
					if (request == null) {
						request = AlcinaPersistentEntityImpl
								.getNewImplementationInstance(
										DomainTransformRequestPersistent.class);
						request.setId(id);
						fireEventThreadLogger.warn(
								"publishTransformEvent - firing emtpy event (no transforms?) dtr {}",
								id);
					}
					if (Ax.isTest() && request.getClientInstance() != null
							&& request.getClientInstance()
									.getId() == PermissionsManager.get()
											.getClientInstanceId()) {
						// local persisted via server
						DomainStoreTransformSequencer transformSequencer = DomainStore
								.writableStore().getTransformSequencer();
						transformSequencer
								.unblockPreLocalNonFireEventsThreadBarrier(id);
						transformSequencer
								.waitForPostLocalFireEventsThreadBarrier(id);
						return;
					}
					Timestamp transactionCommitTime = getTransactionCommitTime(
							request);
					if (transactionCommitTime.before(muteEventsOnOrBefore)
							|| transactionCommitTime
									.equals(muteEventsOnOrBefore)) {
						request.setEvents(new ArrayList<>());
					}
					Transaction.current().toNoActiveTransaction();
					DomainTransformPersistenceEvent event = createPersistenceEventFromPersistedRequest(
							request);
					event.ensureTransformsValidForVm();
					persistenceEvents
							.fireDomainTransformPersistenceEvent(event);
					loadedRequests.remove(id);
					synchronized (queueModificationLock) {
						transformLogPosition = position;
						queueModificationLock.notifyAll();
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
			this.timeoutMs = timeoutMs;
			synchronized (queueModificationLock) {
				if (requestId.isPresent()) {
					waiting = Stream.of(requestId.get())
							.collect(Collectors.toSet());
					waiting.removeAll(appLifetimeEventsFired);
				} else {
					waiting = persistedEvents.stream()
							.map(p -> p.commitRequestId)
							.collect(Collectors.toSet());
					waiting.addAll(firingLocalToVm);
				}
				waiters.add(this);
			}
		}

		QueueWaiter(long timeoutMs, Timestamp timestamp) {
			this(timeoutMs, Optional.empty());
			if (transformLogPosition != null
					&& transformLogPosition.commitTimestamp
							.compareTo(timestamp) >= 0) {
				/*
				 * No need to wait, we've already passed the required epoch
				 */
				waiting.clear();
			} else {
				long requestId = persistenceEvents.domainStore
						.getTransformSequencer()
						.getRequestIdAtTimestamp(timestamp);
				waiting.clear();
				waiting.add(requestId);
				synchronized (queueModificationLock) {
					waiting.removeAll(appLifetimeEventsFired);
				}
			}
		}

		void notifyFired(Long requestId) {
			waiting.remove(requestId);
		}

		void waitForProcessedRequests() {
			long startTime = System.currentTimeMillis();
			long warnLongRunningTime = startTime + 1000;
			while (true) {
				long now = System.currentTimeMillis();
				long timeRemaining = -now + startTime + timeoutMs;
				synchronized (queueModificationLock) {
					try {
						/*
						 * requestId, if defined, may have been fired before we
						 * got here -
						 */
						boolean override = false;
						if (override) {
							waiting.clear();
						}
						if (waiting.isEmpty() || timeRemaining <= 0) {
							waiters.remove(this);
							return;
						}
						/*
						 * debugging - why waiting so long
						 */
						if (now > warnLongRunningTime) {
							logger.info(
									"Long running wait for processed - {} - {} ms",
									waiting, now - startTime);
						}
						queueModificationLock
								.wait(10 * TimeConstants.ONE_SECOND_MS);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}
}

package cc.alcina.framework.entity.transform.event;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPositionProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue.Event.Type;

/**
 * Improvement: rather than a strict dtrp-id queue, use 'happens after' field of
 * dtrp to allow out-of-sequence publishing
 * 
 * 
 * TODO: doc :
 * 
 * how this interacts with ClusterTransformListener; how the threading works
 * 
 * @author nick@alcina.cc
 *
 */
public class DomainTransformPersistenceQueue {
	public static final String CONTEXT_WAIT_TIMEOUT_MS = DomainTransformPersistenceQueue.class
			.getName() + ".CONTEXT_WAIT_TIMEOUT_MS";

	Logger logger = LoggerFactory.getLogger(getClass());

	State state;

	AtomicBoolean closed = new AtomicBoolean(false);

	BlockingDeque<Event> events = new LinkedBlockingDeque<>();

	private FireEventsThread eventQueue;

	private DomainTransformPersistenceEvents persistenceEvents;

	/*
	 * On change, notify() is called
	 */
	private Map<Long, DomainTransformRequestPersistent> loadedRequests = new ConcurrentHashMap<>();

	private Map<Long, Boolean> enqueuedRequestIds = new ConcurrentHashMap<>();

	private Timestamp muteEventsOnOrBefore;

	private Sequencer sequencer;

	private ConcurrentLinkedQueue<QueueWaiter> queueWaiters = new ConcurrentLinkedQueue<>();

	private Event firingEvent;

	public DomainTransformPersistenceQueue(
			DomainTransformPersistenceEvents persistenceEvents) {
		this.persistenceEvents = persistenceEvents;
		state = new State();
	}

	public void appShutdown() {
		closed.set(true);
		events.add(new Event().withType(Type.SHUTDOWN));
	}

	public Thread getFireEventsThread() {
		return eventQueue;
	}

	public long getLength() {
		Event event = this.firingEvent;
		return events.size() + (event == null ? 0 : 1);
	}

	public long getOldestTx() {
		Event event = this.firingEvent;
		if (event == null) {
			event = events.peek();
		}
		if (event == null) {
			return 0;
		}
		return event.submitTime;
	}

	public DomainTransformCommitPosition getTransformCommitPosition() {
		return state.transformCommitPosition;
	}

	public void
			onPersistingVmLocalRequest(DomainTransformRequestPersistent dtrp) {
		// noop
	}

	public void
			onRequestDataReceived(DomainTransformRequestPersistent request) {
		long requestId = request.getId();
		if (loadedRequests.containsKey(requestId)) {
			// local request coming in from clustertransformlistener - ignore
			logger.debug("Did not cache already loaded request: {}", requestId);
			return;
		}
		logger.info("Cached request: {}", requestId);
		loadedRequests.put(requestId, request);
		/*
		 * Notify the possibly waiting event thread
		 */
		synchronized (loadedRequests) {
			loadedRequests.notifyAll();
		}
	}

	public void onSequencedCommitPositions(
			List<DomainTransformCommitPosition> positions) {
		positions.forEach(p -> events.add(Event.committed(p)));
	}

	public void onTransformRequestAborted(long requestId) {
		events.add(Event.aborted(requestId));
	}

	public void onTransformRequestCommitted(long requestId) {
		if (enqueuedRequestIds.containsKey(requestId)
				|| state.hasFired(requestId)) {
			return;
		} else {
			sequencer.onPersistedRequestCommitted(requestId);
		}
	}

	public void setMuteEventsOnOrBefore(
			Timestamp highestVisibleTransactionTimestamp) {
		this.muteEventsOnOrBefore = highestVisibleTransactionTimestamp;
	}

	public void setSequencer(Sequencer sequencer) {
		this.sequencer = sequencer;
	}

	public void setTransformLogPosition(
			DomainTransformCommitPosition transformLogPosition) {
		state.transformCommitPosition = transformLogPosition;
	}

	public void startEventQueue() {
		eventQueue = new FireEventsThread();
		eventQueue.start();
	}

	public String toDebugString() {
		synchronized (state) {
			return GraphProjection.fieldwiseToString(this, true, false, 9999,
					"loadedRequests");
		}
	}

	public void waitUntilCurrentRequestsProcessed() {
		waitUntilCurrentRequestsProcessed(10 * TimeConstants.ONE_SECOND_MS);
	}

	public void waitUntilCurrentRequestsProcessed(long timeoutMs) {
		new QueueWaiter(timeoutMs).await();
	}

	public void waitUntilEventQueueIsEmpty() {
		sequencer.refresh();
		new QueueWaiter(365 * TimeConstants.ONE_DAY_MS)
				.withAwaitEventEmptyQueue(true).await();
	}

	/*
	 * True if the wait was successful
	 */
	public boolean waitUntilRequestProcessed(long requestId, long timeoutMs) {
		return new QueueWaiter(timeoutMs).withRequestId(requestId).await();
	}

	public void waitUntilRequestTimestampProcessed(Timestamp timestamp,
			long timeoutMs) {
		new QueueWaiter(timeoutMs).withTimestamp(timestamp).await();
	}

	private DomainTransformPersistenceEvent
			createPersistenceEventFromPersistedRequest(
					DomainTransformRequestPersistent dtrp,
					DomainTransformCommitPosition position) {
		// create an "event" to publish in the queue
		/*
		 * note that on warmup ClientInstance.self() will be null, so guard for
		 * that
		 */
		long selfId = ClientInstance.self() == null ? -1
				: ClientInstance.self().getId();
		TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
				dtrp, null, dtrp.getClientInstance().getId() != selfId, false,
				false, null, true);
		persistenceToken.setLocalToVm(state.isLocalToVm(dtrp));
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
		DomainTransformPersistenceEvent event = new DomainTransformPersistenceEvent(
				persistenceToken, wrapper,
				wrapper.providePersistenceEventType(),
				dtrp.getClientInstance() != null && Objects.equals(
						dtrp.getClientInstance(), ClientInstance.self()));
		event.setFiringFromQueue(true);
		event.setPosition(position);
		return event;
	}

	private Logger getLogger(boolean localToVm) {
		return localToVm ? logger : eventQueue.fireEventThreadLogger;
	}

	void onEventListenerFiring(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return;
		}
		Logger logger = getLogger(event.isLocalToVm());
		logger.info("firing - {} - {} - {} events - range {}",
				Ax.friendly(event.getPersistenceEventType()),
				event.getTransformPersistenceToken().getRequest().shortId(),
				event.getTransformPersistenceToken().getRequest().getEvents()
						.size(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
	}

	void onEventListenerFiringCompleted(DomainTransformPersistenceEvent event) {
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
	}

	void onEventQueueEmpty() {
		synchronized (state) {
			state.notifyAll();
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
			return queue.getTransformCommitPosition();
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
					firingEvent = events.poll(5, TimeUnit.SECONDS);
					if (closed.get()) {
						return;
					}
					if (firingEvent == null) {
						continue;
					}
					logger.debug("Polled event from queue: {}", firingEvent);
					try {
						Transaction.ensureBegun();
						ThreadedPermissionsManager.cast().pushSystemUser();
						publishTransformEvent(firingEvent);
					} finally {
						Transaction.ensureBegun();
						ThreadedPermissionsManager.cast().popSystemUser();
						Transaction.ensureEnded();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					firingEvent = null;
				}
			}
		}

		private DomainTransformRequestPersistent loadRequest(Event event) {
			if (event.requestId == 0 && event.type == Type.ABORTED) {
				return null;
			}
			DomainTransformRequestPersistent request = loadedRequests
					.get(event.requestId);
			if (request != null) {
				return request;
			}
			if (event.type == Type.ABORTED) {
				return request;
			}
			long id = event.requestId;
			/*
			 * 'Exists' seems ... to return false when should be true, in a few
			 * cases
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
			if (ResourceUtilities.is(DomainTransformPersistenceQueue.class,
					"noDbRequestWait")) {
				endWaitLoop = 0;
			}
			while (true) {
				request = loadedRequests.get(id);
				if (request != null) {
					fireEventThreadLogger.warn(
							"publishTransformEvent - loaded request during wait - dtr {} ",
							id);
					return request;
				}
				long now = System.currentTimeMillis();
				if (request == null && now < endWaitLoop) {
					synchronized (loadedRequests) {
						try {
							loadedRequests.wait(endWaitLoop - now);
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					}
				} else {
					break;
				}
			}
			fireEventThreadLogger.warn(
					"publishTransformEvent - no loaded request - last try -  dtr {} - exists {}",
					id, exists);
			request = loadedRequests.get(id);
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
			return request;
		}

		private void publishTransformEvent(Event event) {
			boolean local = false;
			/*
			 * REVISIT - null if of type ERROR?
			 */
			Long requestId = event.requestId;
			fireEventThreadLogger.info("publishTransformEvent - dtr {}",
					requestId);
			DomainTransformRequestPersistent request = loadRequest(event);
			if (request == null) {
				request = AlcinaPersistentEntityImpl
						.getNewImplementationInstance(
								DomainTransformRequestPersistent.class);
				request.setId(requestId);
				fireEventThreadLogger.warn(
						"publishTransformEvent - firing empty event (no transforms?) dtr {}",
						requestId);
			}
			if (event.commitPosition != null) {
				Timestamp transactionCommitTime = event.commitPosition.commitTimestamp;
				if (transactionCommitTime.before(muteEventsOnOrBefore)
						|| transactionCommitTime.equals(muteEventsOnOrBefore)) {
					request.setEvents(new ArrayList<>());
				}
			}
			Transaction.current().toNoActiveTransaction();
			DomainTransformPersistenceEvent persistenceEvent = createPersistenceEventFromPersistedRequest(
					request, event.commitPosition);
			persistenceEvent.ensureTransformsValidForVm();
			persistenceEvents
					.fireDomainTransformPersistenceEvent(persistenceEvent);
			loadedRequests.remove(requestId);
			state.onEventFiringCompleted(request, event);
		}
	}

	public interface Sequencer {
		Long getLastRequestIdAtTimestamp(Timestamp timestamp);

		void onPersistedRequestCommitted(long requestId);

		void refresh();
	}

	static class Event {
		static Event aborted(long requestId) {
			Event event = new Event().withType(Type.ABORTED);
			event.requestId = requestId;
			return event;
		}

		static Event committed(DomainTransformCommitPosition commitPosition) {
			Event event = new Event().withType(Type.COMMIT);
			event.requestId = commitPosition.commitRequestId;
			event.commitPosition = commitPosition;
			return event;
		}

		Type type;

		long requestId;

		DomainTransformCommitPosition commitPosition;

		long submitTime;

		private Event() {
			submitTime = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		Event withType(Type type) {
			this.type = type;
			return this;
		}

		static enum Type {
			COMMIT, ABORTED, SHUTDOWN;
		}
	}

	class QueueWaiter {
		private Set<Long> awaitingRequestIds = new LinkedHashSet<>();

		private long timeoutMs;

		Timestamp awaitingTimestamp;

		private boolean awaitEmptyEventQueue;

		private Thread thread;

		private boolean timedOut;

		QueueWaiter(long timeoutMs) {
			this.timeoutMs = timeoutMs;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		private void await0() throws InterruptedException {
			long startTime = System.currentTimeMillis();
			long warnLongRunningTime = startTime + 1000;
			while (true) {
				long now = System.currentTimeMillis();
				long timeRemaining = -now + startTime + timeoutMs;
				int awaitingRequestCount = 0;
				synchronized (state) {
					if (timeRemaining <= 0) {
						logger.warn("Queue waiter timeout - {} - {} ms", this,
								now - startTime);
						timedOut = true;
						return;
					}
					if (now > warnLongRunningTime) {
						logger.info(
								"Long running wait for processed - {} - {} ms",
								this, now - startTime);
					}
					state.removeFiredFrom(awaitingRequestIds);
					awaitingRequestCount = awaitingRequestIds.size();
					if (awaitingRequestCount > 0) {
					} else {
						if (awaitEmptyEventQueue) {
							if (events.isEmpty()) {
								return;
							} else {
								// empty event queue calls state.notify()
							}
						} else {
							return;
						}
					}
					state.wait(Math.min(timeRemaining,
							1 * TimeConstants.ONE_SECOND_MS));
				}
			}
		}

		/*
		 * returns true if completed normally, false if timed out/exception
		 */
		boolean await() {
			try {
				this.thread = Thread.currentThread();
				awaitingRequestIds.remove(null);
				queueWaiters.add(this);
				await0();
				queueWaiters.remove(this);
				return !timedOut;
			} catch (Exception e) {
				// app shutdown
				Ax.simpleExceptionOut(e);
				return false;
			}
		}

		void interrupt() {
			thread.interrupt();
		}

		QueueWaiter withAwaitEventEmptyQueue(boolean awaitEmptyEventQueue) {
			this.awaitEmptyEventQueue = awaitEmptyEventQueue;
			return this;
		}

		QueueWaiter withRequestId(long requestId) {
			awaitingRequestIds.add(requestId);
			return this;
		}

		QueueWaiter withTimestamp(Timestamp timestamp) {
			this.awaitingTimestamp = timestamp;
			if (!state.isTimstampVisible(timestamp)) {
				awaitingRequestIds
						.add(sequencer.getLastRequestIdAtTimestamp(timestamp));
			} else {
				if (state.isAwaitEmptyEventQueue(timestamp)) {
					withAwaitEventEmptyQueue(true);
				}
			}
			return this;
		}
	}

	/*
	 * Modified atomically, where needed for consistency
	 */
	class State {
		// most recent event
		// REVISIT - remove?
		private Set<Long> lastFired = new LinkedHashSet<>();

		private Set<Long> appLifetimeEventsFired = new LinkedHashSet<>();

		private DomainTransformCommitPosition transformCommitPosition;

		public synchronized boolean
				isAwaitEmptyEventQueue(Timestamp timestamp) {
			if (transformCommitPosition == null) {
				return false;
			}
			int dir = transformCommitPosition.commitTimestamp
					.compareTo(timestamp);
			return dir <= 0;
		}

		public synchronized boolean isTimstampVisible(Timestamp timestamp) {
			if (transformCommitPosition == null) {
				return false;
			}
			int dir = transformCommitPosition.commitTimestamp
					.compareTo(timestamp);
			if (dir < 0) {
				return false;
			} else {
				return true;
			}
		}

		public synchronized void removeFiredFrom(Set<Long> requestIds) {
			requestIds.removeAll(appLifetimeEventsFired);
		}

		synchronized boolean hasFired(long requestId) {
			return appLifetimeEventsFired.contains(requestId);
		}

		synchronized boolean
				isLocalToVm(DomainTransformRequestPersistent dtrp) {
			return appLifetimeEventsFired.contains(dtrp.getId());
		}

		synchronized void onEventFiringCompleted(
				DomainTransformRequestPersistent request, Event event) {
			lastFired = new LinkedHashSet<>();
			lastFired.add(request.getId());
			appLifetimeEventsFired.add(request.getId());
			if (event.commitPosition != null) {
				transformCommitPosition = event.commitPosition;
			}
			notifyAll();
		}
	}
}

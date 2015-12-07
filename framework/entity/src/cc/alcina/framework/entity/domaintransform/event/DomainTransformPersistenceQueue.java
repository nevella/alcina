package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

@RegistryLocation(registryPoint = DomainTransformPersistenceQueue.class, implementationType = ImplementationType.SINGLETON)
public class DomainTransformPersistenceQueue implements RegistrableService {
	private static final long PERIODIC_DB_CHECK_MS = 5
			* TimeConstants.ONE_MINUTE_MS;

	public static int WAIT_FOR_PERSISTED_REQUEST_TIMEOUT_MS = 30 * 1000;

	boolean logDbEventCheck = true;

	LinkedList<DtrpQueued> requestQueue = new LinkedList<>();

	Map<Long, DtrpQueued> dtrpIdQueueLookup = new LinkedHashMap<>();

	private int requestQueueUnpublishedIndex = 0;

	private long lastDbCheck = 0;

	private Timer timer;

	private TimerTask gapCheckTask;

	protected TaggedLogger logger = Registry.impl(TaggedLoggers.class)
			.getLogger(getClass(), TaggedLogger.INFO);

	volatile long exMachineSourceIdCounter = -1;

	private long nonPublishedTransformsFoundTime;

	public DomainTransformPersistenceQueue() {
		Registry.checkSingleton(this);
		this.timer = new Timer("Timer-DomainTransformPersistenceQueue-runner");
	}

	@Override
	public void appShutdown() {
		if (timer != null) {
			logger.log("Ending gap check timer");
			timer.cancel();
			timer = null;
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void checkPersistedTransforms(boolean forceDbCheck) {
		boolean dbCheck = forceDbCheck;
		synchronized (requestQueue) {
			dbCheck |= (getFirstUnpublished().isPresent()
					&& getFirstUnpublished().get().persistenceEvent == null);
		}
		if (dbCheck) {
			List<DomainTransformRequestPersistent> persisted = getCommonPersistence()
					.getPersistentTransformRequests(
							getMaxDbPersistedRequestId(), 0, null, true, false);
			// check only fails if a new db
			if (!persisted.isEmpty()) {
				long maxDbPersistedRequestIdFromDirectCall = persisted.get(0)
						.getId();
				synchronized (requestQueue) {
					ensureQueued(maxDbPersistedRequestIdFromDirectCall);
				}
				if (logDbEventCheck) {
					logger.format("max persisted transform id: %s",
							maxDbPersistedRequestIdFromDirectCall);
				}
			}
		}
		List<Long> requestsToCheckFromDb = new ArrayList<>();
		synchronized (requestQueue) {
			pendingRequests().filter(rqq -> rqq.persistenceEvent == null)
					.map(rqq -> rqq.id)
					.forEach(id -> requestsToCheckFromDb.add(id));
		}
		if (!requestsToCheckFromDb.isEmpty()) {
			logger.format("Checking persisted transforms - gap %s",
					requestsToCheckFromDb);
			List<DomainTransformRequestPersistent> requests = runWithDisabledObjectPermissions(
					() -> getCommonPersistence().getPersistentTransformRequests(
							0, 0, requestsToCheckFromDb, false, true));
			// these - from db - won't have sub requests
			for (DomainTransformRequestPersistent dtrp : requests) {
				DomainTransformPersistenceEvent persistenceEvent = createPersistenceEventFromPersistedRequest(
						dtrp);
				ensureQueued(dtrp.getId()).persistenceEvent = persistenceEvent;
			}
			synchronized (requestQueue) {
				pendingRequests().forEach(rqq -> rqq.checkTimeout());
			}
		}
		synchronized (requestQueue) {
			Optional<DtrpQueued> firstUnpublished = getFirstUnpublished();
			if (firstUnpublished.isPresent()) {
				DtrpQueued queued = firstUnpublished.get();
				if (queued.persistenceEvent != null && !queued.isLocalToVm()) {
					// have it on a separate thread so it can "fire back"
					// into the checking thread
					submitPersistenceEventOnNewThread(queued);
				}
			}
		}
		notifyAll();
	}

	public synchronized void eventFired(DomainTransformPersistenceEvent event) {
		if (event.getDomainTransformLayerWrapper() == null) {
			return;
		}
		synchronized (requestQueue) {
			pendingRequests().filter(rqq -> rqq.persistenceEvent == event)
					.forEach(rqq -> rqq.modifyStatus(DtrpStatus.COMMITTED));
		}
		updateStatsForNonPublishedTransforms();
		notifyAll();
		checkPersistedTransforms(false);
	}

	public long getMaxDbPersistedRequestId() {
		synchronized (requestQueue) {
			return requestQueue.isEmpty() ? 0 : requestQueue.getLast().id;
		}
	}

	public void getMaxPersistentRequestBaseline() {
		List<DomainTransformRequestPersistent> persisted = runWithDisabledObjectPermissions(
				() -> getCommonPersistence().getPersistentTransformRequests(0,
						0, null, true, false));
		if (!persisted.isEmpty()) {
			long maxDbPersistedRequestIdPublished = persisted.get(0).getId();
			synchronized (requestQueue) {
				DtrpQueued queued = ensureQueued(
						maxDbPersistedRequestIdPublished);
				queued.setLocalToVm(false);
				queued.modifyStatus(DtrpStatus.BASELINE);
			}
			logger.format("max persisted transform id published: %s",
					maxDbPersistedRequestIdPublished);
		}
	}

	public long getTimeWaitingForPersistenceQueue() {
		return nonPublishedTransformsFoundTime == 0 ? 0
				: System.currentTimeMillis() - nonPublishedTransformsFoundTime;
	}

	public synchronized void
			registerPersisting(DomainTransformRequestPersistent dtrp) {
		synchronized (requestQueue) {
			ensureQueued(dtrp.getId()).setLocalToVm(true);
		}
	}

	public boolean
			shouldFire(DomainTransformPersistenceEvent persistenceEvent) {
		if (persistenceEvent.getPersistedRequestIds().isEmpty()) {
			return true;
		}
		/*
		 * it's possible that the event persistent ids are out of order (e.g.
		 * xxx1, xxx3) - could occur when multiple requests are bundled by a web
		 * client, and persistence is interleaved.
		 * 
		 * as long as the first id is lowest, commit 'em all (they'll be
		 * coherent in the db)
		 */
		Long min = persistenceEvent.getPersistedRequestIds().stream()
				.min(Comparator.naturalOrder()).get();
		synchronized (requestQueue) {
			LongPair firstGap = null;
			ensureEventRequestsQueued(persistenceEvent);
			DtrpQueued queuedEvent = ensureQueued(min);
			DtrpQueued unpublished = getFirstUnpublished().orElse(null);
			//could be that unpublished is null, if queuedEvent timedout
			if (queuedEvent != unpublished && unpublished != null) {
				firstGap = new LongPair(unpublished.id, queuedEvent.id - 1);
			}
			if (firstGap != null) {
				logger.format("found gap (waiting) - rqid %s - gap %s",
						persistenceEvent.getTransformPersistenceToken()
								.getRequest().shortId(),
						firstGap);
				if (unpublished.isLocalToVm()) {
					logger.log("waiting on another in-jvm db transaction");
				}
				return false;// let the queue sort this out
			}
			return true;
		}
	}

	public void startGapCheckTimer() {
		this.gapCheckTask = new GapCheckTask();
		/*
		 * this should only need to run when db changes have been made by
		 * another application. those will appear as gaps in this vm's persisted
		 * transforms
		 */
		timer.schedule(gapCheckTask, 0, 500);
		logger.log("Starting gap check timer");
	}

	public void startup() {
		checkPersistedTransforms(true);
		startGapCheckTimer();
	}

	public synchronized void submit(DomainTransformPersistenceEvent event) {
		if (event
				.getPersistenceEventType() != DomainTransformPersistenceEventType.COMMIT_OK) {
			return;
		}
		synchronized (requestQueue) {
			ensureEventRequestsQueued(event);
		}
		notifyAll();
	}

	public void waitUntilCurrentRequestsProcessed() {
		if (AppPersistenceBase.isTest()) {
			return;
		}
		checkPersistedTransforms(true);
		long max = getMaxDbPersistedRequestId();
		while (true) {
			synchronized (requestQueue) {
				Optional<DtrpQueued> unpublished = getFirstUnpublished();
				if (!unpublished.isPresent() || unpublished.get().id >= max) {
					break;
				}
			}
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	private void ensureEventRequestsQueued(
			DomainTransformPersistenceEvent persistenceEvent) {
		persistenceEvent.getPersistedRequestIds().stream().forEach(
				id -> ensureQueued(id).persistenceEvent = persistenceEvent);
	}

	private Optional<DtrpQueued> getFirstUnpublished() {
		synchronized (requestQueue) {
			while (true) {
				if (requestQueueUnpublishedIndex == requestQueue.size()) {
					return Optional.empty();
				}
				Optional<DtrpQueued> ret = Optional
						.of(requestQueue.get(requestQueueUnpublishedIndex));
				if (ret.get().status == DtrpStatus.PENDING) {
					return ret;
				}
				requestQueueUnpublishedIndex++;
			}
		}
	}

	protected CommonPersistenceLocal getCommonPersistence() {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence();
	}

	protected LongPair getFirstContiguousRange(LongPair boundingRange,
			Collection<Long> excludes, Collection<Long> includes) {
		LongPair pair = new LongPair();
		for (long lx = boundingRange.l1; lx <= boundingRange.l2; lx++) {
			boolean include = excludes.isEmpty() && includes.contains(lx)
					|| includes.isEmpty() && !excludes.contains(lx);
			if (include) {
				if (pair.l1 == 0) {
					pair.l1 = lx;
					pair.l2 = lx;
				} else {
					if (pair.l2 + 1 == lx) {
						pair.l2 = lx;
					} else {
						break;// first contiguous
					}
				}
			}
		}
		return pair;
	}

	private boolean allPersistedTransformsArePublished() {
		synchronized (requestQueue) {
			return requestQueue.size() == requestQueueUnpublishedIndex;
		}
	}

	private DomainTransformPersistenceEvent
			createPersistenceEventFromPersistedRequest(
					DomainTransformRequestPersistent dtrp) {
		// create an "event" to publish in the queue
		TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
				dtrp, null, Registry.impl(TransformLoggingPolicy.class), false,
				false, false, null, true);
		DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
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

	// not synchronized here - callers must sync
	private DtrpQueued ensureQueued(long dtrpId) {
		if (!dtrpIdQueueLookup.containsKey(dtrpId)) {
			if (requestQueue.size() == 0) {
				new DtrpQueued(dtrpId);
			} else {
				// create placeholder requests
				for (long id = requestQueue.getLast().id
						+ 1; id <= dtrpId; id++) {
					new DtrpQueued(id);
				}
			}
		}
		return dtrpIdQueueLookup.get(dtrpId);
	}

	void logFired(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return;
		}
		logger.format("fired - %s - range %s",
				event.getTransformPersistenceToken().getRequest().shortId(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
	}

	void logFiring(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return;
		}
		logger.format("firing - %s - range %s",
				event.getTransformPersistenceToken().getRequest().shortId(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
	}

	private Stream<DtrpQueued> pendingRequests() {
		return requestQueue
				.subList(requestQueueUnpublishedIndex, requestQueue.size())
				.stream();
	}

	private <T> T
			runWithDisabledObjectPermissions(ThrowingSupplier<T> supplier) {
		try {
			// this prevents a deadlock where we might have a waiting write
			// preventing us from getting the lock
			LooseContext.pushWithBoolean(AlcinaMemCache.CONTEXT_NO_LOCKS);
			ThreadedPermissionsManager.cast().pushSystemUser();
			PermissibleFieldFilter.disablePerObjectPermissions = true;
			return supplier.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			PermissibleFieldFilter.disablePerObjectPermissions = false;
			ThreadedPermissionsManager.cast().popSystemUser();
			LooseContext.pop();
		}
	}

	private void submitPersistenceEventOnNewThread(DtrpQueued queued) {
		if (queued.firing) {
			return;
		}
		queued.firing = true;
		new Thread() {
			public void run() {
				try {
					setName("DomainTransformPersistenceQueue-fire");
					ThreadedPermissionsManager.cast().pushSystemUser();
					PermissibleFieldFilter.disablePerObjectPermissions = true;
					Registry.impl(DomainTransformPersistenceEvents.class)
							.fireDomainTransformPersistenceEvent(
									queued.persistenceEvent);
				} catch (Throwable t) {
					t.printStackTrace();
					throw new RuntimeException(t);
				} finally {
					PermissibleFieldFilter.disablePerObjectPermissions = false;
					ThreadedPermissionsManager.cast().popSystemUser();
					// in case of error
					queued.firing = false;
				}
			};
		}.start();
	}

	class DtrpQueued {
		long id;

		DtrpStatus status = DtrpStatus.PENDING;

		DomainTransformPersistenceEvent persistenceEvent;

		private boolean localToVm;

		long startTime;

		int index;

		public DtrpQueued(long id) {
			this.id = id;
			index = requestQueue.size();
			startTime = System.currentTimeMillis();
			requestQueue.add(this);
			dtrpIdQueueLookup.put(id, this);
		}

		public void checkTimeout() {
			long time = System.currentTimeMillis();
			if (persistenceEvent == null && time
					- startTime > WAIT_FOR_PERSISTED_REQUEST_TIMEOUT_MS) {
				logger.format(
						"Timed out waiting for  persisted transforms (probably a crash/exception)- gap %s",
						id);
				modifyStatus(DtrpStatus.TIMED_OUT);
			}
		}

		boolean isLocalToVm() {
			return localToVm || (persistenceEvent != null
					&& persistenceEvent.isLocalToVm());
		}

		boolean firing = false;

		void modifyStatus(DtrpStatus newStatus) {
			synchronized (requestQueue) {
				status = newStatus;
				firing = false;
				// remove ref
				persistenceEvent = null;
				getFirstUnpublished();
			}
			synchronized (DomainTransformPersistenceQueue.this) {
				DomainTransformPersistenceQueue.this.notifyAll();
			}
		}

		void setLocalToVm(boolean localToVm) {
			this.localToVm = localToVm;
		}
	}

	enum DtrpStatus {
		PENDING, COMMITTED, TIMED_OUT, BASELINE
	}

	class GapCheckTask extends TimerTask {
		@Override
		public void run() {
			try {
				updateStatsForNonPublishedTransforms();
				if ((System.currentTimeMillis()
						- lastDbCheck) > PERIODIC_DB_CHECK_MS) {
					lastDbCheck = System.currentTimeMillis();
					int mins = Calendar.getInstance().get(Calendar.MINUTE);
					logDbEventCheck = mins <= 5;
					checkPersistedTransforms(true);
				} else {
					checkPersistedTransforms(false);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	void updateStatsForNonPublishedTransforms() {
		synchronized (requestQueue) {
			if (allPersistedTransformsArePublished()) {
				nonPublishedTransformsFoundTime = 0;
			} else {
				nonPublishedTransformsFoundTime = getFirstUnpublished()
						.get().startTime;
			}
		}
	}
}

package cc.alcina.framework.entity.domaintransform.event;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.EntityUtils;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

@RegistryLocation(registryPoint = DomainTransformPersistenceQueue.class, implementationType = ImplementationType.SINGLETON)
public class DomainTransformPersistenceQueue implements RegistrableService {
	boolean logDbEventCheck = true;

	class GapCheckTask extends TimerTask {
		private static final long PERIODIC_DB_CHECK_MS = 5 * TimeConstants.ONE_MINUTE_MS;

		@Override
		public void run() {
			try {
				if ((System.currentTimeMillis() - lastDbCheck) > PERIODIC_DB_CHECK_MS) {
					lastDbCheck = System.currentTimeMillis();
					int mins = Calendar.getInstance().get(Calendar.MINUTE);
					logDbEventCheck = mins <= 5;
					forceDbCheck();
				} else if (shouldCheckPersistedTransforms() != null) {
					maybeCheckPersistedTransforms();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	private long maxDbPersistedRequestIdPublished = 0;

	private long maxDbPersistedRequestId = 0;

	LinkedHashMap<Long, DomainTransformPersistenceEvent> persistedRequestIdToEvent = new LinkedHashMap<Long, DomainTransformPersistenceEvent>();

	Set<Long> persistingRequestIds = new LinkedHashSet<Long>();

	public static int WAIT_FOR_PERSISTED_REQUEST_TIMEOUT_MS = 30 * 1000;

	Set<Long> timedOutRequestIds = new LinkedHashSet<Long>();

	private long lastRangeCheckFirstCheckTime = 0;

	private long lastDbCheck = 0;

	private LongPair lastRangeCheck = null;

	private Timer timer;

	private TimerTask gapCheckTask;

	private volatile boolean checkingPersistedTransforms = false;

	private boolean forceDbCheck;

	protected TaggedLogger logger = Registry.impl(TaggedLoggers.class)
			.getLogger(getClass(), TaggedLogger.INFO);

	volatile long exMachineSourceIdCounter = -1;

	protected boolean firingPersistedEvents;

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

	public synchronized void eventFired(DomainTransformPersistenceEvent event) {
		if (event.getDomainTransformLayerWrapper() == null) {
			return;
		}
		persistedRequestIdToEvent.remove(event.getSourceThreadId());
		maxDbPersistedRequestIdPublished = Math.max(
				maxDbPersistedRequestIdPublished,
				event.getMaxPersistedRequestId());
		notifyAll();
	}

	public void forceDbCheck() {
		forceDbCheck = true;
		maybeCheckPersistedTransforms();
	}

	public void getMaxPersistentRequestBaseline() {
		try {
			ThreadedPermissionsManager.cast().pushSystemUser();
			PermissibleFieldFilter.disablePerObjectPermissions = true;
			List<DomainTransformRequestPersistent> persisted = getCommonPersistence()
					.getPersistentTransformRequests(0, 0, null, true, false);
			if (!persisted.isEmpty()) {
				maxDbPersistedRequestIdPublished = persisted.get(0).getId();
				logger.format("max persisted transform id published: %s",
						maxDbPersistedRequestIdPublished);
			}
		} finally {
			PermissibleFieldFilter.disablePerObjectPermissions = false;
			ThreadedPermissionsManager.cast().popSystemUser();
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		persistingRequestIds.add(dtrp.getId());
		maxDbPersistedRequestId = Math.max(maxDbPersistedRequestId,
				dtrp.getId());
	}

	public boolean shouldFire(DomainTransformPersistenceEvent event) {
		List<Long> persistedRequestIds = event.getPersistedRequestIds();
		if (persistedRequestIds.isEmpty()) {
			return true;
		}
		LongPair firstGap = getFirstGapLessThan(
				CollectionFilters.min(persistedRequestIds), false);
		if (firstGap != null && maxDbPersistedRequestIdPublished > 0) {// &&false)
																		// {//temp
																		// fix
																		// for
																		// prod.
																		// serv.
			logger.format("found gap (waiting) - rqid %s - gap %s", event
					.getTransformPersistenceToken().getRequest().shortId(),
					firstGap);
			LongPair withPublishingGap = getFirstGapLessThan(
					CollectionFilters.min(persistedRequestIds), true);
			if (withPublishingGap == null) {
				logger.log("waiting on another in-jvm db transaction");
			}
			return false;// let the queue sort this out
		}
		logger.format("firing - %s - range %s", event
				.getTransformPersistenceToken().getRequest().shortId(),
				new LongPair(CollectionFilters.min(persistedRequestIds),
						CollectionFilters.max(persistedRequestIds)));
		return true;
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
		forceDbCheck();
		startGapCheckTimer();
	}

	public synchronized void submit(DomainTransformPersistenceEvent event) {
		if (event.getPersistenceEventType() != DomainTransformPersistenceEventType.COMMIT_OK) {
			return;
		}
		persistedRequestIdToEvent.put(event.getSourceThreadId(), event);
		maxDbPersistedRequestId = Math.max(maxDbPersistedRequestId,
				event.getMaxPersistedRequestId());
		persistingRequestIds.removeAll(event.getPersistedRequestIds());
		notifyAll();
	}

	@SuppressWarnings("unchecked")
	private void checkPersistedTransforms(LongPair checkRequestRange) {
		try {
			checkingPersistedTransforms = true;
			// check timeout
			if (lastRangeCheck != null
					&& checkRequestRange.equals(lastRangeCheck)) {
				if (System.currentTimeMillis() - lastRangeCheckFirstCheckTime > WAIT_FOR_PERSISTED_REQUEST_TIMEOUT_MS) {
					logger.format(
							"Timed out waiting for  persisted transforms (probably a crash/exception)- gap %s",
							checkRequestRange);
					for (long l = lastRangeCheck.l1; l <= lastRangeCheck.l2; l++) {
						timedOutRequestIds.add(l);
					}
					synchronized (this) {
						notifyAll();
					}
					return;
				}
			} else {
				lastRangeCheck = checkRequestRange;
				lastRangeCheckFirstCheckTime = System.currentTimeMillis();
			}
			logger.format("Checking persisted transforms - gap %s",
					checkRequestRange);
			List<DomainTransformRequestPersistent> requests = getCommonPersistence()
					.getPersistentTransformRequests(checkRequestRange.l1,
							checkRequestRange.l2, null, false, true);
			// unless we have multiple writers, if we get anything, we'll get
			// the whole range
			// nevertheless, let's do this properly - just fire the first
			// contiguous range
			if (!requests.isEmpty()) {
				final LongPair contiguousRange = getFirstContiguousRange(
						new LongPair(CommonUtils.first(requests).getId(),
								CommonUtils.last(requests).getId()),
						Collections.EMPTY_LIST,
						EntityUtils.hasIdsToIdList(requests));
				CollectionFilter<DomainTransformRequestPersistent> filter = new CollectionFilter<DomainTransformRequestPersistent>() {
					@Override
					public boolean allow(DomainTransformRequestPersistent o) {
						return contiguousRange.containsIncludingBoundaries(o
								.getId());
					}
				};
				requests = CollectionFilters.filter(requests, filter);
				if (requests.isEmpty()) {
				}
				logger.format(
						"enqueueing persisted transforms - dtrp %s => subrange %s",
						checkRequestRange, contiguousRange);
				final TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
						CommonUtils.last(requests), null,
						Registry.impl(TransformLoggingPolicy.class), false,
						false, false, null, true);
				final DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
				List<DomainTransformEventPersistent> events = (List) DomainTransformRequest
						.allEvents(requests);
				wrapper.persistentEvents = events;
				wrapper.persistentRequests = requests;
				DomainTransformResponse dtr = new DomainTransformResponse();
				dtr.setRequestId(persistenceToken.getRequest().getRequestId());
				dtr.setTransformsProcessed(events.size());
				dtr.setResult(DomainTransformResponseResult.OK);
				dtr.setRequest(persistenceToken.getRequest());
				wrapper.response = dtr;
				
				// have it on a separate thread so it can "fire back"
				// into the checking thread
				new Thread() {
					public void run() {
						try {
							ThreadedPermissionsManager.cast().pushSystemUser();
							PermissibleFieldFilter.disablePerObjectPermissions = true;
							firingPersistedEvents=true;
							Registry.impl(
									DomainTransformPersistenceEvents.class)
									.fireDomainTransformPersistenceEvent(
											new DomainTransformPersistenceEvent(
													persistenceToken, wrapper,
													exMachineSourceIdCounter--));
						} finally {
							firingPersistedEvents=false;
							PermissibleFieldFilter.disablePerObjectPermissions = false;
							ThreadedPermissionsManager.cast().popSystemUser();
						}
					};
				}.start();
			}
		} finally {
			checkingPersistedTransforms = false;
		}
	}

	private synchronized LongPair getFirstGapLessThan(Long lx,
			boolean includePublishingInHandledByThisVm) {
		Set<Long> handledIds = new LinkedHashSet<Long>();
		if (includePublishingInHandledByThisVm) {
			handledIds.addAll(persistingRequestIds);
		}
		String ids = ResourceUtilities.getBundledString(
				DomainTransformPersistenceQueue.class, "ignoreForQueueingIds");
		handledIds.addAll(TransformManager.idListToLongs(ids));
		handledIds.addAll(timedOutRequestIds);
		for (DomainTransformPersistenceEvent persistenceEvent : persistedRequestIdToEvent
				.values()) {
			handledIds.addAll(persistenceEvent.getPersistedRequestIds());
		}
		long max = CommonUtils.lv(CollectionFilters.max(handledIds));
		max = maxDbPersistedRequestId;
		LongPair pair = getFirstContiguousRange(new LongPair(
				maxDbPersistedRequestIdPublished + 1, max), handledIds,
				Collections.EMPTY_LIST);
		return pair.isZero() || pair.l1 >= lx ? null : pair;
	}

	private synchronized LongPair shouldCheckPersistedTransforms() {
		if (checkingPersistedTransforms) {
			return null;
		}
		if(firingPersistedEvents){
			System.out.println("firing persisted events...");
			return null;
		}
		if (forceDbCheck) {
			List<DomainTransformRequestPersistent> persisted = getCommonPersistence()
					.getPersistentTransformRequests(0, 0, null, true, false);
			if (!persisted.isEmpty()) {
				maxDbPersistedRequestId = persisted.get(0).getId();
				if (logDbEventCheck) {
					logger.format("max persisted transform id: %s",
							maxDbPersistedRequestId);
				}
			}
			forceDbCheck = false;
		}
		return getFirstGapLessThan(maxDbPersistedRequestId, true);
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

	protected synchronized void maybeCheckPersistedTransforms() {
		LongPair checkRequestRange = shouldCheckPersistedTransforms();
		if (checkRequestRange != null) {
			try {
				ThreadedPermissionsManager.cast().pushSystemUser();
				PermissibleFieldFilter.disablePerObjectPermissions = true;
				checkPersistedTransforms(checkRequestRange);
			} finally {
				PermissibleFieldFilter.disablePerObjectPermissions = false;
				ThreadedPermissionsManager.cast().popSystemUser();
			}
		}
	}
}

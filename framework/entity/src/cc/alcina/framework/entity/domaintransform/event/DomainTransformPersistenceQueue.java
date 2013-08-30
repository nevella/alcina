package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
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
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.servlet.servlet.control.WriterService;

@RegistryLocation(registryPoint = DomainTransformPersistenceEvents.class, implementationType = ImplementationType.SINGLETON)
public class DomainTransformPersistenceQueue extends WriterService {
	private long maxDbPersistedRequestIdPublished = 0;

	private long maxDbPersistedRequestId = 0;

	LinkedHashMap<Long, DomainTransformPersistenceEvent> persistedRequestIdToEvent = new LinkedHashMap<Long, DomainTransformPersistenceEvent>();

	Set<Long> persistingRequestIds = new LinkedHashSet<Long>();

	private Timer timer;

	private TimerTask gapCheckTask;

	private volatile boolean checkingPersistedTransforms = false;

	private boolean forceDbCheck;

	protected TaggedLogger logger = Registry.impl(TaggedLoggers.class)
			.getLogger(getClass(), TaggedLogger.INFO);

	public DomainTransformPersistenceQueue() {
		Registry.checkSingleton(this);
		this.timer = new Timer();
	}

	public void eventFired(DomainTransformPersistenceEvent event) {
		persistedRequestIdToEvent.remove(event.getSourceThreadId());
		maxDbPersistedRequestIdPublished = Math.max(
				maxDbPersistedRequestIdPublished,
				CollectionFilters.max(event.getPersistedRequestIds()));
		notifyAll();
	}

	public void forceDbCheck() {
		forceDbCheck = true;
		maybeCheckPersistedTransforms();
	}

	public void getMaxPersistentRequestBaseline() {
		List<DomainTransformRequestPersistent> persisted = getCommonPersistence()
				.getPersistentTransformRequests(0, 0, null, true);
		if (!persisted.isEmpty()) {
			maxDbPersistedRequestIdPublished = persisted.get(0).getId();
			logger.format("dtrq - max persisted transform id published: %s",
					maxDbPersistedRequestIdPublished);
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		persistingRequestIds.add(dtrp.getId());
	}

	public boolean shouldFire(DomainTransformPersistenceEvent event) {
		if (event.getPersistedRequestIds().isEmpty()) {
			return true;
		}
		LongPair firstGap = getFirstGap();
		if (firstGap != null) {
			logger.format("Dtr queue - found gap - rqid %s - gap %s", event
					.getTransformPersistenceToken().getRequest().shortId(),
					firstGap);
			return false;// let the queue sort this out
		}
		return true;
	}

	@Override
	public void shutdown() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	public void startGapCheckTimer() {
		this.gapCheckTask = new TimerTask() {
			@Override
			public void run() {
				if (shouldCheckPersistedTransforms() != null) {
					new Thread() {
						public void run() {
							maybeCheckPersistedTransforms();
						};
					}.start();
				}
			}
		};
		timer.schedule(gapCheckTask, 0, 100);
	}

	@Override
	public void startup() {
		forceDbCheck();
		startGapCheckTimer();
	}

	public void submit(DomainTransformPersistenceEvent event) {
		persistedRequestIdToEvent.put(event.getSourceThreadId(), event);
		persistingRequestIds.removeAll(event.getPersistedRequestIds());
		notifyAll();
	}

	@SuppressWarnings("unchecked")
	private void checkPersistedTransforms(LongPair checkRequestRange) {
		try {
			checkingPersistedTransforms = true;
			logger.format("Checking persisted transforms - gap %s",
					checkRequestRange);
			List<DomainTransformRequestPersistent> requests = getCommonPersistence()
					.getPersistentTransformRequests(checkRequestRange.l1,
							checkRequestRange.l2, null, false);
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
						return contiguousRange.allow(o.getId());
					}
				};
				requests = CollectionFilters.filter(requests, filter);
				logger.format(
						"Firing persisted transforms - dtrp %s => subrange %s",
						checkRequestRange, contiguousRange);
				TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
						CommonUtils.last(requests), null,
						Registry.impl(TransformLoggingPolicy.class), false,
						false, false, null, true);
				DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
				List<DomainTransformEventPersistent> events = new ArrayList<DomainTransformEventPersistent>();
				for (DomainTransformRequestPersistent request : requests) {
					events.addAll((List) request.getEvents());
				}
				wrapper.persistentEvents = events;
				wrapper.persistentRequests = requests;
				Registry.impl(DomainTransformPersistenceEvents.class)
						.fireDomainTransformPersistenceEvent(
								new DomainTransformPersistenceEvent(
										persistenceToken, wrapper,
										exMachineSourceIdCounter--));
			}
		} finally {
			checkingPersistedTransforms = false;
		}
	}

	volatile long exMachineSourceIdCounter = -1;

	private synchronized LongPair getFirstGap() {
		Set<Long> publishedOrPublishingIds = new LinkedHashSet<Long>();
		publishedOrPublishingIds.addAll(persistingRequestIds);
		for (DomainTransformPersistenceEvent persistenceEvent : persistedRequestIdToEvent
				.values()) {
			publishedOrPublishingIds.addAll(persistenceEvent
					.getPersistedRequestIds());
		}
		long max = CommonUtils.lv(CollectionFilters
				.max(publishedOrPublishingIds));
		max = Math.max(max, maxDbPersistedRequestId);
		LongPair pair = getFirstContiguousRange(new LongPair(
				maxDbPersistedRequestIdPublished, max),
				publishedOrPublishingIds, Collections.EMPTY_LIST);
		return pair.isZero() ? null : pair;
	}

	private synchronized LongPair shouldCheckPersistedTransforms() {
		if (checkingPersistedTransforms) {
			return null;
		}
		if (forceDbCheck) {
			List<DomainTransformRequestPersistent> persisted = getCommonPersistence()
					.getPersistentTransformRequests(0, 0, null, true);
			if (!persisted.isEmpty()) {
				maxDbPersistedRequestId = persisted.get(0).getId();
				logger.format("dtrq - max persisted transform id: %s",
						maxDbPersistedRequestId);
			}
		}
		return getFirstGap();
	}

	protected CommonPersistenceLocal getCommonPersistence() {
		return EntityLayerLocator.get().commonPersistenceProvider()
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
			checkPersistedTransforms(checkRequestRange);
		}
	}
}

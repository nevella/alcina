package cc.alcina.framework.entity.transform.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

public class DomainTransformPersistenceEvents {
	private List<DomainTransformPersistenceListener> listenerList = new ArrayList<DomainTransformPersistenceListener>();

	private DomainTransformPersistenceQueue queue;

	DomainStore domainStore;

	CascadedTransformLocalIdSupport transformLocalIdSupport = new CascadedTransformLocalIdSupport();

	Logger logger = LoggerFactory.getLogger(getClass());

	public DomainTransformPersistenceEvents(DomainStore domainStore) {
		this.domainStore = domainStore;
		this.queue = new DomainTransformPersistenceQueue(this);
	}

	public void addDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener) {
		listenerList.add(listener);
	}

	public void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
		switch (event.getPersistenceEventType()) {
		case PRE_COMMIT: {
			if (event.getTransformPersistenceToken()
					.isRequestorExternalToThisJvm()) {
				transformLocalIdSupport.runWithOffsetLocalIdCounter(
						() -> fireDomainTransformPersistenceEvent0(event));
			} else {
				fireDomainTransformPersistenceEvent0(event);
			}
			break;
		}
		case PRE_FLUSH: {
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				if (listener.isPreBarrierListener() && event.isLocalToVm()) {
					listener.onDomainTransformRequestPersistence(event);
				}
			}
			break;
		}
		default: {
			event.getPersistedRequests().forEach(queue::onRequestDataReceived);
			if (event.isLocalToVm() && !event.isFiringFromQueue()) {
				event.getPersistedRequestIds()
						.forEach(queue::onTransformRequestCommitted);
			}
			fireDomainTransformPersistenceEvent0(event);
			break;
		}
		}
	}

	public DomainTransformPersistenceQueue getQueue() {
		return this.queue;
	}

	public void removeDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener) {
		listenerList.remove(listener);
	}

	public void startEventQueue() {
		queue.startEventQueue();
	}

	private void fireDomainTransformPersistenceEvent0(
			DomainTransformPersistenceEvent event) {
		boolean hasRequests = event.getPersistedRequestIds() != null
				&& event.getPersistedRequestIds().size() > 0;
		long firstRequestId = hasRequests
				? event.getPersistedRequestIds().get(0)
				: 0;
		if (hasRequests && event.isLocalToVm()) {
			if (Ax.isTest()) {
				// won't know that the companion dev server is persisting this
				// persistent rq id (and is logically "local") until now
				event.getDomainTransformLayerWrapper().persistentRequests
						.forEach(queue::onPersistingVmLocalRequest);
			}
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				if (listener.isPreBarrierListener()) {
					listener.onDomainTransformRequestPersistence(event);
				}
			}
			if (!event.isFiringFromQueue()) {
				switch (event.getPersistenceEventType()) {
				// REVISIT - check this is fired;
				case COMMIT_ERROR:
					int debug = 3;
				case COMMIT_OK:
					/*
					 * will be fired from the queue
					 */
					boolean timedOut = !queue.waitUntilRequestProcessed(
							event.getMaxPersistedRequestId(),
							10 * TimeConstants.ONE_SECOND_MS);
					if (timedOut) {
						logger.warn("Timed out waiting for local-vm tx - {}",
								event);
					}
					Transaction.endAndBeginNew();
					return;
				}
			}
		}
		synchronized (this) {
			try {
				queue.onEventListenerFiring(event);
				for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
						listenerList)) {
					if (listener.isPreBarrierListener()) {
						continue;
					}
					// only fire ex-machine transforms to certain general
					// listeners
					if (event.isLocalToVm()
							|| listener.isAllVmEventsListener()) {
						try {
							InternalMetrics.get().startTracker(event,
									() -> describeEvent(event),
									InternalMetricTypeAlcina.service,
									Thread.currentThread().getName(),
									() -> true);
							listener.onDomainTransformRequestPersistence(event);
						} finally {
							InternalMetrics.get().endTracker(event);
						}
					}
				}
			} finally {
				queue.onEventListenerFiringCompleted(event);
			}
		}
	}

	String describeEvent(DomainTransformPersistenceEvent event) {
		return Ax.format("Persistence event: id: %s - %s",
				CommonUtils.first(event.getPersistedRequestIds()),
				event.getPersistenceEventType());
	}

	// FIXME - mvcc.4 - add optional
	// cluster-level counter (allowing for proxy swap etc)
	//
	// cluster-level will need reaper and reconstituter
	//
	// FIXME - should look at the TR to check it's an async client
	static class CascadedTransformLocalIdSupport {
		Map<Long, AtomicLong> perClientInstanceLocalIdCounter = new LinkedHashMap<>();

		synchronized void runWithOffsetLocalIdCounter(Runnable runnable) {
			ClientInstance serverAsClientInstance = EntityLayerObjects.get()
					.getServerAsClientInstance();
			ClientInstance threadInstance = PermissionsManager.get()
					.getClientInstance();
			// hack - Jumail's config? threadInstance should never be null
			// mvcc.jobs.2 - this branch should never be reached
			if (threadInstance == null || serverAsClientInstance
					.getId() == threadInstance.getId()) {
				LoggerFactory.getLogger(getClass()).warn(
						"CascadedTransformLocalIdSupport - threadInstance: {}\n{}",
						threadInstance,
						SEUtilities.getFullStacktrace(Thread.currentThread()));
				runnable.run();
				return;
			} else {
				AtomicLong idCounter = perClientInstanceLocalIdCounter
						.getOrDefault(threadInstance.getId(),
								// allow some headroom for alcina fast gwt long
								// impl - it has maxvalue of 1 << 30 -1
								//
								// set the initial created id of cascaded local
								// instances (per client instance) at 2^28 -
								// about 5*10^8
								//
								// assume we don't get that many per-instance
								// creation events (for jade, that's guaranteed)
								new AtomicLong(1 << 29));
				// tm.sequentialidgenerator throws an
				// exception at 1^29
				try {
					LooseContext.push();
					ThreadlocalTransformManager.cast()
							.resetLocalIdCounterForCurrentThread(idCounter);
					runnable.run();
				} finally {
					ThreadlocalTransformManager.cast()
							.useGlobalLocalIdCounter();
					LooseContext.pop();
				}
			}
		}
	}
}
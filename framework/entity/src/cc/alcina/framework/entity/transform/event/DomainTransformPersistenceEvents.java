package cc.alcina.framework.entity.transform.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

public class DomainTransformPersistenceEvents {
	private List<DomainTransformPersistenceListener> listenerList = new ArrayList<DomainTransformPersistenceListener>();

	private List<DomainTransformPersistenceListener> nonThreadListenerList = new ArrayList<DomainTransformPersistenceListener>();

	private DomainTransformPersistenceQueue queue;

	DomainStore domainStore;

	CascadedTransformLocalIdSupport transformLocalIdSupport = new CascadedTransformLocalIdSupport();

	public DomainTransformPersistenceEvents(DomainStore domainStore) {
		this.domainStore = domainStore;
		this.queue = new DomainTransformPersistenceQueue(this);
	}

	public void addDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener) {
		addDomainTransformPersistenceListener(listener, false);
	}

	public void addDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener,
			boolean listenOnNonThreadEvents) {
		listenerList.add(listener);
		if (listenOnNonThreadEvents) {
			nonThreadListenerList.add(listener);
		}
	}

	public void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
		switch (event.getPersistenceEventType()) {
		case PRE_COMMIT: {
			transformLocalIdSupport.runWithOffsetLocalIdCounter(
					() -> fireDomainTransformPersistenceEvent0(event));
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
			event.getPersistedRequests().forEach(queue::cachePersistedRequest);
			DomainStore.writableStore().onTransformsPersisted();
			fireDomainTransformPersistenceEvent0(event);
			event.getPostEventRunnables().forEach(Runnable::run);
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
		nonThreadListenerList.remove(listener);
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
						.forEach(queue::registerPersisting);
			}
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				if (listener.isPreBarrierListener() && event.isLocalToVm()) {
					listener.onDomainTransformRequestPersistence(event);
				}
			}
			if (event
					.getPersistenceEventType() == DomainTransformPersistenceEventType.COMMIT_OK) {
				domainStore.getTransformSequencer()
						.waitForPreLocalNonFireEventsThreadBarrier(
								firstRequestId);
			}
		}
		synchronized (this) {
			try {
				queue.logFiring(event);
				if (hasRequests) {
					event.getPersistedRequestIds()
							.forEach(queue::transformRequestQueued);
				}
				for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
						listenerList)) {
					if (listener.isPreBarrierListener()) {
						continue;
					}
					// only fire ex-machine transforms to certain general
					// listeners
					if (event.isLocalToVm()
							|| nonThreadListenerList.contains(listener)) {
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
				if (hasRequests) {
					event.getPersistedRequestIds()
							.forEach(queue::transformRequestFinishedFiring);
				}
				if (hasRequests && event.isLocalToVm()) {
					domainStore.getTransformSequencer()
							.finishedFiringLocalEvent(firstRequestId);
				}
				/*
				 * this can block if cascaded transforms were called, so run
				 * after finishedFiringLocalEvent
				 */
				queue.logFired(event);
			}
		}
	}

	String describeEvent(DomainTransformPersistenceEvent event) {
		return Ax.format("Persistence event: id: %s - %s",
				CommonUtils.first(event.getPersistedRequestIds()),
				event.getPersistenceEventType());
	}

	boolean isUseTransformDbCommitSequencing() {
		return domainStore.getDomainDescriptor()
				.isUseTransformDbCommitSequencing();
	}

	// FIXME - mvcc.4 - add optional
	// cluster-level counter (allowing for proxy swap etc)
	//
	// cluster-level will need reaper and reconstituter
	static class CascadedTransformLocalIdSupport {
		Map<Long, AtomicLong> perClientInstanceLocalIdCounter = new LinkedHashMap<>();

		synchronized void runWithOffsetLocalIdCounter(Runnable runnable) {
			ClientInstance serverAsClientInstance = EntityLayerObjects.get()
					.getServerAsClientInstance();
			ClientInstance threadInstance = PermissionsManager.get()
					.getClientInstance();
			// hack - Jumail's config? threadInstance should never be null
			if (threadInstance == null || serverAsClientInstance
					.getId() == threadInstance.getId()) {
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
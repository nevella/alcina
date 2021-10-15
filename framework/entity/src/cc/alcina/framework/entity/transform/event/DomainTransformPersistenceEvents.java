package cc.alcina.framework.entity.transform.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.OffThreadLogger;

public class DomainTransformPersistenceEvents {
	private static final String CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS = DomainTransformPersistenceEvents.class
			.getName() + ".CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS";

	public static void setLocalCommitTimeout(long timeout) {
		LooseContext.set(CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS, timeout);
	}

	private List<DomainTransformPersistenceListener> listenerList = new ArrayList<DomainTransformPersistenceListener>();

	private DomainTransformPersistenceQueue queue;

	DomainStore domainStore;

	CascadedTransformLocalIdSupport transformLocalIdSupport = new CascadedTransformLocalIdSupport();

	Logger logger = OffThreadLogger.getLogger(getClass());

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
		if (event.isLocalToVm()) {
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				if (listener.isPreBarrierListener()) {
					try {
						listener.onDomainTransformRequestPersistence(event);
					} catch (RuntimeException rex) {
						logger.warn(
								"DEVEX::0 - Exception in persistenceListener - {}",
								rex);
						throwOrLogBasedOnEventPhase(
								event.getPersistenceEventType(), rex);
					}
				}
			}
		}
		switch (event.getPersistenceEventType()) {
		case PRE_COMMIT: {
			if (event.getTransformPersistenceToken()
					.isRequestorExternalToThisJvm()) {
				transformLocalIdSupport.runWithOffsetLocalIdCounter(
						() -> fireDomainTransformPersistenceEvent0(event));
			} else {
				event.getTransformPersistenceToken().getRequest().allRequests()
						.forEach(
								rq -> getQueue().onPreparingVmLocalRequest(rq));
				fireDomainTransformPersistenceEvent0(event);
			}
			break;
		}
		case PRE_FLUSH: {
			event.getPersistedRequests()
					.forEach(queue::onPersistedRequestPreCommitted);
			break;
		}
		case COMMIT_OK:
		case COMMIT_ERROR: {
			event.getPersistedRequests().forEach(queue::onRequestDataReceived);
			if (event.isLocalToVm() && !event.isFiringFromQueue()) {
				event.getPersistedRequestIds().forEach(
						id -> queue.onTransformRequestCommitted(id, true));
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
		boolean hasPersistentRequests = event.getPersistedRequestIds() != null
				&& event.getPersistedRequestIds().size() > 0;
		long firstRequestId = hasPersistentRequests
				? event.getPersistedRequestIds().get(0)
				: 0;
		DomainTransformPersistenceEventType persistenceEventType = event
				.getPersistenceEventType();
		if (event.isLocalToVm()) {
			if (hasPersistentRequests && !event.isFiringFromQueue()) {
				switch (persistenceEventType) {
				// REVISIT - check this is fired;
				case COMMIT_ERROR:
					int debug = 3;
				case COMMIT_OK:
					/*
					 * will be fired from the queue
					 */
					boolean timedOut = !queue.waitUntilRequestProcessed(
							event.getMaxPersistedRequestId(),
							getLocalCommitTimeout());
					if (timedOut) {
						logger.warn(
								"DEVEX::0 - Timed out waiting for local-vm tx - {}\n\n{}\n",
								event, SEUtilities.getFullStacktrace(
										Thread.currentThread()));
						queue.onLocalVmTxTimeout();
					}
					Transaction.endAndBeginNew();
					return;
				}
			}
		}
		Object monitor = null;
		switch (persistenceEventType) {
		// fire sequentially
		case COMMIT_OK:
		case COMMIT_ERROR:
			monitor = this;
			break;
		// can fire in parallel
		case PRE_COMMIT:
		case PRE_FLUSH:
			monitor = new Object();
			break;
		default:
			throw new UnsupportedOperationException();
		}
		synchronized (monitor) {
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
							// any transforms that arise are logical cascades
							// (owned by root) - allow
							ThreadlocalTransformManager.get()
									.setTransformsExplicitlyPermitted(true);
							InternalMetrics.get().startTracker(event,
									() -> describeEvent(event),
									InternalMetricTypeAlcina.service,
									Thread.currentThread().getName(),
									() -> true);
							listener.onDomainTransformRequestPersistence(event);
							if (persistenceEventType == DomainTransformPersistenceEventType.PRE_COMMIT) {
								TransformManager.get()
										.checkNoPendingTransforms();
							}
						} catch (RuntimeException rex) {
							logger.warn(
									"DEVEX::0 - Exception in persistenceListener - {}",
									rex);
							throwOrLogBasedOnEventPhase(persistenceEventType,
									rex);
						} finally {
							InternalMetrics.get().endTracker(event);
							ThreadlocalTransformManager.get()
									.setTransformsExplicitlyPermitted(false);
						}
					}
				}
			} finally {
				queue.onEventListenerFiringCompleted(event);
			}
		}
	}

	private long getLocalCommitTimeout() {
		if (LooseContext.has(CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS)) {
			return LooseContext.get(CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS);
		} else {
			return 10 * TimeConstants.ONE_SECOND_MS;
		}
	}

	private void throwOrLogBasedOnEventPhase(
			DomainTransformPersistenceEventType persistenceEventType,
			RuntimeException rex) {
		rex.printStackTrace();
		switch (persistenceEventType) {
		case PRE_COMMIT:
		case PRE_FLUSH:
			throw rex;
		default:
			break;
		}
	}

	String describeEvent(DomainTransformPersistenceEvent event) {
		return Ax
				.format("Persistence event: id: %s - %s - %s",
						Ax.first(event.getPersistedRequestIds()),
						event.getTransformPersistenceToken().getRequest()
								.getChunkUuidString(),
						event.getPersistenceEventType());
	}

	// FIXME - mvcc.cascade - add optional - actually remove all
	// cascadedtransforms
	// (replace with jobs where necessary)
	//
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
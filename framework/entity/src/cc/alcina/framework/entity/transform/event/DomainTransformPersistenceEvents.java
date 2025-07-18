package cc.alcina.framework.entity.transform.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.ServerClientInstance;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.OffThreadLogger;

public class DomainTransformPersistenceEvents
		implements DomainTransformPersistenceListener.Has {
	private static final String CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS = DomainTransformPersistenceEvents.class
			.getName() + ".CONTEXT_OVERRIDE_LOCAL_COMMIT_TIMEOUT_MS";

	public static final String CONTEXT_FIRING_EVENT = DomainTransformPersistenceEvents.class
			.getName() + ".CONTEXT_FIRING_EVENT";

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

	@Override
	public void addDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener) {
		listenerList.add(listener);
	}

	String describeEvent(DomainTransformPersistenceEvent event) {
		return Ax
				.format("Persistence event: id: %s - %s - %s",
						Ax.first(event.getPersistedRequestIds()),
						event.getTransformPersistenceToken().getRequest()
								.getChunkUuidString(),
						event.getPersistenceEventType());
	}

	public void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
		Preconditions.checkState(!LooseContext.is(CONTEXT_FIRING_EVENT));
		try {
			LooseContext.pushWithTrue(CONTEXT_FIRING_EVENT);
			fireDomainTransformPersistenceEvent0(event);
		} finally {
			LooseContext.pop();
		}
	}

	private void fireDomainTransformPersistenceEvent0(
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
		case PREPARE_COMMIT: {
			if (event.getTransformPersistenceToken()
					.isRequestorExternalToThisJvm()) {
				transformLocalIdSupport.runWithOffsetLocalIdCounter(
						() -> fireDomainTransformPersistenceEvent1(event));
			} else {
				event.getTransformPersistenceToken().getRequest().allRequests()
						.forEach(
								rq -> getQueue().onPreparingVmLocalRequest(rq));
				fireDomainTransformPersistenceEvent1(event);
			}
			break;
		}
		case PRE_COMMIT: {
			event.getPersistedRequests()
					.forEach(queue::onPersistedRequestPreFlushed);
			break;
		}
		case COMMIT_OK:
		case COMMIT_ERROR: {
			event.getPersistedRequests().forEach(
					request -> queue.onRequestDataReceived(request, false));
			if (event.isLocalToVm() && !event.isFiringFromQueue()) {
				event.getPersistedRequestIds().forEach(
						id -> queue.onTransformRequestCommitted(id, true));
			}
			fireDomainTransformPersistenceEvent1(event);
			break;
		}
		}
	}

	private void fireDomainTransformPersistenceEvent1(
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
					// deliberate fallthrough
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
		case PREPARE_COMMIT:
		case PRE_COMMIT:
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
							if (persistenceEventType == DomainTransformPersistenceEventType.PREPARE_COMMIT) {
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

	private void throwOrLogBasedOnEventPhase(
			DomainTransformPersistenceEventType persistenceEventType,
			RuntimeException rex) {
		rex.printStackTrace();
		switch (persistenceEventType) {
		case PREPARE_COMMIT:
		case PRE_COMMIT:
			throw rex;
		default:
			break;
		}
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
	/*
	 * Note re the fixmes - this feature has a potential (very rare) bug if
	 * there's a commit from an external source which causes cascaded object
	 * creation.
	 * 
	 * The solution is to maintain a cluster-level counter for the
	 * clientinstance, and reap it on instance expiration.
	 * 
	 * That counter could possibly be a field on the persistentrequest, as part
	 * of the persistence uniquness db constraints
	 */
	static class CascadedTransformLocalIdSupport {
		Map<Long, AtomicLong> perClientInstanceLocalIdCounter = new LinkedHashMap<>();

		synchronized void runWithOffsetLocalIdCounter(Runnable runnable) {
			ClientInstance serverAsClientInstance = ServerClientInstance.get();
			ClientInstance threadInstance = Permissions.get()
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
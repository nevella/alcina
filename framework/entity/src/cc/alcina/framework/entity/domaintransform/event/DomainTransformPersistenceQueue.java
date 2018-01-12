package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

/**
 * Improvement: rather than a strict dtrp-id queue, use 'happens after' field of
 * dtrp to allow out-of-sequence publishing
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = DomainTransformPersistenceQueue.class, implementationType = ImplementationType.SINGLETON)
public class DomainTransformPersistenceQueue implements RegistrableService {
	public static DomainTransformPersistenceQueue get() {
		return Registry.impl(DomainTransformPersistenceQueue.class);
	}

	protected TaggedLogger logger = Registry.impl(TaggedLoggers.class)
			.getLogger(getClass(), TaggedLogger.INFO);

	Set<Long> firing = new LinkedHashSet<>();

	// most recent event
	Set<Long> lastFired = new LinkedHashSet<>();

	Set<Long> firedOrQueued = new LinkedHashSet<>();

	LinkedList<Long> toFire = new LinkedList<>();

	Object queueModificationLock = new Object();

	AtomicInteger waiterCounter = new AtomicInteger();

	AtomicBoolean closed = new AtomicBoolean(false);

	CountDownLatch waiterLatch;

	private Thread eventQueue;

	@Override
	public void appShutdown() {
		closed.set(true);
		synchronized (queueModificationLock) {
			queueModificationLock.notifyAll();
		}
		synchronized (toFire) {
			toFire.notifyAll();
		}
	}

	public DomainTransformCommitPosition getTransformLogPosition() {
		synchronized (queueModificationLock) {
			return new DomainTransformCommitPosition(CommonUtils.first(lastFired),
					lastFired.size(), null);
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		synchronized (queueModificationLock) {
			firing.add(dtrp.getId());
		}
	}

	public void startEventQueue() {
		eventQueue = new Thread() {
			@Override
			public void run() {
				setName("DomainTransformPersistenceQueue-fire");
				ThreadedPermissionsManager.cast().pushSystemUser();
				PermissibleFieldFilter.disablePerObjectPermissions = true;
				while (!closed.get()) {
					try {
						Long id = null;
						synchronized (toFire) {
							if (toFire.isEmpty()) {
								toFire.wait();
							}
							if (!toFire.isEmpty()) {
								id = toFire.pop();
							}
						}
						if (id != null) {
							publishTransformEvent(id);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				PermissibleFieldFilter.disablePerObjectPermissions = false;
				ThreadedPermissionsManager.cast().popSystemUser();
			}

			protected void publishTransformEvent(Long id) {
				List<DomainTransformRequestPersistent> requests = runWithDisabledObjectPermissions(
						() -> getCommonPersistence()
								.getPersistentTransformRequests(0, 0,
										Collections.singletonList(id), false,
										true));
				DomainTransformRequestPersistent request = CommonUtils
						.first(requests);
				if (request != null) {
					DomainTransformPersistenceEvent event = createPersistenceEventFromPersistedRequest(
							request);
					Registry.impl(DomainTransformPersistenceEvents.class)
							.fireDomainTransformPersistenceEvent(event);
				}
			}
		};
		eventQueue.start();
	}

	public void transformRequestPublished(long id) {
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

	public void waitUntilCurrentRequestsProcessed() {
		waitUntilCurrentRequestsProcessed(60 * TimeConstants.ONE_SECOND_MS);
	}

	public void waitUntilCurrentRequestsProcessed(long timeoutMs) {
		new QueueWaiter().pauseUntilProcessed(timeoutMs);
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

	private <T> T
			runWithDisabledObjectPermissions(ThrowingSupplier<T> supplier) {
		try {
			// this prevents a deadlock where we might have a waiting write
			// preventing us from getting the lock
			LooseContext.pushWithTrue(AlcinaMemCache.CONTEXT_NO_LOCKS);
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

	protected CommonPersistenceLocal getCommonPersistence() {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence();
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
		synchronized (queueModificationLock) {
			lastFired = new LinkedHashSet<>(event.getPersistedRequestIds());
			waiterLatch = new CountDownLatch(waiterCounter.get());
			queueModificationLock.notifyAll();
		}
		try {
			waiterLatch.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	void transformRequestPublishedLocal(long id) {
		synchronized (queueModificationLock) {
			firedOrQueued.add(id);
			lastFired.add(id);
			firing.remove(id);
		}
	}

	class QueueWaiter {
		private Set<Long> waiting;

		public void pauseUntilProcessed(long timeoutMs) {
			synchronized (queueModificationLock) {
				waiting = new LinkedHashSet<>(firing);
			}
			long startTime = System.currentTimeMillis();
			while (true) {
				long timeRemaining = -System.currentTimeMillis() + startTime
						+ timeoutMs;
				synchronized (queueModificationLock) {
					try {
						if (waiting.isEmpty() || timeRemaining <= 0) {
							break;
						}
						waiterCounter.incrementAndGet();
						queueModificationLock.wait(timeRemaining);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
				waiting.removeAll(lastFired);
				waiterCounter.decrementAndGet();
				waiterLatch.countDown();
			}
		}
	}
}

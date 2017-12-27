package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

@RegistryLocation(registryPoint = DomainTransformPersistenceEvents.class, implementationType = ImplementationType.SINGLETON)
public class DomainTransformPersistenceEvents {
	private List<DomainTransformPersistenceListener> listenerList = new ArrayList<DomainTransformPersistenceListener>();

	private List<DomainTransformPersistenceListener> nonThreadListenerList = new ArrayList<DomainTransformPersistenceListener>();

	private DomainTransformPersistenceQueue queue;;

	public DomainTransformPersistenceEvents() {
		this.queue = Registry.impl(DomainTransformPersistenceQueue.class);
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

	public void removeDomainTransformPersistenceListener(
			DomainTransformPersistenceListener listener) {
		listenerList.remove(listener);
		nonThreadListenerList.remove(listener);
	}

	boolean initialisedPreCache = false;

	boolean startedQueue = false;

	public void initialisePreCache() {
		initialisedPreCache = true;
		queue.getMaxPersistentRequestBaseline();
	}

	public void startSequentialEventChecks() {
		startedQueue = true;
		queue.startup();
	}

	public void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
		if (!initialisedPreCache) {
			initialisePreCache();
		}
		if (!startedQueue) {
			startSequentialEventChecks();
		}
		queue.submit(event);
		while (!queue.shouldFire(event)) {
			try {
				synchronized (queue) {
					queue.wait();
				}
			} catch (InterruptedException e) {
			}
		}
		try {
			queue.logFiring(event);
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				// only fire ex-machine transforms to certain general listeners
				if (event.isLocalToVm()
						|| nonThreadListenerList.contains(listener)) {
					listener.onDomainTransformRequestPersistence(event);
				}
			}
		} finally {
			queue.logFired(event);
			queue.eventFired(event);
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		queue.registerPersisting(dtrp);
	}

	public synchronized void acquireCommitLock(boolean lock) {
		Registry.impl(DomainTransformPersistenceEventsCommitLock.class)
				.acquireCommitLock(lock);
	}

	@RegistryLocation(registryPoint = DomainTransformPersistenceEventsCommitLock.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainTransformPersistenceEventsCommitLock {
		public void acquireCommitLock(boolean lock) {
			//NOOP
		}
	}

	public long getMaxPublishedId() {
		return queue.getMaxDbPersistedRequestId();
	}
}
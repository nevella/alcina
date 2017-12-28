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

	private DomainTransformPersistenceQueue queue;

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

	public synchronized void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
		try {
			queue.logFiring(event);
			if(event.isLocalToVm()&&event.getPersistedRequestIds()!=null){
				event.getPersistedRequestIds().forEach(queue::transformRequestPublishedLocal);
			}
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
		}
	}

	public void startEventQueue() {
		queue.startEventQueue();
	}
}
package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrySingleton;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

//TODO - Jira - events must be fired in DomainTransformRequestPersistent.id order - fairly important
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

	public void initialisePreCache() {
		queue.getMaxPersistentRequestBaseline();
	}

	public void startSequentialEventChecks() {
		queue.startup();
	}

	public void fireDomainTransformPersistenceEvent(
			DomainTransformPersistenceEvent event) {
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
			for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
					listenerList)) {
				//ex-machine transforms
				if(event.getSourceThreadId()!=Thread.currentThread().getId()){
					if(!nonThreadListenerList.contains(listener)){
						continue;
					}
				}
				listener.onDomainTransformRequestPersistence(event);
			}
		} finally {
			queue.eventFired(event);
		}
	}

	public void registerPersisting(DomainTransformRequestPersistent dtrp) {
		queue.registerPersisting(dtrp);
	}
}
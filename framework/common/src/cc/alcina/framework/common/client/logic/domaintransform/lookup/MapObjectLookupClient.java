package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;

/**
 * 
 * 
 * @param hasIdAndLocalId
 * @param obj
 */
public class MapObjectLookupClient extends MapObjectLookup {
	private Multimap<Class, List<ClientPropertyReflector>> registerChildren = new Multimap<Class, List<ClientPropertyReflector>>();

	private int registerCounter;

	LinkedList<Entity> toRegister = new LinkedList<Entity>();

	private ScheduledCommand postRegisterCommand;

	public MapObjectLookupClient(PropertyChangeListener listener) {
		this.listener = listener;
	}

	public void clearReflectionCache() {
		// because different code modules may have different reflection data
		registerChildren.clear();
	}

	@Override
	public synchronized void mapObject(Entity obj) {
		mappedObjects = new PerClassLookup();
		addObjectOrCollectionToEndOfQueue(obj);
		iterateRegistration();
		// assert toRegister.isEmpty();
		// it'd be nice, but not always possible to assert this (counterexample:
		// using a
		// complicated handshake, say, where code can map objects while a big
		// block is still being registered)
	}

	public synchronized void registerAsync(Collection registerableDomainObjects,
			final ScheduledCommand postRegisterCommand) {
		mappedObjects = new PerClassLookup();
		for (Object o : registerableDomainObjects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		Scheduler.get().scheduleIncremental(new RepeatingCommand() {
			// private int ctr;
			@Override
			public boolean execute() {
				try {
					if (iterateRegistration()) {
						return true;
					}
				} catch (Exception e1) {
					throw new AsyncRegistrationException(e1);
				}
				try {
					postRegisterCommand.execute();
					return false;
				} catch (Exception e) {
					Registry.impl(ClientExceptionHandler.class)
							.handleException(new WrappedRuntimeException(
									"Exception in post-register command", e));
					return false;
				}
			}
		});
	}

	@Override
	public synchronized void registerObjects(Collection objects) {
		mappedObjects = new PerClassLookup();
		for (Object o : objects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		iterateRegistration();
	}

	private synchronized void mapObjectFromFrontOfQueue() {
		Entity obj = toRegister.removeFirst();
		if ((obj.getId() == 0 && obj.getLocalId() == 0)
				|| mappedObjects.contains(obj)) {
			return;
		}
		Class<? extends Entity> clazz = obj.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.put(obj, obj.getId() == 0);
		if (obj instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) obj;
			sb.removePropertyChangeListener(listener);
			sb.addPropertyChangeListener(listener);
		}
		boolean lookupCreated = registerChildren.containsKey(clazz);
		if (!registerChildren.containsKey(clazz)) {
			ClientBeanReflector bi = ClientReflector.get()
					.beanInfoForClass(clazz);
			Collection<ClientPropertyReflector> prs = bi == null
					? new ArrayList<ClientPropertyReflector>()
					: bi.getPropertyReflectors().values();
			List<ClientPropertyReflector> target = new ArrayList<ClientPropertyReflector>();
			registerChildren.put(clazz, target);
			for (ClientPropertyReflector pr : prs) {
				DomainProperty dpi = pr.getAnnotation(DomainProperty.class);
				if (dpi != null && dpi.registerChildren()) {
					target.add(pr);
				}
			}
		}
		List<ClientPropertyReflector> childRegisterReflectors = registerChildren
				.get(clazz);
		if (!childRegisterReflectors.isEmpty()) {
			for (ClientPropertyReflector pr : childRegisterReflectors) {
				Object value = Reflections.propertyAccessor()
						.getPropertyValue(obj, pr.getPropertyName());
				addObjectOrCollectionToEndOfQueue(value);
			}
		}
		mappedObjects.put(obj);
	}

	synchronized void addObjectOrCollectionToEndOfQueue(Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Collection) {
			for (Entity child : (Collection<Entity>) o) {
				toRegister.add(child);
			}
		} else {
			toRegister.add((Entity) o);
		}
	}

	synchronized boolean iterateRegistration() {
		registerCounter = 0;
		while (!toRegister.isEmpty()
				&& (postRegisterCommand == null || registerCounter++ < 500)) {
			mapObjectFromFrontOfQueue();
		}
		return !toRegister.isEmpty();
	}

	public static class AsyncRegistrationException extends RuntimeException {
		public AsyncRegistrationException(Exception e1) {
			super(e1);
		}
	}
}
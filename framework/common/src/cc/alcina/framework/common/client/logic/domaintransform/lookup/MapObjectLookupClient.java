package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

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
 * <h2>Thread-safety</h2>
 * <ul>
 * <li>All methods (mostly wrapping superclass methods) that access internal
 * state directly are synchronized
 * <li>Return collections must be iterated across/accessed within a block
 * synchroized on 'this'
 * </ul>
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

	@Override
	public synchronized Set<Entity> allValues() {
		return super.allValues();
	}

	@Override
	public synchronized void changeMapping(Entity obj, long id, long localId) {
		super.changeMapping(obj, id, localId);
	}

	public void clearReflectionCache() {
		// because different code modules may have different reflection data
		registerChildren.clear();
	}

	@Override
	public synchronized void deregisterObject(Entity entity) {
		super.deregisterObject(entity);
	}

	@Override
	/*
	 * the map itself is a replica of the internal structure, so thread-safe -
	 * the entity collections require synchronization on this
	 */
	public synchronized Map<Class<? extends Entity>, Collection<Entity>>
			getCollectionMap() {
		return super.getCollectionMap();
	}

	@Override
	public synchronized <T extends Entity> T getObject(Class<? extends T> c,
			long id, long localId) {
		return super.getObject(c, id, localId);
	}

	@Override
	public synchronized void invalidate(Class<? extends Entity> clazz) {
		super.invalidate(clazz);
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
		Entity entity = toRegister.removeFirst();
		if ((entity.getId() == 0 && entity.getLocalId() == 0)
				|| mappedObjects.contains(entity)) {
			return;
		}
		Class<? extends Entity> clazz = entity.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.put(entity, entity.getId() == 0);
		entity.removePropertyChangeListener(listener);
		entity.addPropertyChangeListener(listener);
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
						.getPropertyValue(entity, pr.getPropertyName());
				addObjectOrCollectionToEndOfQueue(value);
			}
		}
		mappedObjects.put(entity);
	}

	@Override
	protected synchronized FastIdLookup ensureLookup(Class c) {
		return super.ensureLookup(c);
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
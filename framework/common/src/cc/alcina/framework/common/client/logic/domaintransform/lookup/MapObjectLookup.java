package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 * 
 * 
 * @param hasIdAndLocalId
 * @param obj
 */
public class MapObjectLookup implements ObjectLookup {
	private static final String REGISTERING_OBJECTS = "Registering objects";

	private final PropertyChangeListener listener;

	private PerClassLookup perClassLookups;

	private Multimap<Class, List<ClientPropertyReflector>> registerChildren = new Multimap<Class, List<ClientPropertyReflector>>();

	private Map<Class, FastIdInfo> fastInfo = new HashMap<Class, FastIdInfo>();

	private int registerCounter;

	LinkedList<HasIdAndLocalId> toRegister = new LinkedList<HasIdAndLocalId>();

	private PerClassLookup mappedObjects;

	private ScheduledCommand postRegisterCommand;

	public MapObjectLookup(PropertyChangeListener listener) {
		this.listener = listener;
		this.perClassLookups = new PerClassLookup();
	}

	class PerClassLookup {
		private Map<Class<? extends HasIdAndLocalId>, FastIdLookup> lookups = new LinkedHashMap<Class<? extends HasIdAndLocalId>, FastIdLookup>();

		FastIdLookup ensureLookup(Class c) {
			FastIdLookup lookup = lookups.get(c);
			if (lookup == null) {
				lookup = createIdLookup(c);
				lookups.put(c, lookup);
			}
			return lookup;
		}

		FastIdLookup getLookup(Class c) {
			return lookups.get(c);
		}

		public boolean contains(HasIdAndLocalId obj) {
			FastIdLookup lookup = ensureLookup(obj.getClass());
			return lookup.values().contains(obj);
		}

		public void put(HasIdAndLocalId obj) {
			FastIdLookup lookup = ensureLookup(obj.getClass());
			lookup.values().add(obj);
		}

		public Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollnMap() {
			Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> result = new LinkedHashMap<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>>();
			for (Entry<Class<? extends HasIdAndLocalId>, FastIdLookup> entry : lookups
					.entrySet()) {
				result.put(entry.getKey(), entry.getValue().values());
			}
			return result;
		}
	}

	public FastIdLookup createIdLookup(Class c) {
		return GWT.isScript() ? new FastIdLookupScript(c, fastInfo.get(c))
				: new FastIdLookupDev(c, fastInfo.get(c));
	}

	public void changeMapping(HasIdAndLocalId obj, long id, long localId) {
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.remove(id, false);
		lookup.remove(localId, true);
		// see discussion in AbstractDomainBase - nuffink's perfect
		// collnMap.get(clazz).remove(obj);
		// if (obj instanceof AbstractDomainBase) {
		// AbstractDomainBase adb = (AbstractDomainBase) obj;
		// adb.clearHash();
		// }
		mapObject(obj);
	}

	public void deregisterObject(HasIdAndLocalId hili) {
		if (hili == null) {
			return;
		}
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.remove(hili.getId(), false);
		lookup.remove(hili.getLocalId(), true);
		if (hili instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) hili;
			sb.removePropertyChangeListener(listener);
		}
	}

	public void deregisterObjects(Collection<HasIdAndLocalId> objects) {
		if (objects == null) {
			return;
		}
		for (HasIdAndLocalId hili : objects) {
			deregisterObject(hili);
		}
	}

	public Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollnMap() {
		return this.perClassLookups.getCollnMap();
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		FastIdLookup lookup = perClassLookups.getLookup(c);
		if (lookup == null) {
			return null;
		}
		if (id != 0) {
			return (T) lookup.get(id, false);
		} else {
			return (T) lookup.get(localId, true);
		}
	}

	public HasIdAndLocalId getObject(HasIdAndLocalId bean) {
		return (HasIdAndLocalId) getObject(bean.getClass(), bean.getId(),
				bean.getLocalId());
	}

	public void registerAsync(List registerableDomainObjects,
			ScheduledCommand postRegisterCommand) {
		this.postRegisterCommand = postRegisterCommand;
		mappedObjects = new PerClassLookup();
		for (Object o : registerableDomainObjects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		final ModalNotifier notifier = ClientLayerLocator.get().notifications()
				.getModalNotifier(REGISTERING_OBJECTS);
		notifier.modalOn();
		Scheduler.get().scheduleIncremental(new RepeatingCommand() {
			private int ctr;

			@Override
			public boolean execute() {
				if (iterateRegistration()) {
					System.out.println("Async register obj:" + ctr++);
					return true;
				}
				try {
					notifier.modalOff();
					ScheduledCommand postRegisterCommandCopy = MapObjectLookup.this.postRegisterCommand;
					MapObjectLookup.this.postRegisterCommand = null;
					postRegisterCommandCopy.execute();
					return false;
				} catch (Exception e) {
					ClientLayerLocator
							.get()
							.exceptionHandler()
							.handleException(
									new WrappedRuntimeException(
											"Exception in post-register command",
											e));
					return false;
				}
			}
		});
	}

	public void registerObjects(Collection objects) {
		mappedObjects = new PerClassLookup();
		for (Object o : objects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		iterateRegistration();
	}

	private void addObjectOrCollectionToEndOfQueue(Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Collection) {
			for (HasIdAndLocalId child : (Collection<HasIdAndLocalId>) o) {
				toRegister.add(child);
			}
		} else {
			toRegister.add((HasIdAndLocalId) o);
		}
	}

	private FastIdLookup ensureLookup(Class c) {
		return perClassLookups.ensureLookup(c);
	}

	private boolean iterateRegistration() {
		registerCounter = 0;
		while (!toRegister.isEmpty()
				&& (postRegisterCommand == null || registerCounter++ < 500)) {
			mapObjectFromFrontOfQueue();
		}
		return !toRegister.isEmpty();
	}

	private void mapObjectFromFrontOfQueue() {
		HasIdAndLocalId obj = toRegister.removeFirst();
		if ((obj.getId() == 0 && obj.getLocalId() == 0)
				|| mappedObjects.contains(obj)) {
			return;
		}
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.put(obj, obj.getId() == 0);
		if (obj instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) obj;
			sb.removePropertyChangeListener(listener);
			sb.addPropertyChangeListener(listener);
		}
		boolean lookupCreated = registerChildren.containsKey(clazz);
		if (!registerChildren.containsKey(clazz)) {
			ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
					clazz);
			Collection<ClientPropertyReflector> prs = bi == null ? new ArrayList<ClientPropertyReflector>()
					: bi.getPropertyReflectors().values();
			List<ClientPropertyReflector> target = new ArrayList<ClientPropertyReflector>();
			registerChildren.put(clazz, target);
			for (ClientPropertyReflector pr : prs) {
				DomainPropertyInfo dpi = pr
						.getAnnotation(DomainPropertyInfo.class);
				if (dpi != null && dpi.registerChildren()) {
					target.add(pr);
				}
			}
		}
		List<ClientPropertyReflector> childRegisterReflectors = registerChildren
				.get(clazz);
		if (!childRegisterReflectors.isEmpty()) {
			for (ClientPropertyReflector pr : childRegisterReflectors) {
				Object value = CommonLocator.get().propertyAccessor()
						.getPropertyValue(obj, pr.getPropertyName());
				addObjectOrCollectionToEndOfQueue(value);
			}
		}
		mappedObjects.put(obj);
	}

	public void mapObject(HasIdAndLocalId obj) {
		mappedObjects = new PerClassLookup();
		addObjectOrCollectionToEndOfQueue(obj);
		iterateRegistration();
		// assert toRegister.isEmpty();
		// it'd be nice, but not always possible to assert this (counterexample:
		// using a
		// complicated handshake, say, where code can map objects while a big
		// block is still being registered)
	}

	public void removeListeners() {
		for (FastIdLookup lookup : perClassLookups.lookups.values()) {
			for (HasIdAndLocalId o : lookup.values()) {
				if (o instanceof SourcesPropertyChangeEvents) {
					SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) o;
					sb.removePropertyChangeListener(listener);
				}
			}
		}
	}

	public <T> Collection<T> getCollection(Class<T> clazz) {
		return (Collection<T>) ensureLookup(clazz).values();
	}
}
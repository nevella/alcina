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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

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
public class MapObjectLookupClient extends MapObjectLookup {
	private Multimap<Class, List<ClientPropertyReflector>> registerChildren = new Multimap<Class, List<ClientPropertyReflector>>();

	private int registerCounter;

	LinkedList<HasIdAndLocalId> toRegister = new LinkedList<HasIdAndLocalId>();

	private ScheduledCommand postRegisterCommand;

	public MapObjectLookupClient(PropertyChangeListener listener) {
		this.listener = listener;
	}

	

	public void registerAsync(Collection registerableDomainObjects,
			ScheduledCommand postRegisterCommand) {
		this.postRegisterCommand = postRegisterCommand;
		mappedObjects = new PerClassLookup();
		for (Object o : registerableDomainObjects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		Scheduler.get().scheduleIncremental(new RepeatingCommand() {
			// private int ctr;
			@Override
			public boolean execute() {
				if (iterateRegistration()) {
					// System.out.println("Async register obj:" + ctr++);
					return true;
				}
				try {
					ScheduledCommand postRegisterCommandCopy = MapObjectLookupClient.this.postRegisterCommand;
					MapObjectLookupClient.this.postRegisterCommand = null;
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

	void addObjectOrCollectionToEndOfQueue(Object o) {
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

	boolean iterateRegistration() {
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

	@Override
	public void registerObjects(Collection objects) {
		mappedObjects = new PerClassLookup();
		for (Object o : objects) {
			addObjectOrCollectionToEndOfQueue(o);
		}
		iterateRegistration();
	}

	@Override
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
}
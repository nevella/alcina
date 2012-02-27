package cc.alcina.framework.common.client.logic.domaintransform;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 * 
 * 
 * @param hasIdAndLocalId
 * @param obj
 */
public class MapObjectLookup implements ObjectLookup {
	private final PropertyChangeListener listener;

	private Map<Class<? extends HasIdAndLocalId>, Map<LongWrapperHash, HasIdAndLocalId>> idMap;

	private Map<Class<? extends HasIdAndLocalId>, Map<LongWrapperHash, HasIdAndLocalId>> localIdMap;

	private Map<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>> collnMap;

	private Multimap<Class, List<ClientPropertyReflector>> registerChildren = new Multimap<Class, List<ClientPropertyReflector>>();

	public static class LongWrapperHash {
		private final long value;

		private int hash;

		public LongWrapperHash(long value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			if (hash == 0) {
				hash = GWT.isScript() ? fastHash(value) : Long.valueOf(value)
						.hashCode();
				if (hash == 0) {
					hash = -1;
				}
			}
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LongWrapperHash) {
				return ((LongWrapperHash) obj).value == value;
			}
			return false;
		}

		@UnsafeNativeLong
		private native int fastHash(long value)/*-{
			return value.l ^ value.m ^ value.h;
		}-*/;
	}

	public MapObjectLookup(PropertyChangeListener listener, List topLevelObjects) {
		this.listener = listener;
		this.idMap = new HashMap<Class<? extends HasIdAndLocalId>, Map<LongWrapperHash, HasIdAndLocalId>>();
		this.localIdMap = new HashMap<Class<? extends HasIdAndLocalId>, Map<LongWrapperHash, HasIdAndLocalId>>();
		this.collnMap = new HashMap<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>>();
		registerObjects(topLevelObjects);
	}

	public void changeMapping(HasIdAndLocalId obj, long id, long localId) {
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		ensureCollections(clazz);
		idMap.get(clazz).remove(new LongWrapperHash(id));
		localIdMap.get(clazz).remove(new LongWrapperHash(localId));
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
		ensureCollections(clazz);
		idMap.get(clazz).remove(new LongWrapperHash(hili.getId()));
		localIdMap.get(clazz).remove(new LongWrapperHash(hili.getLocalId()));
		collnMap.get(clazz).remove(hili);
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

	<T> Collection<T> getCollection(Class<T> clazz) {
		ensureCollections(clazz);
		return (Collection<T>) collnMap.get(clazz);
	}

	public Map<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>> getCollnMap() {
		return this.collnMap;
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (idMap.get(c) == null) {
			return null;
		}
		if (id != 0) {
			return (T) idMap.get(c).get(new LongWrapperHash(id));
		} else {
			return (T) localIdMap.get(c).get(new LongWrapperHash(localId));
		}
	}

	public HasIdAndLocalId getObject(HasIdAndLocalId bean) {
		return (HasIdAndLocalId) getObject(bean.getClass(), bean.getId(),
				bean.getLocalId());
	}

	public void registerObjects(Collection objects) {
		for (Object o : objects) {
			mapObjectOrCollection(o);
		}
	}

	private void mapObjectOrCollection(Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Collection) {
			flattenCollection((Collection) o);
		} else {
			mapObject((HasIdAndLocalId) o);
		}
	}

	private void removeListenerFromMap(
			Map<Class<? extends HasIdAndLocalId>, Map<LongWrapperHash, HasIdAndLocalId>> map) {
		for (Map m : map.values()) {
			for (Object o : m.values()) {
				if (o instanceof SourcesPropertyChangeEvents) {
					SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) o;
					sb.removePropertyChangeListener(listener);
				}
			}
		}
	}

	protected void flattenCollection(Collection c) {
		if (c == null) {
			return;
		}
		Collection<HasIdAndLocalId> ch = c;
		for (HasIdAndLocalId obj : ch) {
			mapObject(obj);
		}
	}

	protected void mapObject(HasIdAndLocalId obj) {
		if (obj.getId() == 0 && obj.getLocalId() == 0) {
			return;
		}
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		ensureCollections(clazz);
		collnMap.get(clazz).remove(obj);
		collnMap.get(clazz).add(obj);
		if (obj.getId() != 0) {
			Map<LongWrapperHash, HasIdAndLocalId> clMap = idMap.get(clazz);
			clMap.put(new LongWrapperHash(obj.getId()), obj);
		}
		if (obj.getLocalId() != 0) {
			Map<LongWrapperHash, HasIdAndLocalId> clMap = localIdMap.get(clazz);
			clMap.put(new LongWrapperHash(obj.getLocalId()), obj);
		}
		if (obj instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) obj;
			sb.removePropertyChangeListener(listener);
			sb.addPropertyChangeListener(listener);
		}
		boolean lookupCreated = registerChildren.containsKey(clazz);
		if (ClientReflector.get().isDefined()) {
			if (!registerChildren.containsKey(clazz)) {
				ClientBeanReflector bi = ClientReflector.get()
						.beanInfoForClass(clazz);
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
					mapObjectOrCollection(value);
				}
			}
		}
	}

	protected void removeListeners() {
		removeListenerFromMap(idMap);
		removeListenerFromMap(localIdMap);
	}

	void ensureCollections(Class c) {
		if (!idMap.containsKey(c)) {
			idMap.put(c, new LinkedHashMap<LongWrapperHash, HasIdAndLocalId>());
			localIdMap.put(c,
					new LinkedHashMap<LongWrapperHash, HasIdAndLocalId>());
			collnMap.put(c, new LinkedHashSet<HasIdAndLocalId>());
		}
	}
}
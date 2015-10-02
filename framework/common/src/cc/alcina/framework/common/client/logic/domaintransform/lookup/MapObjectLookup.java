package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

public abstract class MapObjectLookup implements ObjectStore {
	protected PropertyChangeListener listener;

	protected PerClassLookup perClassLookups;

	protected PerClassLookup mappedObjects;

	public MapObjectLookup() {
		super();
		this.perClassLookups = new PerClassLookup();
	}

	@Override
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

	public FastIdLookup createIdLookup(Class c) {
		return GWT.isScript() ? new FastIdLookupScript()
				: new FastIdLookupJvm();
	}

	@Override
	public boolean contains(HasIdAndLocalId obj) {
		return getObject(obj) != null;
	}

	@Override
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

	@Override
	public void deregisterObjects(Collection<HasIdAndLocalId> objects) {
		if (objects == null) {
			return;
		}
		for (HasIdAndLocalId hili : objects) {
			deregisterObject(hili);
		}
	}

	@Override
	public <T> Collection<T> getCollection(Class<T> clazz) {
		return (Collection<T>) ensureLookup(clazz).values();
	}

	public <T> Collection<T> values(Class<T> clazz) {
		return (Collection<T>) ensureLookup(clazz).values();
	}

	@Override
	public Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollectionMap() {
		return this.perClassLookups.getCollnMap();
	}

	@Override
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

	@Override
	public HasIdAndLocalId getObject(HasIdAndLocalId bean) {
		return (HasIdAndLocalId) getObject(bean.getClass(), bean.getId(),
				bean.getLocalId());
	}

	@Override
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

	protected FastIdLookup ensureLookup(Class c) {
		return perClassLookups.ensureLookup(c);
	}

	class PerClassLookup {
		Map<Class<? extends HasIdAndLocalId>, FastIdLookup> lookups = new LinkedHashMap<Class<? extends HasIdAndLocalId>, FastIdLookup>(100);

		public boolean contains(HasIdAndLocalId obj) {
			FastIdLookup lookup = ensureLookup(obj.getClass());
			return lookup.values().contains(obj);
		}

		public Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollnMap() {
			Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> result = new LinkedHashMap<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>>();
			for (Entry<Class<? extends HasIdAndLocalId>, FastIdLookup> entry : lookups
					.entrySet()) {
				result.put(entry.getKey(), entry.getValue().values());
			}
			return result;
		}

		public void put(HasIdAndLocalId obj) {
			FastIdLookup lookup = ensureLookup(obj.getClass());
			lookup.put(obj, obj.getId() == 0);
		}

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
	}

	public Set<HasIdAndLocalId> allValues() {
		Set<HasIdAndLocalId> result = new LinkedHashSet<HasIdAndLocalId>();
		for (Collection<HasIdAndLocalId> collection : getCollectionMap()
				.values()) {
			result.addAll(collection);
		}
		return result;
	}

	@Override
	public void invalidate(Class<? extends HasIdAndLocalId> clazz) {
		perClassLookups.lookups.remove(clazz);
		perClassLookups.ensureLookup(clazz);
	}
}
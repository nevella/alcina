package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;

public abstract class MapObjectLookup implements ObjectStore {
	protected PropertyChangeListener listener;

	protected PerClassLookup perClassLookups;

	protected PerClassLookup mappedObjects;

	public MapObjectLookup() {
		super();
		this.perClassLookups = new PerClassLookup();
	}

	public Set<Entity> allValues() {
		Set<Entity> result = new LinkedHashSet<Entity>();
		for (Collection<Entity> collection : getCollectionMap()
				.values()) {
			result.addAll(collection);
		}
		return result;
	}

	@Override
	public void changeMapping(Entity obj, long id, long localId) {
		Class<? extends Entity> clazz = obj.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.remove(id, false);
		lookup.remove(localId, true);
		// see discussion in Entity - nuffink's perfect
		// collnMap.get(clazz).remove(obj);
		// if (obj instanceof Entity) {
		// Entity adb = (Entity) obj;
		// adb.clearHash();
		// }
		mapObject(obj);
	}

	@Override
	public boolean contains(Class<? extends Entity> clazz, long id) {
		return getObject(clazz, id, 0L) != null;
	}

	@Override
	public boolean contains(Entity obj) {
		return getObject(obj) != null;
	}

	public FastIdLookup createIdLookup(Class c) {
		return GWT.isScript() ? new FastIdLookupScript()
				: new FastIdLookupJvm();
	}

	@Override
	public void deregisterObject(Entity entity) {
		if (entity == null) {
			return;
		}
		Class<? extends Entity> clazz = entity.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.remove(entity.getId(), false);
		lookup.remove(entity.getLocalId(), true);
		if (entity instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) entity;
			sb.removePropertyChangeListener(listener);
		}
	}

	@Override
	public void deregisterObjects(Collection<Entity> objects) {
		if (objects == null) {
			return;
		}
		for (Entity entity : objects) {
			deregisterObject(entity);
		}
	}

	@Override
	public <T> Collection<T> getCollection(Class<T> clazz) {
		return (Collection<T>) ensureLookup(clazz).values();
	}

	@Override
	public Map<Class<? extends Entity>, Collection<Entity>>
			getCollectionMap() {
		return this.perClassLookups.getCollnMap();
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c,
			long id, long localId) {
		FastIdLookup lookup = perClassLookups.getLookup(c);
		if (lookup == null) {
			return null;
		}
		T t = null;
		if (id != 0) {
			t = (T) lookup.get(id, false);
		}
		if (t == null && localId != 0) {
			t = (T) lookup.get(localId, true);
		}
		return t;
	}

	@Override
	public Entity getObject(Entity bean) {
		return (Entity) getObject(bean.getClass(), bean.getId(),
				bean.getLocalId());
	}

	@Override
	public void invalidate(Class<? extends Entity> clazz) {
		perClassLookups.lookups.remove(clazz);
		perClassLookups.ensureLookup(clazz);
	}

	@Override
	public void removeListeners() {
		for (FastIdLookup lookup : perClassLookups.lookups.values()) {
			for (Entity o : lookup.values()) {
				if (o instanceof SourcesPropertyChangeEvents) {
					SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) o;
					sb.removePropertyChangeListener(listener);
				}
			}
		}
	}

	public <T> Collection<T> values(Class<T> clazz) {
		return (Collection<T>) ensureLookup(clazz).values();
	}

	protected FastIdLookup ensureLookup(Class c) {
		return perClassLookups.ensureLookup(c);
	}

	class PerClassLookup {
		Map<Class<? extends Entity>, FastIdLookup> lookups = new LinkedHashMap<Class<? extends Entity>, FastIdLookup>(
				100);

		public boolean contains(Entity obj) {
			FastIdLookup lookup = ensureLookup(obj.getClass());
			return lookup.values().contains(obj);
		}

		public Map<Class<? extends Entity>, Collection<Entity>>
				getCollnMap() {
			Map<Class<? extends Entity>, Collection<Entity>> result = new LinkedHashMap<Class<? extends Entity>, Collection<Entity>>();
			for (Entry<Class<? extends Entity>, FastIdLookup> entry : lookups
					.entrySet()) {
				result.put(entry.getKey(), entry.getValue().values());
			}
			return result;
		}

		public void put(Entity obj) {
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
}
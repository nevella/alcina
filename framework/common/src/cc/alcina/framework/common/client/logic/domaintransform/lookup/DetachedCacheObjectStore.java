package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;

public class DetachedCacheObjectStore implements ObjectStore {
	DetachedEntityCache cache;

	private LazyObjectLoader lazyObjectLoader;

	public DetachedCacheObjectStore(DetachedEntityCache cache) {
		this.cache = cache;
	}

	@Override
	public void changeMapping(Entity obj, long id, long localId) {
		// noop - doesn't support localids
	}

	@Override
	public boolean contains(Class<? extends Entity> clazz, long id) {
		return cache.contains(clazz, id);
	}

	@Override
	public boolean contains(Entity obj) {
		return getObject(obj) != null;
	}

	@Override
	public void deregister(Entity entity) {
		// just remove
		if (entity == null) {
			return;
		}
		cache.remove(entity);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	@Override
	// FIXME - reflection - probably remove (cache.values discouraged)
	public <T> Collection<T> getCollection(Class<T> clazz) {
		return (Collection<T>) cache.stream(clazz).collect(Collectors.toSet());
	}

	@Override
	// FIXME - mvcc.adjunct - for local-only domains, this is problematic.
	// Probably better to return Map<Class<? extends Entity>, Stream<Entity>>
	public Map<Class<? extends Entity>, Collection<Entity>> getCollectionMap() {
		return (Map) cache.getDomain();
	}

	public LazyObjectLoader getLazyObjectLoader() {
		return this.lazyObjectLoader;
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId) {
		T t = cache.get(c, id);
		if (t == null && lazyObjectLoader != null && id > 0) {
			lazyObjectLoader.loadObject(c, id, localId);
			t = cache.get(c, id);
		}
		return t;
	}

	@Override
	public <T extends Entity> T getObject(T entity) {
		return (T) (entity == null ? null
				: getObject(entity.entityClass(), entity.getId(), 0));
	}

	@Override
	public void invalidate(Class<? extends Entity> clazz) {
		cache.invalidate(clazz);
	}

	@Override
	public void mapObject(Entity obj) {
		cache.put(obj);
	}

	@Override
	public void registerObjects(Collection objects) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeListeners() {
		// noop - doesn't listen
	}

	public void setLazyObjectLoader(LazyObjectLoader lazyObjectLoader) {
		this.lazyObjectLoader = lazyObjectLoader;
	}
}

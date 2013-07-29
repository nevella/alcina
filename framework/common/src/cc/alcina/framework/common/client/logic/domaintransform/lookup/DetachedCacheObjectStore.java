package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;

public class DetachedCacheObjectStore implements ObjectStore {
	DetachedEntityCache cache;

	public DetachedCacheObjectStore(DetachedEntityCache cache) {
		this.cache = cache;
	}

	@Override
	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		return cache.get(c, id);
	}

	@Override
	public <T extends HasIdAndLocalId> T getObject(T bean) {
		return (T) (bean == null ? null : getObject(bean.getClass(),
				bean.getId(), 0));
	}

	@Override
	public <T> Collection<T> getCollection(Class<T> clazz) {
		return (Collection<T>) cache.values(clazz);
	}

	@Override
	public void removeListeners() {
		// noop - doesn't listen
	}

	@Override
	public void mapObject(HasIdAndLocalId obj) {
		cache.put(obj);
	}

	@Override
	public void registerObjects(Collection objects) {
		throw new UnsupportedOperationException();
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
	public void deregisterObject(HasIdAndLocalId hili) {
		// just remove
		cache.remove(hili);
	}

	@Override
	public void changeMapping(HasIdAndLocalId obj, long id, long localId) {
		// noop - doesn't support localids
	}

	@Override
	public Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollectionMap() {
		return (Map) cache.getDetached();
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	@Override
	public boolean contains(HasIdAndLocalId obj) {
		return getObject(obj) != null;
	}
}

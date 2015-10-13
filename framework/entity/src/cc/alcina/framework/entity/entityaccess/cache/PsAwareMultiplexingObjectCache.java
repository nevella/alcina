package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.cache.CacheItemDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;

class PsAwareMultiplexingObjectCache extends DetachedEntityCache {
	private volatile boolean committing;

	private DetachedEntityCacheArrayBacked main = new DetachedEntityCacheArrayBacked();

	private List<PropertyStoreCacheWrapper> psWrappers=new ArrayList<>();

	public void addPropertyStore(CacheItemDescriptor descriptor) {
		psWrappers.add(new PropertyStoreCacheWrapper(
				(PropertyStoreItemDescriptor) descriptor));
	}

	@Override
	public Set<HasIdAndLocalId> allValues() {
		return main.allValues();
	}

	@Override
	public void clear() {
		main.clear();
	}

	@Override
	public DetachedEntityCache clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(HasIdAndLocalId hili) {
		return main.contains(hili);
	}

	@Override
	public Map<Long, HasIdAndLocalId> createMap() {
		return main.createMap();
	}

	@Override
	public <T> T get(Class<T> clazz, Long id) {
		return getSubCache(clazz).get(clazz, id);
	}

	@Override
	public Map<Class, Map<Long, HasIdAndLocalId>> getDetached() {
		return main.getDetached();
	}

	@Override
	public Map<Long, HasIdAndLocalId> getMap(Class clazz) {
		return main.getMap(clazz);
	}

	@Override
	public <T> Collection<T> immutableRawValues(Class<T> clazz) {
		return main.immutableRawValues(clazz);
	}

	@Override
	public void invalidate(Class clazz) {
		main.invalidate(clazz);
	}

	@Override
	public void invalidate(Class[] classes) {
		main.invalidate(classes);
	}

	@Override
	public boolean isEmpty(Class clazz) {
		return main.isEmpty(clazz);
	}

	@Override
	public Set<Long> keys(Class clazz) {
		return main.keys(clazz);
	}

	@Override
	public List<Long> notContained(Collection<Long> ids, Class clazz) {
		return main.notContained(ids, clazz);
	}

	@Override
	public void put(HasIdAndLocalId hili) {
		getSubCache(hili.getClass()).put(hili);
	}

	@Override
	public void putAll(Class clazz, Collection<? extends HasIdAndLocalId> values) {
		main.putAll(clazz, values);
	}

	@Override
	public void putForSuperClass(Class clazz, HasIdAndLocalId hili) {
		main.putForSuperClass(clazz, hili);
	}

	@Override
	public void remove(HasIdAndLocalId hili) {
		getSubCache(hili.getClass()).remove(hili);
	}

	@Override
	public int size(Class clazz) {
		return main.size(clazz);
	}

	@Override
	public String sizes() {
		return main.sizes();
	}

	@Override
	public <T> Set<T> values(Class<T> clazz) {
		return main.values(clazz);
	}

	private <T> MultiplexableCache getSubCache(Class<T> clazz) {
		if (!committing) {
			return main;
		} else {
			for (PropertyStoreCacheWrapper psWrapper : psWrappers) {
				if (psWrapper.getCachedClass() == clazz) {
					return psWrapper;
				}
			}
			return main;
		}
	}

	@Override
	protected void ensureMaps(Class clazz) {
		main.ensureMaps(clazz);
	}

	synchronized void endCommit() {
		committing = false;
	}

	synchronized void startCommit() {
		for (PropertyStoreCacheWrapper psWrapper : psWrappers) {
			psWrapper.resetThreadCache();
		}
		committing = true;
	}

	class PropertyStoreCacheWrapper<V extends HasIdAndLocalId> implements
			MultiplexableCache {
		private PropertyStoreItemDescriptor descriptor;

		Map<Long, V> commitLookup = new LinkedHashMap<>();

		PropertyStoreCacheWrapper(PropertyStoreItemDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public <T> T get(Class<T> clazz, Long id) {
			if (!commitLookup.containsKey(id)) {
				commitLookup.put(id, descriptor.getProxy(main, id, false));
			}
			return (T) commitLookup.get(id);
		}

		@Override
		public void put(HasIdAndLocalId hili) {
			long id = hili.getId();
			commitLookup.put(id, descriptor.getProxy(main, id, true));
		}

		@Override
		public void remove(HasIdAndLocalId hili) {
			commitLookup.remove(hili.getId());
			descriptor.remove(hili.getId());
		}

		public void resetThreadCache() {
			commitLookup.clear();
		}

		Class getCachedClass() {
			return descriptor.clazz;
		}
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;

class PropertyStoreAwareMultiplexingObjectCache extends DetachedEntityCache {
	private volatile boolean committing;

	private DetachedEntityCacheAccess main = new DetachedEntityCacheAccess();

	private List<PropertyStoreCacheWrapper> psWrappers = new ArrayList<>();

	public PropertyStoreAwareMultiplexingObjectCache() {
		main.setThrowOnExisting(true);
		//
		// double-put can happen due to incomplete transaction isolation on PG
		// warmup
		//
		// let's try that again...
	}

	public void addPropertyStore(DomainClassDescriptor descriptor) {
		psWrappers.add(new PropertyStoreCacheWrapper(
				(PropertyStoreItemDescriptor) descriptor));
	}

	@Override
	public Set<Entity> allValues() {
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
	public <T extends Entity> boolean contains(Class<T> clazz, long id) {
		return main.contains(clazz, id);
	}

	@Override
	public boolean contains(Entity entity) {
		return main.contains(entity);
	}

	@Override
	public <T> List<T> fieldValues(Class<? extends Entity> clazz,
			String propertyName) {
		return getSubCache(clazz).fieldValues(clazz, propertyName);
	}

	@Override
	public <T> T get(Class<T> clazz, Long id) {
		return getSubCache(clazz).get(clazz, id);
	}

	@Override
	public Map<Long, Entity> getCreatedLocals() {
		return main.getCreatedLocals();
	}

	@Override
	public Map<Class, Map<Long, Entity>> getDomain() {
		return main.getDomain();
	}

	@Override
	public Map<Long, Entity> getMap(Class clazz) {
		return main.getMap(clazz);
	}

	@Override
	public boolean isEmpty(Class clazz) {
		return main.isEmpty(clazz);
	}

	@Override
	public Set<Long> keys(Class clazz) {
		return getSubCache(clazz).keys(clazz);
	}

	@Override
	public List<Long> notContained(Collection<Long> ids, Class clazz) {
		return main.notContained(ids, clazz);
	}

	@Override
	public void put(Entity entity) {
		getSubCache(entity.entityClass()).put(entity);
	}

	@Override
	public void putAll(Class clazz, Collection<? extends Entity> values) {
		main.putAll(clazz, values);
	}

	@Override
	public void putForSuperClass(Class clazz, Entity entity) {
		main.putForSuperClass(clazz, entity);
	}

	@Override
	public void remove(Entity entity) {
		getSubCache(entity.entityClass()).remove(entity);
	}

	@Override
	public void removeLocal(Entity entity) {
		main.removeLocal(entity);
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
	public <T> Stream<T> stream(Class<T> clazz) {
		return main.stream(clazz);
	}

	@Override
	public <T> Set<T> values(Class<T> clazz) {
		return main.values(clazz);
	}

	private <T> DomainStoreCache getSubCache(Class<T> clazz) {
		for (PropertyStoreCacheWrapper psWrapper : psWrappers) {
			if (psWrapper.getCachedClass() == clazz) {
				return psWrapper;
			}
		}
		return main;
	}

	@Override
	protected Map<Long, Entity> createIdEntityMap(Class clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ensureMap(Class clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected <T> T getLocal(Class<T> clazz, long localId) {
		return main.getLocal(clazz, localId);
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

	static class DetachedEntityCacheAccess extends DetachedEntityCache
			implements DomainStoreCache {
		@Override
		public void invalidate(Class clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		// linkedhashmap is fine here
		protected Map<Class, Map<Long, Entity>> createClientInstanceClassMap() {
			return super.createClientInstanceClassMap();
		}

		@Override
		protected Map<Long, Entity> createIdEntityMap(Class clazz) {
			TransactionalMap transactionalMap = new TransactionalMap(Long.class,
					clazz);
			transactionalMap.setImmutableValues(true);
			return transactionalMap;
		}

		@Override
		protected void createTopMaps() {
			domain = new LinkedHashMap<>();
			local = new LinkedHashMap<>();
			createdLocals = new ConcurrentHashMap<>();
		}

		@Override
		protected <T> T getLocal(Class<T> clazz, long localId) {
			return super.getLocal(clazz, localId);
		}
	}

	class PropertyStoreCacheWrapper<V extends Entity>
			implements DomainStoreCache {
		private PropertyStoreItemDescriptor<V> descriptor;

		Map<Long, V> commitLookup = new LinkedHashMap<>();

		PropertyStoreCacheWrapper(PropertyStoreItemDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public <T> List<T> fieldValues(Class<? extends Entity> clazz,
				String propertyName) {
			return descriptor.propertyStore.fieldValues(propertyName);
		}

		@Override
		public <T> T get(Class<T> clazz, Long id) {
			if (committing) {
				if (!commitLookup.containsKey(id)) {
					commitLookup.put(id, descriptor.getProxy(main, id, false));
				}
				return (T) commitLookup.get(id);
			} else {
				// does not cache, returns new instance each time.
				// FIXME - link to transaction?
				return (T) descriptor.getProxy(main, id, false);
			}
		}

		@Override
		public Set<Long> keys(Class clazz) {
			return descriptor.propertyStore.getIds();
		}

		@Override
		public void put(Entity entity) {
			if (committing) {
				long id = entity.getId();
				commitLookup.put(id, descriptor.getProxy(main, id, true));
			} else {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public void remove(Entity entity) {
			if (committing) {
				commitLookup.remove(entity.getId());
				descriptor.remove(entity.getId());
			} else {
				throw new UnsupportedOperationException();
			}
		}

		public void resetThreadCache() {
			commitLookup.clear();
		}

		Class getCachedClass() {
			return descriptor.clazz;
		}
	}
}

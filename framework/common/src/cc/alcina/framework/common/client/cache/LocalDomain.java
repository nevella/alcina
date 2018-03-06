package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.CommonUtils;

public class LocalDomain {
	private DetachedEntityCache cache = new DetachedEntityCache();

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	private CacheDescriptor cacheDescriptor;

	public <T> Collection<T> getCollectionFor(Class<T> clazz) {
		return cache.values(clazz);
	}

	public LocalDomain() {
	}
	
	public <T extends HasIdAndLocalId> T find(Class<T> clazz, long id){
		return cache.get(clazz, id);
	}

	public LocalDomain(CacheDescriptor cacheDescriptor) {
		this.cacheDescriptor = cacheDescriptor;
	}

	public void add(HasIdAndLocalId obj) {
		cache.put(obj);
		index(obj, true);
	}

	private void index(HasIdAndLocalId obj, boolean add) {
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		CacheItemDescriptor<?> itemDescriptor = cacheDescriptor.perClass
				.get(clazz);
		itemDescriptor.index(obj, add);
		for (HasIdAndLocalId dependentObject : itemDescriptor
				.getDependentObjectsWithDerivedProjections(obj)) {
			index(dependentObject, add);
		}
	}

	public void init() {
		for (CacheItemDescriptor<?> descriptor : cacheDescriptor.perClass
				.values()) {
			for (CacheLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
				lookupDescriptor.createLookup();
			}
		}
	}

	public static class LocalDomainQuery<T> {
		private LocalDomain localDomain;

		private Class<T> clazz;

		private Object alias;

		private Object key;

		public LocalDomainQuery(LocalDomain localDomain, Class<T> clazz,
				Object alias, Object key) {
			this.localDomain = localDomain;
			this.clazz = clazz;
			this.alias = alias;
			this.key = key;
		}

		public List<T> list() {
			CacheItemDescriptor itemDescriptor = localDomain.cacheDescriptor.perClass
					.get(clazz);
			Optional<CacheLookupDescriptor> lookupDescriptor = itemDescriptor
					.findDescriptorByAlias(alias);
			if (lookupDescriptor.isPresent()) {
				Set ids = lookupDescriptor.get().getLookup().get(key);
				if (ids == null || ids.isEmpty()) {
					return new ArrayList<>();
				} else {
					return localDomain.cache.list(clazz, ids);
				}
			} else {
				throw new IllegalArgumentException();
			}
		}

		public Optional<T> findFirst() {
			return Optional.ofNullable(CommonUtils.first(list()));
		}
	}

	public <T extends HasIdAndLocalId> LocalDomainQuery<T>
			aliasedQuery(Class<T> clazz, Object alias, Object key) {
		return new LocalDomainQuery(this, clazz, alias, key);
	}
}

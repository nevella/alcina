package cc.alcina.framework.common.client.domain;

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

	private DomainDescriptor domainDescriptor;

	public LocalDomain() {
	}

	public LocalDomain(DomainDescriptor domainDescriptor) {
		this.domainDescriptor = domainDescriptor;
	}

	public void add(HasIdAndLocalId obj) {
		cache.put(obj);
		index(obj, true);
	}

	public <T extends HasIdAndLocalId> LocalDomainQuery<T>
			aliasedQuery(Class<T> clazz, Object alias, Object key) {
		return new LocalDomainQuery(this, clazz, alias, key);
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, long id) {
		return cache.get(clazz, id);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public void init() {
		for (DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			for (DomainStoreLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
				lookupDescriptor.createLookup();
			}
		}
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}

	public <T> Collection<T> values(Class<T> clazz) {
		return cache.values(clazz);
	}

	private synchronized void index(HasIdAndLocalId obj, boolean add) {
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		itemDescriptor.index(obj, add);
		for (HasIdAndLocalId dependentObject : itemDescriptor
				.getDependentObjectsWithDerivedProjections(obj)) {
			index(dependentObject, add);
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

		public Optional<T> findFirst() {
			return Optional.ofNullable(CommonUtils.first(list()));
		}

		public List<T> list() {
			DomainClassDescriptor itemDescriptor = localDomain.domainDescriptor.perClass
					.get(clazz);
			Optional<DomainStoreLookupDescriptor> lookupDescriptor = itemDescriptor
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
	}
}

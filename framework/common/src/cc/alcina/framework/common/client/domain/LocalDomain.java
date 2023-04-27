package cc.alcina.framework.common.client.domain;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.LooseContext;

public class LocalDomain {
	private DetachedEntityCache cache = new DetachedEntityCache();

	private DomainDescriptor domainDescriptor;

	public LocalDomain() {
	}

	public LocalDomain(DomainDescriptor domainDescriptor) {
		this.domainDescriptor = domainDescriptor;
	}

	public synchronized void add(Entity obj) {
		cache.put(obj);
		index(obj, true);
	}

	public <T extends Entity> LocalDomainQuery<T> aliasedQuery(Class<T> clazz,
			Object alias, Object key) {
		return new LocalDomainQuery(this, clazz, alias, key);
	}

	public <T extends Entity> T find(Class<T> clazz, long id, long localId) {
		return cache.get(clazz, id, localId);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public void initialise() {
		try {
			LooseContext.pushWithTrue(
					IDomainStore.CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT);
			this.domainDescriptor.initialise();
			for (DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
					.values()) {
				for (DomainStoreLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
					lookupDescriptor.createLookup();
				}
			}
		} finally {
			LooseContext.pop();
		}
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}

	public <T> Stream<T> stream(Class<T> clazz) {
		return cache.stream(clazz);
	}

	private synchronized void index(Entity obj, boolean add) {
		Class<? extends Entity> clazz = obj.entityClass();
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		itemDescriptor.index(obj, add, true, null);
		itemDescriptor.getDependentObjectsWithDerivedProjections(obj,
				Collections.emptySet()).forEach(e -> index(e, add));
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
			return stream().findFirst();
		}

		public List<T> list() {
			return stream().collect(Collectors.toList());
		}

		public Stream<T> stream() {
			DomainClassDescriptor itemDescriptor = localDomain.domainDescriptor.perClass
					.get(clazz);
			Optional<DomainStoreLookupDescriptor> lookupDescriptor = itemDescriptor
					.findDescriptorByAlias(alias);
			if (lookupDescriptor.isPresent()) {
				Set<T> values = lookupDescriptor.get().getLookup().get(key);
				if (values == null || values.isEmpty()) {
					return Stream.empty();
				} else {
					return values.stream();
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	public static class Transactions {
		/*
		 * TODO - this will normally be called in a client environment
		 */
		public static void commit() {
		}
	}
}

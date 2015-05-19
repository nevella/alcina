package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.entityaccess.cache.CacheLookupDescriptor.IdCacheLookupDescriptor;

public class CacheItemDescriptor {
	public Class clazz;

	public List<CacheLookupDescriptor> lookupDescriptors = new ArrayList<CacheLookupDescriptor>();

	public List<CacheProjection> projections = new ArrayList<CacheProjection>();

	private StringMap propertyAlia = new StringMap();

	public boolean lazy = false;

	public void addPropertyAlias(String from, String to) {
		propertyAlia.put(from, to);
	}

	public CacheItemDescriptor(Class clazz) {
		this.clazz = clazz;
	}

	public CacheItemDescriptor(Class clazz, boolean idLookups,
			String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new IdCacheLookupDescriptor(clazz, propertyIndex));
		}
	}

	public CacheItemDescriptor(Class clazz, String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new CacheLookupDescriptor(clazz, propertyIndex));
		}
	}

	public CacheItemDescriptor addLookup(CacheLookupDescriptor lookup) {
		lookupDescriptors.add(lookup);
		return this;
	}

	public Set<Long> evaluateFilter(DetachedEntityCache cache,
			Set<Long> existing, CollectionFilter filter) {
		if (existing == null) {
			List filtered = CollectionFilters.filter(
					cache.immutableRawValues(clazz), filter);
			return HiliHelper.toIdSet(filtered);
		} else {
			CollectionFilter withIdFilter = new CollectionFilter<Long>() {
				@Override
				public boolean allow(Long id) {
					return filter.allow(cache.get(clazz, id));
				}
			};
			existing = new LinkedHashSet<Long>(existing);
			CollectionFilters.filterInPlace(existing, withIdFilter);
			return existing;
		}
	}

	public boolean ignoreField(String name) {
		return false;
	}

	public <T> List<T> getRawValues(Set<Long> ids, DetachedEntityCache cache) {
		ArrayList<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			T value = (T) cache.get(clazz, id);
			if (value != null) {
				raw.add(value);
			}
		}
		return raw;
	}

	public String getCanonicalPropertyPath(String propertyPath) {
		for (CacheLookupDescriptor desc : lookupDescriptors) {
			String path = desc.getCanonicalPropertyPath(propertyPath);
			if (path != null) {
				return path;
			}
		}
		if (propertyAlia.containsKey(propertyPath)) {
			return propertyAlia.get(propertyPath);
		}
		return propertyPath;
	}
}

package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor.IdLookupDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.StringMap;

public class DomainClassDescriptor<T extends HasIdAndLocalId> {
	public Class<T> clazz;

	public List<DomainStoreLookupDescriptor> lookupDescriptors = new ArrayList<DomainStoreLookupDescriptor>();

	public List<DomainProjection> projections = new ArrayList<DomainProjection>();

	private StringMap propertyAlia = new StringMap();
	
	private Map<Object,DomainStoreLookupDescriptor> aliasedFunctionLookups = new LinkedHashMap<>();

	public boolean lazy = false;

	public DomainClassDescriptor(Class<T> clazz) {
		this.clazz = clazz;
	}

	public DomainClassDescriptor(Class<T> clazz, boolean idLookups,
			String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new IdLookupDescriptor(clazz, propertyIndex));
		}
	}

	public DomainClassDescriptor(Class<T> clazz, String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new DomainStoreLookupDescriptor(clazz, propertyIndex));
		}
	}

	public DomainClassDescriptor<T> addLookup(DomainStoreLookupDescriptor lookup) {
		lookupDescriptors.add(lookup);
		return this;
	}

	public void addPropertyAlias(String from, String to) {
		propertyAlia.put(from, to);
	}

	public Set<Long> evaluateFilter(DetachedEntityCache cache,
			Set<Long> existing, CollectionFilter filter) {
		if (existing == null) {
			List filtered = CollectionFilters
					.filter(cache.immutableRawValues(clazz), filter);
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

	public String getCanonicalPropertyPath(String propertyPath) {
		for (DomainStoreLookupDescriptor desc : lookupDescriptors) {
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

	public Collection<HasIdAndLocalId>
			getDependentObjectsWithDerivedProjections(HasIdAndLocalId obj) {
		return new ArrayList<>();
	}

	public List<T> getRawValues(Set<Long> ids, DetachedEntityCache cache) {
		ArrayList<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			T value = (T) cache.get(clazz, id);
			if (value != null) {
				raw.add(value);
			}
		}
		return raw;
	}

	public boolean ignoreField(String name) {
		return false;
	}

	public void index(HasIdAndLocalId obj, boolean add) {
		for (DomainStoreLookupDescriptor lookupDescriptor : lookupDescriptors) {
			DomainLookup lookup = lookupDescriptor.getLookup();
			if (add) {
				lookup.insert(obj);
			} else {
				lookup.remove(obj);
			}
		}
		for (DomainProjection projection : projections) {
			if (add) {
				projection.insert(obj);
			} else {
				projection.remove(obj);
			}
		}
	}

	public boolean isTransactional() {
		return true;
	}

	public DomainClassDescriptor<T> addAliasedFunction(Object alias, Function<? super T,?> function) {
		DomainStoreLookupDescriptor lookupDescriptor = new DomainStoreLookupDescriptor<>((Class)clazz, "no-path",false,(Function)function);
		addLookup(lookupDescriptor);
		aliasedFunctionLookups.put(alias, lookupDescriptor);
		return this;
	}

	public Optional<DomainStoreLookupDescriptor> findDescriptorByAlias(Object alias) {
		return Optional.ofNullable(aliasedFunctionLookups.get(alias));
	}
}

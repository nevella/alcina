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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

/*
 * Call initialise once all infrastructure (mvcc) is ready 
 */
public class DomainClassDescriptor<T extends Entity>
		implements MemoryStatProvider {
	public Class<T> clazz;

	public List<DomainStoreLookupDescriptor> lookupDescriptors = new ArrayList<DomainStoreLookupDescriptor>();

	public List<DomainProjection> projections = new ArrayList<DomainProjection>();

	private StringMap propertyAlia = new StringMap();

	private Map<Object, DomainStoreLookupDescriptor> aliasedFunctionLookups = new LinkedHashMap<>();

	public boolean lazy = false;

	Logger logger = LoggerFactory.getLogger(getClass());

	private String[] propertyIndicies;

	public DomainClassDescriptor(Class<T> clazz) {
		this(clazz, new String[0]);
	}

	public DomainClassDescriptor(Class<T> clazz, String... propertyIndicies) {
		this.clazz = clazz;
		this.propertyIndicies = propertyIndicies;
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent, StatType type) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		lookupDescriptors
				.forEach(descriptor -> descriptor.addMemoryStats(self, type));
		projections
				.forEach(projection -> projection.addMemoryStats(self, type));
		return self;
	}

	public void addPropertyAlias(String from, String to) {
		propertyAlia.put(from, to);
	}

	public void addRawValues(Set<Long> ids, DetachedEntityCache cache,
			Set<T> rawValues) {
		for (Long id : ids) {
			T value = (T) cache.get(clazz, id);
			if (value != null) {
				rawValues.add(value);
			}
		}
	}

	public Set<Long> evaluateFilter(DetachedEntityCache cache,
			Set<Long> existing, CollectionFilter filter) {
		if (existing == null) {
			List filtered = CollectionFilters
					.filter(cache.immutableRawValues(clazz), filter);
			return EntityHelper.toIdSet(filtered);
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

	public Optional<DomainStoreLookupDescriptor>
			findDescriptorByAlias(Object alias) {
		return Optional.ofNullable(aliasedFunctionLookups.get(alias));
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

	public Collection<Entity>
			getDependentObjectsWithDerivedProjections(Entity obj) {
		return new ArrayList<>();
	}

	public boolean ignoreField(String name) {
		return false;
	}

	public void index(Entity obj, boolean add) {
		for (DomainStoreLookupDescriptor lookupDescriptor : lookupDescriptors) {
			DomainLookup lookup = lookupDescriptor.getLookup();
			if (add) {
				lookup.insert(obj);
			} else {
				lookup.remove(obj);
			}
		}
		for (DomainProjection projection : projections) {
			try {
				if (add) {
					projection.insert(obj);
				} else {
					projection.remove(obj);
				}
			} catch (Exception e) {
				String msg = Ax.format(
						"Issue indexing: projection: %s  - add: %s - object: %s",
						projection, add, obj);
				logger.warn(msg, e);
			}
		}
	}

	public void initialise() {
		for (String propertyIndex : propertyIndicies) {
			addLookup(new DomainStoreLookupDescriptor(clazz, propertyIndex));
		}
	}

	public boolean isTransactional() {
		return true;
	}

	@Override
	public String toString() {
		return Ax.format("DomainClassDescriptor: %s", clazz.getName());
	}

	protected DomainClassDescriptor<T> addAliasedFunction(Object alias,
			Function<? super T, ?> function) {
		DomainStoreLookupDescriptor lookupDescriptor = new DomainStoreLookupDescriptor<>(
				(Class) clazz, "no-path", (Function) function);
		addLookup(lookupDescriptor);
		aliasedFunctionLookups.put(alias, lookupDescriptor);
		return this;
	}

	protected DomainClassDescriptor<T>
			addLookup(DomainStoreLookupDescriptor lookup) {
		lookupDescriptors.add(lookup);
		return this;
	}
}

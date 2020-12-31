package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyResolver;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
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

	private DomainDescriptor domainDescriptor;

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
	public MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		lookupDescriptors
				.forEach(descriptor -> descriptor.addMemoryStats(self));
		projections.forEach(projection -> projection.addMemoryStats(self));
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

	public Stream<? extends Entity> getDependentObjectsWithDerivedProjections(
			Entity obj, Set<String> modifiedPropertyNames) {
		return Stream.empty();
	}

	public DomainDescriptor getDomainDescriptor() {
		return this.domainDescriptor;
	}

	public String getInitialLoadFilter() {
		return "";
	}

	public boolean ignoreField(String name) {
		return false;
	}

	public void index(Entity obj, boolean add, boolean committed,
			EntityCollation entityCollation) {
		for (DomainStoreLookupDescriptor lookupDescriptor : lookupDescriptors) {
			DomainLookup lookup = lookupDescriptor.getLookup();
			if (add) {
				lookup.insert(obj);
			} else {
				lookup.remove(obj);
			}
		}
		for (DomainProjection projection : projections) {
			if (!committed && projection.isCommitOnly()) {
				continue;
			}
			if (projection.isIgnoreForIndexing(entityCollation)) {
				continue;
			}
			try {
				if (add) {
					projection.insert(obj);
				} else {
					projection.remove(obj);
				}
			} catch (Exception e) {
				String msg = "<error evaluating issue>";
				try {
					msg = Ax.format(
							"Issue indexing: projection: %s  - add: %s - object: %s",
							projection, add, obj);
				} catch (Exception e2) {
				}
				logger.warn(msg, e);
			}
		}
	}

	public void initialise() {
		for (String propertyIndex : propertyIndicies) {
			addLookup(new DomainStoreLookupDescriptor(clazz, propertyIndex));
		}
	}

	public boolean isIgnoreColumn(String name) {
		return false;
	}

	public boolean isTransactional() {
		return true;
	}

	public boolean provideNotFullyLoadedOnStartup() {
		return lazy || getInitialLoadFilter().length() > 0;
	}

	public DomainStoreProperty
			resolveDomainStoreProperty(AnnotationLocation propertyLocation) {
		DomainStorePropertyResolver resolver = new DomainStorePropertyResolver(
				propertyLocation);
		DomainStorePropertyResolver parent = domainDescriptor
				.resolveDomainStoreProperty(resolver);
		return parent.resolver.hasValue() ? parent : null;
	}

	public void setDomainDescriptor(DomainDescriptor domainDescriptor) {
		this.domainDescriptor = domainDescriptor;
	}

	@Override
	public String toString() {
		return Ax.format("DomainClassDescriptor: %s", clazz.getName());
	}

	protected DomainClassDescriptor<T> addAliasedFunction(Object alias,
			Function<? super T, ?> function, Class lookupIndexClass) {
		DomainStoreLookupDescriptor lookupDescriptor = new DomainStoreLookupDescriptor<>(
				(Class) clazz, "no-path", (Function) function,
				lookupIndexClass);
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

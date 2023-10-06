package cc.alcina.framework.common.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.MultisetCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.PropertyPath;

/**
 * <p>
 * A mapping from a domain type to a projected value (possibly a collection),
 * indexed on the projected value.
 *
 * <p>
 * Warning! There is an edge case where distribution of values is not ideal for
 * high write loads, specifically for Class X, index on X.v.y0...y(n) where v is
 * a collection with many elements.
 *
 * <p>
 * This class will emit a warning if an inappropriate distribution is found -
 * generally that sort of distribution is better handled with a multi-layered
 * (DomainProjection) cache
 */
public class DomainLookup<T, E extends Entity>
		implements DomainListener<E>, IndexedValueProvider<E> {
	private static final int WARN_DISTRIBUTION = 100;

	private Multiset<T, Set<E>> store;

	protected DomainStoreLookupDescriptor descriptor;

	private PropertyPath propertyPath;

	private boolean enabled = true;

	private Predicate<E> relevanceFilter;

	private Converter<T, T> normaliser;

	private boolean distributionWarned = false;

	public DomainLookup(DomainStoreLookupDescriptor descriptor) {
		this.descriptor = descriptor;
		if (descriptor.propertyPath != null) {
			this.propertyPath = new PropertyPath(descriptor.propertyPath);
		}
		Class indexClass = CommonUtils.getWrapperType(
				descriptor.getLookupIndexClass(this.propertyPath));
		this.store = Registry.impl(MultisetCreator.class).create(indexClass,
				getListenedClass());
		this.relevanceFilter = descriptor.getRelevanceFilter();
		if (indexClass == Long.class) {
			Converter<Long, Long> normaliser = l -> l == null ? 0 : l;
			this.normaliser = (Converter<T, T>) normaliser;
		} else if (indexClass == Long.class) {
			Converter<Long, Long> normaliser = l -> l == null ? 0 : l;
			this.normaliser = (Converter<T, T>) normaliser;
		}
	}

	@Override
	public FilterCost estimateFilterCost(int entityCount,
			DomainFilter... filters) {
		return FilterCost.lookupProjectionCost();
	}

	public Set<E> get(T k1) {
		k1 = normalise(k1);
		return store.get(k1);
	}

	@Override
	public StreamOrSet<E> getKeyMayBeCollection(Object value) {
		if (value instanceof Collection) {
			Stream<Set> s1 = ((Collection) value).stream()
					.map(o -> get(normalise((T) o))).filter(Objects::nonNull);
			Stream<E> stream = s1.flatMap(Collection::stream);
			return new StreamOrSet<>(stream);
		} else {
			Set<E> set = get(normalise((T) value));
			return set == null ? null : new StreamOrSet<>(set);
		}
	}

	@Override
	public Class getListenedClass() {
		return descriptor.clazz;
	}

	public Converter<T, T> getNormaliser() {
		return this.normaliser;
	}

	public Set<E> getOrEmpty(T k1) {
		Set<E> set = get(k1);
		return set == null ? Collections.emptySet() : set;
	}

	public PropertyPath getPropertyPathAccesor() {
		return this.propertyPath;
	}

	@Override
	public void insert(E entity) {
		if (relevanceFilter != null && !relevanceFilter.test(entity)) {
			return;
		}
		Object v1 = getChainedProperty(entity);
		if (v1 instanceof Collection) {
			Set deduped = new LinkedHashSet((Collection) v1);
			if (deduped.size() > WARN_DISTRIBUTION && !distributionWarned) {
				distributionWarned = true;
				Ax.err("Distribution warning - consider a different index structure: %s",
						this);
			}
			for (Object v2 : deduped) {
				add(normalise((T) v2), entity);
			}
		} else {
			add(normalise((T) v1), entity);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isIgnoreForIndexing(EntityCollation entityCollation) {
		if (propertyPath != null && propertyPath.isSinglePathSegment()
				&& entityCollation.isPropertyOnly() && entityCollation
						.doesNotContainsNames(propertyPath.getPropertyPath())) {
			return true;
		} else {
			return false;
		}
	}

	public Set<T> keys() {
		return store.keySet();
	}

	public boolean matches(E h, Object[] keys) {
		if (keys.length != 1) {
			throw new IllegalArgumentException("Keys length must equal one");
		}
		return CommonUtils.equalsWithNullEquality(getChainedProperty(h),
				keys[0]);
	}

	@Override
	public void remove(E entity) {
		Object v1 = getChainedProperty(entity);
		if (v1 instanceof Collection) {
			Set deduped = new LinkedHashSet((Collection) v1);
			for (Object v2 : deduped) {
				remove(normalise((T) v2), entity);
			}
		} else {
			remove(normalise((T) v1), entity);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setNormaliser(Converter<T, T> normaliser) {
		this.normaliser = normaliser;
	}

	public int size() {
		return store.size();
	}

	public int size(T t) {
		Set<E> set = get(t);
		return set == null ? null : set.size();
	}

	public <E2 extends Entity> Stream<E2> stream(T key, Function<E, E2> map) {
		Set<E> set = get(key);
		return set == null ? Stream.empty() : set.stream().map(map);
	}

	@Override
	public String toString() {
		return Ax.format("Lookup: %s [%s]", getListenedClass().getSimpleName(),
				descriptor.propertyPath);
	}

	private T normalise(T key) {
		return normaliser == null ? key : normaliser.convert(key);
	}

	protected boolean add(T key, E value) {
		if (value == null) {
			System.err.println(
					"Invalid value (null) for cache lookup put - " + key);
			return false;
		}
		try {
			return getAndEnsure(key).add(value);
		} catch (RuntimeException e) {
			String message = FormatBuilder.keyValues("Issue type",
					"lookup insert", "lookup", this, "key", key, "value",
					value);
			throw new RuntimeException(message, e);
		}
	}

	protected Set<E> getAndEnsure(T key) {
		key = normalise(key);
		return store.getAndEnsure(key);
	}

	protected Object getChainedProperty(E entity) {
		if (descriptor.valueFunction != null) {
			return descriptor.valueFunction.apply(entity);
		}
		return propertyPath.getChainedProperty(entity);
	}

	protected boolean remove(T key, E value) {
		Set<E> set = get(key);
		if (set != null) {
			return set.remove(value);
		} else {
			return false;
		}
	}
}

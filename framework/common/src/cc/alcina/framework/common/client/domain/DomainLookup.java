package cc.alcina.framework.common.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.MultisetCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;

public class DomainLookup<T, E extends Entity>
		implements DomainListener<E>, IndexedValueProvider<E> {
	private Multiset<T, Set<E>> store;

	protected DomainStoreLookupDescriptor descriptor;

	private PropertyPathAccessor propertyPathAccesor;

	private boolean enabled = true;

	private CollectionFilter<E> relevanceFilter;

	private Converter<T, T> normaliser;

	public DomainLookup(DomainStoreLookupDescriptor descriptor) {
		this.descriptor = descriptor;
		this.propertyPathAccesor = new PropertyPathAccessor(
				descriptor.propertyPath);
		Class indexClass = CommonUtils.getWrapperType(
				descriptor.getLookupIndexClass(this.propertyPathAccesor));
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
	public Set<E> getKeyMayBeCollection(Object value) {
		if (value instanceof Collection) {
			Set<E> result = new LiSet<>();
			for (T t : (Collection<T>) value) {
				Set<E> values = get(normalise(t));
				if (values != null) {
					result.addAll(values);
				}
			}
			return result;
		} else {
			return get(normalise((T) value));
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

	public PropertyPathAccessor getPropertyPathAccesor() {
		return this.propertyPathAccesor;
	}

	@Override
	public void insert(E entity) {
		if (relevanceFilter != null && !relevanceFilter.allow(entity)) {
			return;
		}
		Object v1 = getChainedProperty(entity);
		if (v1 instanceof Collection) {
			Set deduped = new LinkedHashSet((Collection) v1);
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

	private void remove(T key, E value) {
		Set<E> set = get(key);
		if (set != null) {
			set.remove(value);
		}
	}

	protected void add(T key, E value) {
		if (value == null) {
			System.err.println(
					"Invalid value (null) for cache lookup put - " + key);
			return;
		}
		getAndEnsure(key).add(value);
	}

	protected Set<E> getAndEnsure(T key) {
		key = normalise(key);
		return store.getAndEnsure(key);
	}

	protected Object getChainedProperty(E entity) {
		if (descriptor.valueFunction != null) {
			return descriptor.valueFunction.apply(entity);
		}
		return propertyPathAccesor.getChainedProperty(entity);
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccesor;
import cc.alcina.framework.entity.util.Multiset;

import com.totsp.gwittir.client.beans.Converter;

public class CacheLookup<T, H extends HasIdAndLocalId> implements
		CacheListener<H> {
	private Multiset<T, Set<Long>> store;

	protected CacheLookupDescriptor descriptor;

	private PropertyPathAccesor propertyPathAccesor;

	protected DetachedEntityCache privateCache;

	private boolean enabled = true;

	private CollectionFilter<H> relevanceFilter;

	private Converter<T, T> normaliser;

	public CacheLookup(CacheLookupDescriptor descriptor) {
		this.descriptor = descriptor;
		store = new Multiset<T, Set<Long>>();
		this.propertyPathAccesor = new PropertyPathAccesor(
				descriptor.propertyPath);
		this.relevanceFilter = descriptor.getRelevanceFilter();
	}

	public void add(T k1, Long value) {
		getAndEnsure(k1).add(value);
	}

	public Set<Long> get(T k1) {
		return store.get(normalise(k1));
	}

	private T normalise(T k1) {
		return normaliser == null ? k1 : normaliser.convert(k1);
	}

	public Set<Long> getAndEnsure(T k1) {
		Set<Long> result = get(k1);
		if (result == null) {
			result = new LinkedHashSet<Long>();
			store.put(normalise(k1), result);
		}
		return result;
	}

	public Set<H> getPrivateObjects(T k1) {
		Set<H> result = new LinkedHashSet<H>();
		Set<Long> ids = getAndEnsure(k1);
		for (Long id : ids) {
			result.add(getForResolvedId(id));
		}
		return result;
	}

	@Override
	public Class getListenedClass() {
		return descriptor.clazz;
	}

	public Set<Long> getMaybeCollectionKey(Object value) {
		if (value instanceof Collection) {
			Set<Long> result = new LinkedHashSet<Long>();
			for (T t : (Collection<T>) value) {
				Set<Long> ids = get(t);
				if (ids != null) {
					result.addAll(ids);
				}
			}
			return result;
		} else {
			return get((T) value);
		}
	}

	@Override
	public void insert(H hili) {
		if (relevanceFilter != null && !relevanceFilter.allow(hili)) {
			return;
		}
		Object v1 = getChainedProperty(hili);
		if (v1 instanceof Collection) {
			Set deduped = new LinkedHashSet((Collection) v1);
			for (Object v2 : deduped) {
				add((T) v2, hili.getId());
			}
		} else {
			add((T) v1, hili.getId());
		}
		if (privateCache != null) {
			privateCache.put(hili);
		}
	}

	protected Object getChainedProperty(H hili) {
		return propertyPathAccesor.getChainedProperty(hili);
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public Set<T> keys() {
		return store.keySet();
	}

	@Override
	public void remove(H hili) {
		Object v1 = getChainedProperty(hili);
		remove((T) v1, hili.getId());
	}

	public void remove(T k1, Long value) {
		getAndEnsure(k1).remove(value);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int size() {
		return store.size();
	}

	public int size(T t) {
		return getAndEnsure(t).size();
	}

	@Override
	public boolean matches(H h, Object[] keys) {
		if (keys.length != 1) {
			throw new IllegalArgumentException("Keys length must equal one");
		}
		return CommonUtils.equalsWithNullEquality(getChainedProperty(h),
				keys[0]);
	}

	protected H getForResolvedId(long id) {
		if (privateCache != null) {
			return (H) privateCache.get(descriptor.clazz, id);
		}
		return (H) AlcinaMemCache.get().transactional
				.find(descriptor.clazz, id);
	}

	public Converter<T, T> getNormaliser() {
		return this.normaliser;
	}

	public void setNormaliser(Converter<T, T> normaliser) {
		this.normaliser = normaliser;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup: %s [%s]", getListenedClass()
				.getSimpleName(), descriptor.propertyPath);
	}
}

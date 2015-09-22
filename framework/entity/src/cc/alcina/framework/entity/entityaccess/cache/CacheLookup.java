package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.common.client.util.SortedMultiset;

import com.totsp.gwittir.client.beans.Converter;

public class CacheLookup<T, H extends HasIdAndLocalId> implements
		CacheListener<H> {
	private SortedMultiset<T, Set<Long>> store;

	protected CacheLookupDescriptor descriptor;

	private PropertyPathAccessor propertyPathAccesor;

	protected DetachedEntityCache privateCache;

	private boolean enabled = true;

	private CollectionFilter<H> relevanceFilter;

	private Converter<T, T> normaliser;

	protected boolean concurrent;

	public CacheLookup(CacheLookupDescriptor descriptor) {
		this(descriptor, false);
	}

	public CacheLookup(CacheLookupDescriptor descriptor, boolean concurrent) {
		this.descriptor = descriptor;
		this.concurrent = concurrent;
		if (concurrent) {
			store = new ConcurrentSortedMultiset<>();
		} else {
			store = new SortedMultiset<T, Set<Long>>() {
				@Override
				protected Set createSet() {
					return createLongSet();
				}

				@Override
				protected Map<T, Set<Long>> createTopMap() {
					return new Object2ObjectLinkedOpenHashMap<T, Set<Long>>();
				}
			};
		}
		this.propertyPathAccesor = new PropertyPathAccessor(
				descriptor.propertyPath);
		this.relevanceFilter = descriptor.getRelevanceFilter();
	}

	public void add(T k1, Long value) {
		if (value == null) {
			System.err.println("Invalid value (null) for cache lookup put - "
					+ k1);
			return;
		}
		getAndEnsure(k1).add(value);
	}

	public Set<Long> get(T k1) {
		return store.get(normalise(k1));
	}

	public Set<Long> getAndEnsure(T k1) {
		if (concurrent && k1 == null) {
			return new LinkedHashSet<>();
		}
		Set<Long> result = get(k1);
		if (result == null) {
			result = createLongSet();
			store.put(normalise(k1), result);
		}
		return result;
	}

	@Override
	public Class getListenedClass() {
		return descriptor.clazz;
	}

	public Set<Long> getMaybeCollectionKey(Object value, Set<Long> existing) {
		if (value instanceof Collection) {
			Set<Long> result = createLongSet();
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

	public Converter<T, T> getNormaliser() {
		return this.normaliser;
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
			if (descriptor.clazz != hili.getClass()) {
				privateCache.putForSuperClass(descriptor.clazz, hili);
			} else {
				privateCache.put(hili);
			}
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public Set<T> keys() {
		return store.keySet();
	}

	@Override
	public boolean matches(H h, Object[] keys) {
		if (keys.length != 1) {
			throw new IllegalArgumentException("Keys length must equal one");
		}
		return CommonUtils.equalsWithNullEquality(getChainedProperty(h),
				keys[0]);
	}

	@Override
	public void remove(H hili) {
		Object v1 = getChainedProperty(hili);
		remove((T) v1, hili.getId());
	}

	public void remove(T k1, Long value) {
		getAndEnsure(k1).remove(value);
	}

	public void removeExisting(H hili) {
		H existing = privateCache.getExisting(hili);
		if (existing != null) {
			remove(existing);
		}
	}

	public void removeExisting(HiliLocator locator) {
		H existing = (H) privateCache.get(locator.clazz, locator.id);
		if (existing != null) {
			remove(existing);
		}
	}

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
		return getAndEnsure(t).size();
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup: %s [%s]", getListenedClass()
				.getSimpleName(), descriptor.propertyPath);
	}

	private T normalise(T k1) {
		return normaliser == null ? k1 : normaliser.convert(k1);
	}

	protected Set createLongSet() {
		return new LongAVLTreeSet();
	}

	protected Object getChainedProperty(H hili) {
		return propertyPathAccesor.getChainedProperty(hili);
	}

	protected H getForResolvedId(long id) {
		if (privateCache != null) {
			return (H) privateCache.get(descriptor.clazz, id);
		}
		return (H) AlcinaMemCache.get().transactional
				.find(descriptor.clazz, id);
	}
}

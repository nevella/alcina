package cc.alcina.framework.common.client.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.cache.CacheCreators.CacheLongSetCreator;
import cc.alcina.framework.common.client.cache.CacheCreators.CacheMultisetCreator;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.common.client.util.SortedMultiset;

import com.google.gwt.core.client.GWT;
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

	private ModificationChecker modificationChecker;

	public CacheLookup(CacheLookupDescriptor descriptor) {
		this(descriptor, false);
	}

	public CacheLookup(CacheLookupDescriptor descriptor, boolean concurrent) {
		this.descriptor = descriptor;
		this.concurrent = concurrent;
		this.store = Registry.impl(CacheMultisetCreator.class).get(concurrent);
		this.propertyPathAccesor = new PropertyPathAccessor(
				descriptor.propertyPath);
		this.relevanceFilter = descriptor.getRelevanceFilter();
	}

	public Set<Long> get(T k1) {
		return wrapWithModificationChecker(store.get(normalise(k1)));
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
		Set<Long> ids = get(k1);
		if (ids != null) {
			for (Long id : ids) {
				result.add(getForResolvedId(id));
			}
		}
		return result;
	}

	@Override
	public void insert(H hili) {
		if (relevanceFilter != null && !relevanceFilter.allow(hili)) {
			return;
		}
		checkModification("insert");
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
		return wrapWithModificationChecker(store.keySet());
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
		checkModification("remove");
		Set<Long> set = get(k1);
		if (set != null) {
			set.remove(value);
		}
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
		Set<Long> set = get(t);
		return set == null ? null : set.size();
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup: %s [%s]", getListenedClass()
				.getSimpleName(), descriptor.propertyPath);
	}

	private T normalise(T k1) {
		return normaliser == null ? k1 : normaliser.convert(k1);
	}

	private <V> Set<V> wrapWithModificationChecker(Set<V> set) {
		return set == null ? null
				: set instanceof CacheLookup.ModificationCheckedSet
						|| GWT.isClient() ? set : new ModificationCheckedSet(
						set);
	}

	protected void add(T k1, Long value) {
		if (value == null) {
			System.err.println("Invalid value (null) for cache lookup put - "
					+ k1);
			return;
		}
		getAndEnsure(k1).add(value);
	}

	protected void checkModification(String modificationType) {
		if (getModificationChecker() != null) {
			getModificationChecker().check("fire");
		}
	}

	protected Set createLongSet() {
		return Registry.impl(CacheLongSetCreator.class).get();
	}

	// write thread only!!
	protected Set<Long> getAndEnsure(T k1) {
		if (concurrent && k1 == null) {
			return new LinkedHashSet<>();
		}
		Set<Long> result = get(k1);
		if (result == null) {
			result = createLongSet();
			store.put(normalise(k1), result);
		}
		return wrapWithModificationChecker(result);
	}

	protected Object getChainedProperty(H hili) {
		return propertyPathAccesor.getChainedProperty(hili);
	}

	protected H getForResolvedId(long id) {
		if (privateCache != null) {
			return (H) privateCache.get(descriptor.clazz, id);
		}
		return (H) Domain.transactionalFind
				(descriptor.clazz, id);
	}

	public ModificationChecker getModificationChecker() {
		return modificationChecker;
	}

	public void setModificationChecker(ModificationChecker modificationChecker) {
		this.modificationChecker = modificationChecker;
	}

	class ModificationCheckedIterator implements Iterator {
		private Iterator iterator;

		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		public Object next() {
			return this.iterator.next();
		}

		public void remove() {
			checkModification("itr-remove");
			this.iterator.remove();
		}

		ModificationCheckedIterator(Iterator iterator) {
			if (iterator == null) {
				throw new RuntimeException("null");
			}
			this.iterator = iterator;
		}
	}

	class ModificationCheckedSet implements Set {
		private Set set;

		ModificationCheckedSet(Set set) {
			if (set == null) {
				throw new RuntimeException("null");
			}
			this.set = set;
		}

		public boolean add(Object e) {
			checkModification("add-set");
			return this.set.add(e);
		}

		public boolean addAll(Collection c) {
			checkModification("add-set");
			return this.set.addAll(c);
		}

		public void clear() {
			checkModification("clear");
			this.set.clear();
		}

		public boolean contains(Object o) {
			return this.set.contains(o);
		}

		public boolean containsAll(Collection c) {
			return this.set.containsAll(c);
		}

		public boolean isEmpty() {
			return this.set.isEmpty();
		}

		public Iterator iterator() {
			return this.set.iterator();
		}

		public boolean remove(Object o) {
			checkModification("remove-set");
			return this.set.remove(o);
		}

		public boolean removeAll(Collection c) {
			checkModification("remove-all");
			return this.set.removeAll(c);
		}

		public boolean retainAll(Collection c) {
			checkModification("retain-all");
			return this.set.retainAll(c);
		}

		public int size() {
			return this.set.size();
		}

		public Object[] toArray() {
			return this.set.toArray();
		}

		public Object[] toArray(Object[] a) {
			return this.set.toArray(a);
		}
	}
}

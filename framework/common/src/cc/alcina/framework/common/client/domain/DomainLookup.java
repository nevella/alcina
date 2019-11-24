package cc.alcina.framework.common.client.domain;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreLongSetCreator;
import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreMultisetCreator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;

public class DomainLookup<T, H extends HasIdAndLocalId>
		implements DomainListener<H> {
	private Multiset<T, Set<Long>> store;

	protected DomainStoreLookupDescriptor descriptor;

	private PropertyPathAccessor propertyPathAccesor;

	private boolean enabled = true;

	private CollectionFilter<H> relevanceFilter;

	private Converter<T, T> normaliser;

	private ModificationChecker modificationChecker;

	private DomainStoreLongSetCreator setCreator;

	public DomainLookup(DomainStoreLookupDescriptor descriptor) {
		this.descriptor = descriptor;
		this.propertyPathAccesor = new PropertyPathAccessor(
				descriptor.propertyPath);
		this.store = Registry.impl(DomainStoreMultisetCreator.class).get(this);
		this.relevanceFilter = descriptor.getRelevanceFilter();
		setCreator = Registry.impl(DomainStoreLongSetCreator.class);
	}

	public Set<Long> get(T k1) {
		k1 = normalise(k1);
		return store.get(k1);
	}

	@Override
	public IDomainStore getDomainStore() {
		return descriptor.getDomainStore();
	}

	@Override
	public Class getListenedClass() {
		return descriptor.clazz;
	}

	public Set<Long> getMaybeCollectionKey(Object value, Set<Long> existing) {
		if (value instanceof Collection) {
			Set<Long> result = createLongSet();
			for (T t : (Collection<T>) value) {
				Set<Long> ids = get(normalise(t));
				if (ids != null) {
					result.addAll(ids);
				}
			}
			return result;
		} else {
			return get(normalise((T) value));
		}
	}

	public ModificationChecker getModificationChecker() {
		return modificationChecker;
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

	public PropertyPathAccessor getPropertyPathAccesor() {
		return this.propertyPathAccesor;
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
				add(normalise((T) v2), hili.getId());
			}
		} else {
			add(normalise((T) v1), hili.getId());
		}
	}

	@Override
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
		if (v1 instanceof Collection) {
			Set deduped = new LinkedHashSet((Collection) v1);
			for (Object v2 : deduped) {
				remove(normalise((T) v2), hili.getId());
			}
		} else {
			remove(normalise((T) v1), hili.getId());
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void
			setModificationChecker(ModificationChecker modificationChecker) {
		this.modificationChecker = modificationChecker;
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
		return CommonUtils.formatJ("Lookup: %s [%s]",
				getListenedClass().getSimpleName(), descriptor.propertyPath);
	}

	private T normalise(T k1) {
		return normaliser == null || k1 == null ? k1 : normaliser.convert(k1);
	}

	private void remove(T k1, Long value) {
		checkModification("remove");
		Set<Long> set = get(k1);
		if (set != null) {
			set.remove(value);
		}
	}

	protected void add(T k1, Long value) {
		if (value == null) {
			System.err.println(
					"Invalid value (null) for cache lookup put - " + k1);
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
		return setCreator.get();
	}

	// write thread only!!
	protected Set<Long> getAndEnsure(T k1) {
		k1 = normalise(k1);
		Set<Long> result = get(k1);
		if (result == null) {
			result = createLongSet();
			store.put(k1, result);
		}
		return result;
	}

	protected Object getChainedProperty(H hili) {
		if (descriptor.valueFunction != null) {
			return descriptor.valueFunction.apply(hili);
		}
		return propertyPathAccesor.getChainedProperty(hili);
	}

	protected H getForResolvedId(long id) {
		return (H) Domain.transactionalFind(descriptor.clazz, id);
	}

	class ModificationCheckedIterator implements Iterator {
		private Iterator iterator;

		ModificationCheckedIterator(Iterator iterator) {
			if (iterator == null) {
				throw new RuntimeException("null");
			}
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Object next() {
			return this.iterator.next();
		}

		@Override
		public void remove() {
			checkModification("itr-remove");
			this.iterator.remove();
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

		@Override
		public boolean add(Object e) {
			checkModification("add-set");
			return this.set.add(e);
		}

		@Override
		public boolean addAll(Collection c) {
			checkModification("add-set");
			return this.set.addAll(c);
		}

		@Override
		public void clear() {
			checkModification("clear");
			this.set.clear();
		}

		@Override
		public boolean contains(Object o) {
			return this.set.contains(o);
		}

		@Override
		public boolean containsAll(Collection c) {
			return this.set.containsAll(c);
		}

		@Override
		public boolean isEmpty() {
			return this.set.isEmpty();
		}

		@Override
		public Iterator iterator() {
			return this.set.iterator();
		}

		@Override
		public boolean remove(Object o) {
			checkModification("remove-set");
			return this.set.remove(o);
		}

		@Override
		public boolean removeAll(Collection c) {
			checkModification("remove-all");
			return this.set.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection c) {
			checkModification("retain-all");
			return this.set.retainAll(c);
		}

		@Override
		public int size() {
			return this.set.size();
		}

		@Override
		public Object[] toArray() {
			return this.set.toArray();
		}

		@Override
		public Object[] toArray(Object[] a) {
			return this.set.toArray(a);
		}
	}
}

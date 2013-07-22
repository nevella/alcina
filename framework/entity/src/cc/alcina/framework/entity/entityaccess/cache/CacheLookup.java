package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.PropertyPathAccesor;
import cc.alcina.framework.entity.util.Multiset;

public class CacheLookup<T> implements CacheListener {
	private Multiset<T, Set<Long>> store;

	private CacheLookupDescriptor descriptor;

	private PropertyPathAccesor propertyPathAccesor;

	@Override
	public Class getListenedClass() {
		return descriptor.clazz;
	}

	public CacheLookup(CacheLookupDescriptor descriptor) {
		this.descriptor = descriptor;
		store = new Multiset<T, Set<Long>>();
		this.propertyPathAccesor = new PropertyPathAccesor(
				descriptor.propertyPath);
	}

	public Set<Long> get(T k1) {
		return store.get(k1);
	}

	public Set<T> keys() {
		return store.keySet();
	}

	public Set<Long> getAndEnsure(T k1) {
		Set<Long> result = get(k1);
		if (result == null) {
			result = new LinkedHashSet<Long>();
			store.put(k1, result);
		}
		return result;
	}

	public void add(T k1, Long value) {
		getAndEnsure(k1).add(value);
	}

	public void remove(T k1, Long value) {
		getAndEnsure(k1).remove(value);
	}

	@Override
	public void insert(HasIdAndLocalId hili) {
		Object v1 = propertyPathAccesor.getChainedProperty(hili);
		add((T) v1, hili.getId());
	}

	@Override
	public void remove(HasIdAndLocalId hili) {
		Object v1 = propertyPathAccesor.getChainedProperty(hili);
		remove((T) v1, hili.getId());
	}

	public int size(T t) {
		return getAndEnsure(t).size();
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
}

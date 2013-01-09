package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;

public class CachingPropertyLookup<K, V> {
	private Map<K, V> propertyLookup = new LinkedHashMap<K, V>();

	private final String key;

	public CachingPropertyLookup(String key) {
		this.key = key;
	}

	public void setCollection(Collection<V> values) {
		for (V v : values) {
			propertyLookup.put((K) CommonLocator.get().propertyAccessor()
					.getPropertyValue(v, key), v);
		}
	}

	public V get(K k) {
		return propertyLookup.get(k);
	}
}
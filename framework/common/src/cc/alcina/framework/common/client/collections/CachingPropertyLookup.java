package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;

public class CachingPropertyLookup<K, V> {
	private Map<K, V> propertyLookup = new LinkedHashMap<K, V>();

	private final String key;

	private PropertyAccessor propertyAccessor = Reflections.propertyAccessor();

	public CachingPropertyLookup(String key) {
		this.key = key;
	}

	public V get(K k) {
		return propertyLookup.get(k);
	}

	public void setCollection(Collection<V> values) {
		for (V v : values) {
			propertyLookup.put((K) propertyAccessor.getPropertyValue(v, key),
					v);
		}
	}
}
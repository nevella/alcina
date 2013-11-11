package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.Reflections;

public class PropertyKeyValueMapper<V> implements
		KeyValueMapper<Object, V, V> {
	private final String propertyName;

	public PropertyKeyValueMapper(String propertyName) {
		this.propertyName = propertyName;
	}

	public Object getKey(V o) {
		return Reflections.propertyAccessor().getPropertyValue(o,
				propertyName);
	};

	public V getValue(V o) {
		return o;
	};
}
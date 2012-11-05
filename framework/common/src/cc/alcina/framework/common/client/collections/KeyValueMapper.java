package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface KeyValueMapper<K, V, O> {
	public K getKey(O o);

	public V getValue(O o);

	public abstract class StringKeyValueMapper<V> implements
			KeyValueMapper<String, V, V> {
		public V getValue(V o) {
			return o;
		};
	}

	public static class PropertyKeyValueMapper<V> implements
			KeyValueMapper<String, V, V> {
		private final String propertyName;

		public PropertyKeyValueMapper(String propertyName) {
			this.propertyName = propertyName;
		}

		public String getKey(V o) {
			return (String) CommonLocator.get().propertyAccessor()
					.getPropertyValue(o, propertyName);
		};

		public V getValue(V o) {
			return o;
		};
	}
}

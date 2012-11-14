package cc.alcina.framework.common.client.collections;

import cc.alcina.framework.common.client.CommonLocator;

import com.totsp.gwittir.client.beans.Converter;

public interface KeyValueMapper<K, V, O> {
	public K getKey(O o);

	public V getValue(O o);

	public abstract class StringKeyValueMapper<V> implements
			KeyValueMapper<String, V, V> {
		public V getValue(V o) {
			return o;
		};
	}
	
	public static class PropertyConverter<T> implements Converter<Object, T> {
		private final String key;

		public PropertyConverter(String key) {
			this.key = key;
		}

		@Override
		public T convert(Object o) {
			return (T) CommonLocator.get().propertyAccessor()
					.getPropertyValue(o, key);
		};
	}
	public static class StringPropertyConverter extends PropertyConverter<String>{

		public StringPropertyConverter(String key) {
			super(key);
		}
		
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

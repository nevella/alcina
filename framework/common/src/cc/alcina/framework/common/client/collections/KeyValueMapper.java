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

	public abstract class FromObjectKeyValueMapper<K, V> implements
			KeyValueMapper<K, V, V> {
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

	public static class StringPropertyConverter extends
			PropertyConverter<String> {
		public StringPropertyConverter(String key) {
			super(key);
		}
	}

	public static class PropertyKeyValueMapper<V> implements
			KeyValueMapper<Object, V, V> {
		private final String propertyName;

		public PropertyKeyValueMapper(String propertyName) {
			this.propertyName = propertyName;
		}

		public Object getKey(V o) {
			return CommonLocator.get().propertyAccessor()
					.getPropertyValue(o, propertyName);
		};

		public V getValue(V o) {
			return o;
		};
	}

	public static class ConverterMapper<K, V> extends
			FromObjectKeyValueMapper<K, V> {
		private Converter<? super V, K> converter;

		public ConverterMapper(Converter<? super V, K> converter) {
			this.converter = converter;
		}

		@Override
		public K getKey(V o) {
			return converter.convert(o);
		}
	}

	public static class SelfMapper<V> implements KeyValueMapper<V, V, V> {
		@Override
		public V getKey(V o) {
			return o;
		}

		@Override
		public V getValue(V o) {
			return o;
		}
	}

	public static class LongArray2Mapper implements
			KeyValueMapper<Long, Long, Object[]> {
		@Override
		public Long getKey(Object[] o) {
			return (Long) o[0];
		}

		@Override
		public Long getValue(Object[] o) {
			return (Long) o[1];
		}
	}
}

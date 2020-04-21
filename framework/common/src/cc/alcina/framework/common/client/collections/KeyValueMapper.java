package cc.alcina.framework.common.client.collections;

import com.totsp.gwittir.client.beans.Converter;

//FIXME - this is outmoded by java 8 / streams / lambdas
public interface KeyValueMapper<K, V, O> {
	public K getKey(O o);

	public V getValue(O o);

	public static class FromObjectConverterMapper<K, V>
			extends FromObjectKeyValueMapper<K, V> {
		private Converter<? super V, K> converter;

		public FromObjectConverterMapper(Converter<? super V, K> converter) {
			this.converter = converter;
		}

		@Override
		public K getKey(V o) {
			return converter.convert(o);
		}
	}

	public static class LongArray2Mapper
			implements KeyValueMapper<Long, Long, Object[]> {
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

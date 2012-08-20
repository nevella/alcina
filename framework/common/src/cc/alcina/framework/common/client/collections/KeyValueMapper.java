package cc.alcina.framework.common.client.collections;

public interface KeyValueMapper<K, V, O> {
	public K getKey(O o);

	public V getValue(O o);

	public abstract class StringKeyValueMapper<V> implements
			KeyValueMapper<String, V, V> {
		public V getValue(V o) {
			return o;
		};
	}
}

package cc.alcina.framework.common.client.collections;

public abstract class FromObjectKeyValueMapper<K, V> implements
		KeyValueMapper<K, V, V> {
	public V getValue(V o) {
		return o;
	};
}
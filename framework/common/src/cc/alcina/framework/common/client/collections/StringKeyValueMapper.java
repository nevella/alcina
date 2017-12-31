package cc.alcina.framework.common.client.collections;

public abstract class StringKeyValueMapper<V>
		implements KeyValueMapper<String, V, V> {
	public V getValue(V o) {
		return o;
	};
}
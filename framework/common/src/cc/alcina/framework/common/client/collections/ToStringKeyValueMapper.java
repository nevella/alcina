package cc.alcina.framework.common.client.collections;

public class ToStringKeyValueMapper<V> extends StringKeyValueMapper<V> {
	@Override
	public String getKey(V o) {
		return o.toString();
	}
}

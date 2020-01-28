package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class NullWrappingMap<K, V> implements Map<K, V> {
	private Map<K, NullWrapper<V>> delegate;

	public NullWrappingMap(Map<K, NullWrapper<V>> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	@Override
	public V get(Object key) {
		NullWrapper<V> wrapper = delegate.get(key);
		return wrapper == null ? null : wrapper.value;
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return this.delegate.keySet();
	}

	@Override
	public V put(K key, V value) {
		delegate.put(key, new NullWrapper<V>(value));
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		delegate.remove(key);
		return null;
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	private static class NullWrapper<V> {
		private V value;

		public NullWrapper(V value) {
			this.value = value;
		}
	}
}

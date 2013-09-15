package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ImmutableMap<K, V> implements Map<K, V> {
	private Map<K, V> delegate;

	public ImmutableMap(Map<K, V> delegate) {
		this.delegate = delegate;
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.delegate.containsValue(value);
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new ImmutableSet<Map.Entry<K,V>>(delegate.entrySet());
	}

	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	public V get(Object key) {
		return this.delegate.get(key);
	}

	public int hashCode() {
		return this.delegate.hashCode();
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	public Set<K> keySet() {
		return new ImmutableSet<K>(this.delegate.keySet());
	}

	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return this.delegate.size();
	}

	public Collection<V> values() {
		return this.delegate.values();
	}

}

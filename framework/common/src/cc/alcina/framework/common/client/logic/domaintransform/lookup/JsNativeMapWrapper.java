package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class JsNativeMapWrapper<K, V> extends AbstractMap<K, V> {
	private JsNativeMap<K, V> map;

	private boolean weak;

	JsNativeMapWrapper(boolean weak) {
		this.weak = weak;
		map = JsNativeMap.createJsNativeMap(weak);
	}

	@Override
	public void clear() {
		this.map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values().stream().anyMatch(v -> Objects.equals(v, value));
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (weak) {
			throw new UnsupportedOperationException();
		}
		return new EntrySet();
	}

	@Override
	public V get(Object key) {
		return this.map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	@Override
	public V put(K key, V value) {
		return this.map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.forEach((key, value) -> this.map.put(key, value));
	}

	@Override
	public V remove(Object key) {
		return this.map.remove(key);
	}

	@Override
	public int size() {
		return this.map.size();
	}

	class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public void clear() {
			JsNativeMapWrapper.this.clear();
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new TypedEntryIterator();
		}

		@Override
		public int size() {
			return JsNativeMapWrapper.this.size();
		}
	}

	class KeyIterator implements Iterator {
		JavascriptJavaObjectArray keysSnapshot;

		int idx = -1;

		int itrModCount;

		boolean nextCalled = false;

		Object key;

		public KeyIterator() {
			keysSnapshot = map.keys();
			this.itrModCount = modCount();
		}

		@Override
		public boolean hasNext() {
			return idx + 1 < keysSnapshot.length();
		}

		private int modCount() {
			// FIXME - implement (and check in removal as well)
			return 0;
		}

		@Override
		public Object next() {
			if (idx + 1 == keysSnapshot.length()) {
				throw new NoSuchElementException();
			}
			if (itrModCount != modCount()) {
				throw new ConcurrentModificationException();
			}
			nextCalled = true;
			key = keysSnapshot.get(++idx);
			return key;
		}

		@Override
		public void remove() {
			if (!nextCalled) {
				throw new UnsupportedOperationException();
			}
			JsNativeMapWrapper.this.remove(key);
			nextCalled = false;
		}
	}

	public class TypedEntryIterator implements Iterator<Map.Entry<K, V>> {
		private KeyIterator keyIterator;

		public TypedEntryIterator() {
			this.keyIterator = new KeyIterator();
		}

		@Override
		public boolean hasNext() {
			return keyIterator.hasNext();
		}

		@Override
		public Map.Entry<K, V> next() {
			K key = (K) keyIterator.next();
			return new JsMapEntry(key, get(key));
		}

		@Override
		public void remove() {
			keyIterator.remove();
		}

		public final class JsMapEntry implements Map.Entry<K, V> {
			private V value;

			private K key;

			public JsMapEntry(K key, V value) {
				this.key = key;
				this.value = value;
			}

			@Override
			public K getKey() {
				return key;
			}

			@Override
			public V getValue() {
				return value;
			}

			@Override
			public V setValue(V value) {
				V old = (V) put(key, value);
				this.value = value;
				return old;
			}
		}
	}
}
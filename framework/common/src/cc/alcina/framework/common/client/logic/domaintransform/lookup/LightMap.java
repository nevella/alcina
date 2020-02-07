package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.domain.DomainCollections;

/**
 * 
 * @author nick@alcina.cc Minimises memory usage for small size, falls through
 *         for large
 * 
 *         Currently don't optimise with a separate array for hashes - not sure
 *         how much that would help js performance
 * 
 * @param <H>
 */
public class LightMap<K, V> implements Map<K, V>, Cloneable, Serializable {
	static final transient long serialVersionUID = 1;

	static final transient int DEGENERATE_THRESHOLD = 12;

	// key-value sequence
	private transient Object[] elementData = new Object[0];

	transient int size = 0;

	transient int modCount = 0;

	private transient Map<K, V> degenerate;

	public LightMap() {
	}

	@Override
	public void clear() {
		entrySet().clear();
	}

	@Override
	public boolean containsKey(Object key) {
		if (degenerate != null) {
			return degenerate.containsKey(key);
		}
		return getIndex(key) != -1;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (degenerate != null) {
			return degenerate.entrySet();
		}
		return new EntrySet();
	}

	@Override
	public V get(Object key) {
		if (degenerate != null) {
			return degenerate.get(key);
		}
		int idx = getIndex(key);
		return idx == -1 ? null : (V) elementData[idx * 2 + 1];
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {
			@Override
			public void clear() {
				LightMap.this.clear();
			}

			@Override
			public boolean contains(Object k) {
				return LightMap.this.containsKey(k);
			}

			@Override
			public boolean isEmpty() {
				return LightMap.this.isEmpty();
			}

			@Override
			public Iterator<K> iterator() {
				return new Iterator<K>() {
					private Iterator<Entry<K, V>> i = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override
					public K next() {
						return i.next().getKey();
					}

					@Override
					public void remove() {
						i.remove();
					}
				};
			}

			@Override
			public int size() {
				return LightMap.this.size();
			}
		};
	}

	@Override
	public V put(K key, V value) {
		if (degenerate != null) {
			return degenerate.put(key, value);
		}
		int idx = getIndex(key);
		if (idx != -1) {
			elementData[idx * 2 + 1] = value;
			return value;
		} else {
			if (size == DEGENERATE_THRESHOLD) {
				degenerate = DomainCollections.get().createUnsortedMap();
				degenerate.putAll(this);
				elementData = null;
				return degenerate.put(key, value);
			}
			size++;
			modCount++;
			idx = elementData.length;
			Object[] newData = new Object[idx + 2];
			System.arraycopy(elementData, 0, newData, 0, idx);
			newData[idx] = key;
			newData[idx + 1] = value;
			elementData = newData;
			return value;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V remove(Object key) {
		if (degenerate != null) {
			return degenerate.remove(key);
		}
		int idx = getIndex(key);
		if (idx != -1) {
			size--;
			modCount++;
			Object[] newData = new Object[size * 2];
			V result = (V) elementData[idx * 2 + 1];
			System.arraycopy(elementData, 0, newData, 0, idx * 2);
			System.arraycopy(elementData, (idx + 1) * 2, newData, idx * 2,
					(size - idx) * 2);
			elementData = newData;
			return result;
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		if (degenerate != null) {
			return degenerate.size();
		}
		return size;
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {
			@Override
			public void clear() {
				LightMap.this.clear();
			}

			@Override
			public boolean contains(Object v) {
				return LightMap.this.containsValue(v);
			}

			@Override
			public boolean isEmpty() {
				return LightMap.this.isEmpty();
			}

			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					private Iterator<Entry<K, V>> i = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override
					public V next() {
						return i.next().getValue();
					}

					@Override
					public void remove() {
						i.remove();
					}
				};
			}

			@Override
			public int size() {
				return LightMap.this.size();
			}
		};
	}

	private int getIndex(Object key) {
		for (int idx = 0; idx < size; idx++) {
			if (Objects.equals(key, elementData[idx * 2])) {
				return idx;
			}
		}
		return -1;
	}

	public class EntryIterator implements Iterator<Map.Entry<K, V>> {
		int idx = 0;

		int itrModCount = modCount;

		@Override
		public boolean hasNext() {
			return idx < size;
		}

		@Override
		public java.util.Map.Entry<K, V> next() {
			if (idx >= size) {
				throw new NoSuchElementException();
			}
			if (modCount != itrModCount) {
				throw new ConcurrentModificationException();
			}
			LightMapEntry entry = new LightMapEntry(idx++);
			return entry;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		public void reset() {
			idx = 0;
			itrModCount = modCount;
		}

		public final class LightMapEntry implements Map.Entry<K, V> {
			private int entryIdx;

			public LightMapEntry(int idx) {
				this.entryIdx = idx;
			}

			@Override
			public K getKey() {
				return (K) elementData[entryIdx * 2];
			}

			@Override
			public V getValue() {
				return (V) elementData[entryIdx * 2 + 1];
			}

			@Override
			public V setValue(V value) {
				elementData[entryIdx * 2 + 1] = value;
				return value;
			}
		}
	}

	class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public void clear() {
			elementData = new Object[0];
			size = 0;
			modCount = 0;
			degenerate = null;
		}

		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return size;
		}
	}
}

package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap.EntryIterator.LightMapEntry;

/**
 * 
 * @author nick@alcina.cc Minimises memory usage for small size, falls through
 *         for large
 * @param <H>
 */
public class LightMap<K, V> implements Map<K, V>, Cloneable, Serializable {
	public class EntryIterator implements Iterator<Entry<K, V>> {
		public final class LightMapEntry implements Entry<K, V> {
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

		int idx = 0;

		int itrModCount = modCount;

		@Override
		public boolean hasNext() {
			return idx < size;
		}

		@Override
		public java.util.Map.Entry<K, V> next() {
			if (modCount != itrModCount) {
				throw new ConcurrentModificationException();
			}
			LightMapEntry entry = new LightMapEntry(idx);
			return entry;
		}

		public void reset() {
			idx = 0;
			itrModCount = modCount;
		}
	}

	class EntrySet extends AbstractSet<Entry<K, V>> {
		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public void clear() {
			elementData = new Object[0];
			size = 0;
			modCount = 0;
			degenerate = null;
		}
	}

	static final transient long serialVersionUID = 1;

	// key-value sequence
	private transient Object[] elementData = new Object[0];

	transient int size = 0;

	transient int modCount = 0;

	private transient Map<K, V> degenerate;

	static final transient int DEGENERATE_THRESHOLD = 12;

	public LightMap() {
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (degenerate != null) {
			return degenerate.entrySet();
		}
		return new EntrySet();
	}

	@Override
	public int size() {
		if (degenerate != null) {
			return degenerate.size();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (degenerate != null) {
			return degenerate.containsKey(key);
		}
		return getIndex(key) != -1;
	}

	private int getIndex(Object key) {
		for (int idx = 0; idx < size; idx++) {
			if (Objects.equals(key, elementData[idx * 2])) {
				return idx;
			}
		}
		return -1;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
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
				degenerate = new LinkedHashMap<K, V>();
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
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public void clear() {
		entrySet().clear();
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {
			public Iterator<K> iterator() {
				return new Iterator<K>() {
					private Iterator<Entry<K, V>> i = entrySet().iterator();

					public boolean hasNext() {
						return i.hasNext();
					}

					public K next() {
						return i.next().getKey();
					}

					public void remove() {
						i.remove();
					}
				};
			}

			public int size() {
				return LightMap.this.size();
			}

			public boolean isEmpty() {
				return LightMap.this.isEmpty();
			}

			public void clear() {
				LightMap.this.clear();
			}

			public boolean contains(Object k) {
				return LightMap.this.containsKey(k);
			}
		};
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					private Iterator<Entry<K, V>> i = entrySet().iterator();

					public boolean hasNext() {
						return i.hasNext();
					}

					public V next() {
						return i.next().getValue();
					}

					public void remove() {
						i.remove();
					}
				};
			}

			public int size() {
				return LightMap.this.size();
			}

			public boolean isEmpty() {
				return LightMap.this.isEmpty();
			}

			public void clear() {
				LightMap.this.clear();
			}

			public boolean contains(Object v) {
				return LightMap.this.containsValue(v);
			}
		};
	}
}

package cc.alcina.framework.entity.persistence.domain;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import cc.alcina.framework.common.client.util.Ax;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

/*
 * This has a much lower memory requirement than Integer2Object
 */
public class ArrayBackedIntegerMap<V> implements Map<Integer, V> {
	private transient Object[] elementData;

	private volatile Int2ObjectLinkedOpenHashMap<V> failover;

	private int size;

	private Set<Integer> keySet;

	private AbstractCollection<V> values;

	private EntrySet entrySet;

	private int modCount;

	public ArrayBackedIntegerMap() {
		this(100);
	}

	public ArrayBackedIntegerMap(int size) {
		elementData = new Object[size];
	}

	@Override
	public void clear() {
		if (failover != null) {
			failover.clear();
		} else {
			elementData = new Object[100];
		}
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	private void ensureCapacity(int size) {
		if (size >= elementData.length) {
			Object oldData[] = elementData;
			int newCapacity = (elementData.length * 4) / 2 + 1;
			if (newCapacity < size) {
				newCapacity = size * 3 / 2;
			}
			Object[] copy = new Object[newCapacity];
			System.arraycopy(elementData, 0, copy, 0,
					Math.min(elementData.length, newCapacity));
			elementData = copy;
		}
	}

	@Override
	public Set<java.util.Map.Entry<Integer, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	@Override
	public V get(Object key) {
		if (key == null) {
			return null;
		}
		int idx = intKey(key);
		if (idx == 0) {
			return null;
		}
		if (idx == -1) {
			return failover.get(key);
		}
		return (V) elementData[idx];
	}

	private int intKey(Object key) {
		if (failover != null) {
			return -1;
		}
		if (key instanceof Integer) {
			long l = ((Integer) key).longValue();
			if (l == 0) {
				return 0;
			}
			if (l < 0) {
				throw new RuntimeException(
						"accessing array backed with negative index");
			}
			if (l < 25000000 && l > 0) {
				int idx = (int) l;
				ensureCapacity(idx + 1);
				return idx;
			}
		}
		synchronized (this) {
			if (this.failover == null) {
				Int2ObjectLinkedOpenHashMap failover = new Int2ObjectLinkedOpenHashMap<V>();
				failover.putAll(this);
				this.failover = failover;
				elementData = null;
			}
		}
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Set<Integer> keySet() {
		if (failover != null) {
			return failover.keySet();
		}
		if (keySet == null) {
			keySet = new AbstractSet<Integer>() {
				@Override
				public boolean contains(Object k) {
					return ArrayBackedIntegerMap.this.containsKey(k);
				}

				@Override
				public Iterator<Integer> iterator() {
					return new Iterator<Integer>() {
						private Iterator<Entry<Integer, V>> i = entrySet()
								.iterator();

						@Override
						public boolean hasNext() {
							return i.hasNext();
						}

						@Override
						public Integer next() {
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
					return ArrayBackedIntegerMap.this.size();
				}
			};
		}
		return keySet;
	}

	@Override
	public V put(Integer key, V value) {
		if (key == null) {
			throw new RuntimeException("Cannot put object with key:null");
		}
		int idx = intKey(key);
		if (idx == -1) {
			return failover.put(key, value);
		}
		if (idx == 0) {
			throw new RuntimeException("Cannot put object with key:0");
		}
		V existing = (V) elementData[idx];
		if (existing == null) {
			size++;
		}
		elementData[idx] = value;
		modCount++;
		return existing;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends V> m) {
		Set<Map.Entry<Integer, V>> entrySet = (Set) m.entrySet();
		for (Entry<Integer, V> entry : entrySet) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key) {
		int idx = intKey(key);
		if (idx == -1) {
			return failover.remove(key);
		}
		modCount++;
		V existing = (V) elementData[idx];
		if (existing != null) {
			size--;
		}
		elementData[idx] = null;
		return existing;
	}

	@Override
	public int size() {
		return failover != null ? failover.size() : size;
	}

	@Override
	public String toString() {
		return Ax.format("Array-backed long map - [%s] - %s",
				elementData == null ? "(failover)" + failover.size()
						: size + "," + elementData.length,
				entrySet().iterator().hasNext()
						? entrySet().iterator().next().getClass()
						: null);
	}

	@Override
	public Collection<V> values() {
		if (failover != null) {
			return failover.values();
		}
		if (values == null) {
			values = new AbstractCollection<V>() {
				@Override
				public boolean contains(Object v) {
					return ArrayBackedIntegerMap.this.containsValue(v);
				}

				@Override
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						private Iterator<Entry<Integer, V>> i = entrySet()
								.iterator();

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
					return ArrayBackedIntegerMap.this.size();
				}
			};
		}
		return values;
	}

	private final class EntrySet extends AbstractSet<Map.Entry<Integer, V>> {
		@Override
		public void clear() {
			ArrayBackedIntegerMap.this.clear();
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<Integer, V> e = (Map.Entry<Integer, V>) o;
			Entry<Integer, V> candidate = new ArrayBackedEntry(e.getKey());
			return candidate != null && candidate.equals(e);
		}

		@Override
		public Iterator<Map.Entry<Integer, V>> iterator() {
			return new ArrayBackedIterator();
		}

		@Override
		public boolean remove(Object o) {
			return ArrayBackedIntegerMap.this.remove(o) != null;
		}

		@Override
		public int size() {
			return size;
		}

		class ArrayBackedEntry implements Entry<Integer, V> {
			private Integer key;

			public ArrayBackedEntry(Integer key) {
				this.key = key;
			}

			@Override
			public final boolean equals(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry e = (Map.Entry) o;
				Object k1 = getKey();
				Object k2 = e.getKey();
				if (k1 == k2 || (k1 != null && k1.equals(k2))) {
					Object v1 = getValue();
					Object v2 = e.getValue();
					if (v1 == v2 || (v1 != null && v1.equals(v2)))
						return true;
				}
				return false;
			}

			@Override
			public Integer getKey() {
				return key;
			}

			@Override
			public V getValue() {
				V v = get(key);
				assert v != null;
				return v;
			}

			@Override
			public final int hashCode() {
				return (key == null ? 0 : key.hashCode())
						^ (getValue() == null ? 0 : getValue().hashCode());
			}

			@Override
			public V setValue(V value) {
				return put(key, value);
			}

			@Override
			public String toString() {
				return getKey() + "=" + getValue();
			}
		}

		class ArrayBackedIterator implements Iterator<Entry<Integer, V>> {
			int idx = -1;

			V poppedNextObject;

			boolean atEnd = false;

			boolean poppedNext = false;

			private int itrModCount;

			private int nextCount = 0;

			public ArrayBackedIterator() {
				itrModCount = modCount;
			}

			@Override
			public boolean hasNext() {
				maybePopNext();
				return !atEnd;
			}

			private void maybePopNext() {
				if (atEnd) {
					return;
				}
				if (!poppedNext) {
					poppedNext = true;
					atEnd = popNext();
					return;
				}
			}

			@Override
			public Entry<Integer, V> next() {
				if (atEnd && !poppedNext) {
					throw new NoSuchElementException();
				}
				maybePopNext();
				poppedNext = false;
				poppedNextObject = null;
				return new ArrayBackedEntry(Integer.valueOf(idx));
			}

			private boolean popNext() {
				while (nextCount < size && poppedNextObject == null) {
					if (modCount != itrModCount) {
						throw new ConcurrentModificationException();
					}
					poppedNextObject = (V) elementData[++idx];
				}
				return ++nextCount > size;
			}

			@Override
			public void remove() {
				ArrayBackedIntegerMap.this.remove(Integer.valueOf(idx));
				poppedNextObject = null;
				poppedNext = false;
				itrModCount++;
			}
		}
	}
}

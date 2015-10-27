package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;

public class ArrayBackedLongMap<V> implements Map<Long, V> {
	private transient Object[] elementData;

	private Long2ObjectLinkedOpenHashMap<V> failover;

	public ArrayBackedLongMap() {
		this(100);
	}

	public ArrayBackedLongMap(int size) {
		elementData = new Object[size];
	}

	private int size;

	private Set<Long> keySet;

	private AbstractCollection<V> values;

	private EntrySet entrySet;

	private int modCount;

	@Override
	public int size() {
		return failover != null ? failover.size() : size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
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
		if (key instanceof Long) {
			long l = ((Long) key).longValue();
			if (l == 0) {
				return 0;
			}
			if (l < 0) {
				throw new RuntimeException(
						"accessing array backed with negative index");
			}
			if (l < 10000000 && l > 0) {
				int idx = (int) l;
				ensureCapacity(idx);
				return idx;
			}
		}
		synchronized (this) {
			if (this.failover == null) {
				Long2ObjectLinkedOpenHashMap failover = new Long2ObjectLinkedOpenHashMap<V>();
				failover.putAll(this);
				this.failover = failover;
				elementData = null;
			}
		}
		return -1;
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
	public V put(Long key, V value) {
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
	public void putAll(Map<? extends Long, ? extends V> m) {
		Set<Map.Entry<Long, V>> entrySet = (Set) m.entrySet();
		for (Entry<Long, V> entry : entrySet) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		if (failover != null) {
			failover.clear();
		} else {
			elementData = new Object[100];
		}
	}

	public Set<Long> keySet() {
		if (failover != null) {
			return failover.keySet();
		}
		if (keySet == null) {
			keySet = new AbstractSet<Long>() {
				public Iterator<Long> iterator() {
					return new Iterator<Long>() {
						private Iterator<Entry<Long, V>> i = entrySet()
								.iterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public Long next() {
							return i.next().getKey();
						}

						public void remove() {
							i.remove();
						}
					};
				}

				public int size() {
					return ArrayBackedLongMap.this.size();
				}

				public boolean contains(Object k) {
					return ArrayBackedLongMap.this.containsKey(k);
				}
			};
		}
		return keySet;
	}

	@Override
	public Collection<V> values() {
		if (failover != null) {
			return failover.values();
		}
		if (values == null) {
			values = new AbstractCollection<V>() {
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						private Iterator<Entry<Long, V>> i = entrySet()
								.iterator();

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
					return ArrayBackedLongMap.this.size();
				}

				public boolean contains(Object v) {
					return ArrayBackedLongMap.this.containsValue(v);
				}
			};
		}
		return values;
	}

	@Override
	public Set<java.util.Map.Entry<Long, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	private final class EntrySet extends AbstractSet<Map.Entry<Long, V>> {
		public Iterator<Map.Entry<Long, V>> iterator() {
			return new ArrayBackedIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<Long, V> e = (Map.Entry<Long, V>) o;
			Entry<Long, V> candidate = new ArrayBackedEntry(e.getKey());
			return candidate != null && candidate.equals(e);
		}

		public boolean remove(Object o) {
			return ArrayBackedLongMap.this.remove(o) != null;
		}

		public int size() {
			return size;
		}

		public void clear() {
			ArrayBackedLongMap.this.clear();
		}

		class ArrayBackedIterator implements Iterator<Entry<Long, V>> {
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

			@Override
			public Entry<Long, V> next() {
				if (atEnd && !poppedNext) {
					throw new NoSuchElementException();
				}
				maybePopNext();
				poppedNext = false;
				poppedNextObject = null;
				return new ArrayBackedEntry(new Long(idx));
			}

			@Override
			public void remove() {
				ArrayBackedLongMap.this.remove(new Long(idx));
				poppedNextObject = null;
				poppedNext = false;
				itrModCount++;
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

			private boolean popNext() {
				while (nextCount < size && poppedNextObject == null) {
					if (modCount != itrModCount) {
						throw new ConcurrentModificationException();
					}
					poppedNextObject = (V) elementData[++idx];
				}
				return ++nextCount > size;
			}
		}

		class ArrayBackedEntry implements Entry<Long, V> {
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

			public final int hashCode() {
				return (key == null ? 0 : key.hashCode())
						^ (getValue() == null ? 0 : getValue().hashCode());
			}

			private Long key;

			public ArrayBackedEntry(Long key) {
				this.key = key;
			}

			@Override
			public Long getKey() {
				return key;
			}

			@Override
			public V getValue() {
				V v = get(key);
				assert v != null;
				return v;
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
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Array-backed long map - [%s] - %s",
				elementData == null ? "(failover)" + failover.size() : size
						+ "," + elementData.length, entrySet().iterator()
						.hasNext() ? entrySet().iterator().next().getClass()
						: null);
	}
}

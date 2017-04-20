package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.core.client.JavaScriptObject;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup.EntryIterator;

public class JsUniqueMap<K, V> implements Map<K, V> {
	private Function keyUniquenessMapper = Function.identity();

	private Function reverseKeyMapper = Function.identity();

	private JavascriptKeyableLookup lookup;

	private boolean intLookup = false;

	public static native boolean supportsJsMap()/*-{
        return !!(window.Map && window.Map.prototype.clear);
	}-*/;

	public static <K, V> Map<K, V> create(Class keyClass,
			boolean allowNativePartialSupportMap) {
		if (supportsJsMap() && allowNativePartialSupportMap) {
			return new JsNativeMapWrapper();
		} else {
			return new JsUniqueMap<>(keyClass);
		}
	}

	private JsUniqueMap(Class keyClass) {
		setupMappers(keyClass);
		lookup = JavascriptKeyableLookup.create(intLookup);
	}

	@Override
	public void clear() {
		lookup.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return lookup.containsKey(map(key));
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public V get(Object key) {
		return lookup.get(map(key));
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {
			public void clear() {
				JsUniqueMap.this.clear();
			}

			public boolean contains(Object k) {
				return JsUniqueMap.this.containsKey(k);
			}

			public boolean isEmpty() {
				return JsUniqueMap.this.isEmpty();
			}

			public Iterator<K> iterator() {
				return new Iterator<K>() {
					private Iterator<Entry<K, V>> i = entrySet().iterator();

					public boolean hasNext() {
						return i.hasNext();
					}

					public K next() {
						Map.Entry<K, V> entry = i.next();
						return entry.getKey();
					}

					public void remove() {
						i.remove();
					}
				};
			}

			public int size() {
				return JsUniqueMap.this.size();
			}
		};
	}

	@Override
	public V put(K key, V value) {
		return (V) lookup.put(map(key), value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.entrySet().forEach(e -> put(e.getKey(), e.getValue()));
	}

	@Override
	public V remove(Object key) {
		return (V) lookup.remove(map(key));
	}

	@Override
	public int size() {
		return lookup.size();
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {
			public void clear() {
				JsUniqueMap.this.clear();
			}

			public boolean contains(Object v) {
				return JsUniqueMap.this.containsValue(v);
			}

			public boolean isEmpty() {
				return JsUniqueMap.this.isEmpty();
			}

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
				return JsUniqueMap.this.size();
			}
		};
	}

	private void setupMappers(Class keyClass) {
		if (keyClass == int.class) {
			intLookup = true;
			return;
		}
		if (keyClass.isEnum()) {
			return;
		}
		if (keyClass == String.class || keyClass == Class.class) {
			return;
		}
		// if (keyClass == HasIdAndLocalId.class) {
		// intLookup = true;
		// return h -> h == null ? 0
		// : LongWrapperHash
		// .fastIntValue(((HasIdAndLocalId) h).getId());
		// }
		if (keyClass == Long.class) {
			intLookup = true;
			reverseKeyMapper = new Function<Integer, Long>() {
				@Override
				public Long apply(Integer t) {
					return LongWrapperHash.fastLongValue(t);
				}
			};
			keyUniquenessMapper = h -> h == null ? 0
					: LongWrapperHash.fastIntValue(((Long) h).longValue());
			return;
		}
		throw new RuntimeException("Not js-unique keyable");
	}

	private Object map(Object key) {
		return keyUniquenessMapper.apply(key);
	}

	public class TypedEntryIterator implements Iterator<Map.Entry<K, V>> {
		private JavascriptKeyableLookup.EntryIterator entryIterator;

		public TypedEntryIterator() {
			this.entryIterator = (JavascriptKeyableLookup.EntryIterator) lookup
					.entryIterator();
		}

		@Override
		public boolean hasNext() {
			return entryIterator.hasNext();
		}

		@Override
		public Map.Entry<K, V> next() {
			V value = (V) entryIterator.next();
			return new JsMapEntry((K) reverseKeyMapper.apply(entryIterator.key),
					value);
		}

		@Override
		public void remove() {
			entryIterator.remove();
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
				V old = (V) lookup.put(key, value);
				this.value = value;
				return old;
			}
		}
	}

	class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public void clear() {
			JsUniqueMap.this.clear();
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new TypedEntryIterator();
		}

		@Override
		public int size() {
			return JsUniqueMap.this.size();
		}
	}
}
package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JsUniqueMap<K, V> implements Map<K, V> {
    public static <K, V> Map<K, V> create(Class keyClass,
            boolean allowNativePartialSupportMap) {
        if (supportsJsMap() && allowNativePartialSupportMap) {
            return new JsNativeMapWrapper(false);
        } else {
            return new JsUniqueMap<>(keyClass);
        }
    }

    public static <K, V> Map<K, V> createWeakMap() {
        if (supportsJsWeakMap()) {
            return new JsNativeMapWrapper(true);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static native boolean supportsJsMap()/*-{
    return !!(window.Map && window.Map.prototype.clear);
    }-*/;

    public static native boolean supportsJsWeakMap()/*-{
    return !!(window.WeakMap && window.WeakMap.prototype.get);
    }-*/;

    private Function keyUniquenessMapper = Function.identity();

    private Function reverseKeyMapper = Function.identity();

    private JavascriptKeyableLookup lookup;

    private boolean intLookup = false;

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
            @Override
            public void clear() {
                JsUniqueMap.this.clear();
            }

            @Override
            public boolean contains(Object k) {
                return JsUniqueMap.this.containsKey(k);
            }

            @Override
            public boolean isEmpty() {
                return JsUniqueMap.this.isEmpty();
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
                        Map.Entry<K, V> entry = i.next();
                        return entry.getKey();
                    }

                    @Override
                    public void remove() {
                        i.remove();
                    }
                };
            }

            @Override
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
            @Override
            public void clear() {
                JsUniqueMap.this.clear();
            }

            @Override
            public boolean contains(Object v) {
                return JsUniqueMap.this.containsValue(v);
            }

            @Override
            public boolean isEmpty() {
                return JsUniqueMap.this.isEmpty();
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
                return JsUniqueMap.this.size();
            }
        };
    }

    private Object map(Object key) {
        return keyUniquenessMapper.apply(key);
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
        // if (keyClass == Entity.class) {
        // intLookup = true;
        // return h -> h == null ? 0
        // : LongWrapperHash
        // .fastIntValue(((Entity) h).getId());
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
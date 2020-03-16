/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilters;


/**
 *
 * @author Nick Reddel
 */
public class Multimap<K, V extends List>
        implements Map<K, V>, Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    public static Multimap<String, List<String>> fromPropertyString(String text,
            boolean unQuote) {
        Multimap<String, List<String>> map = new Multimap();
        if (text == null) {
            return map;
        }
        for (String line : text.split("\n")) {
            int idx = line.indexOf("=");
            if (idx != -1 && !line.startsWith("#")) {
                String value = line.substring(idx + 1).replace("\\n", "\n")
                        .replace("\\=", "=").replace("\\\\", "\\");
                if (unQuote && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                map.add(line.substring(0, idx), value);
            }
        }
        return map;
    }

    private Map<K, V> map;

    public Multimap() {
        map = createMap();
    }

    public void add(K key, Object item) {
        if (!containsKey(key)) {
            put(key, (V) new ArrayList());
        }
        get(key).add(item);
    }

    public void addAll(Map<K, V> otherMultimap) {
        for (K k : otherMultimap.keySet()) {
            getAndEnsure(k).addAll(otherMultimap.get(k));
        }
    }

    public void addCollection(K key, Collection collection) {
        if (!containsKey(key)) {
            put(key, (V) new ArrayList());
        }
        get(key).addAll(collection);
    }

    public void addIfNotContained(K key, Object item) {
        if (!containsKey(key)) {
            put(key, (V) new ArrayList());
        }
        V v = get(key);
        if (!v.contains(item)) {
            v.add(item);
        }
    }

    public V allItems() {
        List list = new ArrayList();
        for (V v : values()) {
            list.addAll(v);
        }
        return (V) list;
    }

    public V allNonFirstItems() {
        List result = new ArrayList();
        for (V v : values()) {
            for (int i = 1; i < v.size(); i++) {
                result.add(v.get(i));
            }
        }
        return (V) result;
    }

    public CountingMap<K> asCountingMap() {
        CountingMap<K> countingMap = new CountingMap<>();
        countingMap.addMultimap((Multimap) this);
        return countingMap;
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
        return this.map.containsValue(value);
    }

    public Multimap<K, V> copy() {
        Multimap<K, V> copy = new Multimap<K, V>();
        for (Entry<K, V> entry : entrySet()) {
            copy.put(entry.getKey(),
                    (V) new ArrayList((List) entry.getValue()));
        }
        return copy;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return this.map.equals(o);
    }

    public <T> T first(K key) {
        return (T) CommonUtils.first(getAndEnsure(key));
    }

    @Override
    public V get(Object key) {
        return this.map.get(key);
    }

    public V getAndEnsure(K key) {
        if (!containsKey(key)) {
            put(key, (V) new ArrayList());
        }
        return get(key);
    }

    public Collection getForKeys(List keys) {
        Set dedupe = new LinkedHashSet();
        for (Object key : keys) {
            if (containsKey(key)) {
                dedupe.addAll(get(key));
            }
        }
        return dedupe;
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    public Multimap invert() {
        Multimap result = new Multimap();
        for (Map.Entry<K, V> entry : entrySet()) {
            for (Object o : entry.getValue()) {
                result.add(o, entry.getKey());
            }
        }
        return result;
    }

    public Map invertAsMap() {
        Map result = new LinkedHashMap();
        for (Map.Entry<K, V> entry : entrySet()) {
            for (Object o : entry.getValue()) {
                result.put(o, entry.getKey());
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public int itemSize() {
        int iSize = 0;
        for (V v : values()) {
            iSize += v.size();
        }
        return iSize;
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    public K maxKey() {
        K max = null;
        for (K k : keySet()) {
            if (max == null || get(k).size() > get(max).size()) {
                max = k;
            }
        }
        return max;
    }

    public <T extends Comparable> Map<K, T> maxMap() {
        Map<K, T> result = new LinkedHashMap<K, T>();
        for (java.util.Map.Entry<K, V> e : entrySet()) {
            result.put(e.getKey(), (T) CollectionFilters.max(e.getValue()));
        }
        return result;
    }

    public K minKey() {
        K min = null;
        for (K k : keySet()) {
            if (min == null || get(k).size() < get(min).size()) {
                min = k;
            }
        }
        return min;
    }

    public <T extends Comparable> Map<K, T> minMap() {
        Map<K, T> result = new LinkedHashMap<K, T>();
        for (java.util.Map.Entry<K, V> e : entrySet()) {
            result.put(e.getKey(), (T) CollectionFilters.min(e.getValue()));
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        return this.map.put(key, value);
    }

    // TODO - check usage - probably don't want this since lists aren't cloned
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        this.map.putAll(m);
    }

    @Override
    public V remove(Object key) {
        return this.map.remove(key);
    }

    // public boolean remove(Object key, Object value) {
    //
    // }
    public void removeValue(Object value) {
        for (V v : values()) {
            v.remove(value);
        }
    }

    @Override
    public int size() {
        return this.map.size();
    }

    public void subtract(K key, Object item) {
        if (containsKey(key)) {
            get(key).remove(item);
        }
    }

    @Override
    public String toString() {
        return isEmpty() ? "{}" : CommonUtils.join(entrySet(), "\n");
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    private Map<K, V> createMap() {
        return new LinkedHashMap<K, V>();
    }
}

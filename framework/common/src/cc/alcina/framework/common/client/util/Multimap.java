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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilters;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class Multimap<K, V extends List> extends LinkedHashMap<K, V> {
	public Multimap() {
		super();
	}

	public Multimap(int initialCapacity) {
		super(initialCapacity);
	}

	public V allItems() {
		List list = new ArrayList();
		for (V v : values()) {
			list.addAll(v);
		}
		return (V) list;
	}

	public V getAndEnsure(K key) {
		if (!containsKey(key)) {
			put(key, (V) new ArrayList());
		}
		return get(key);
	}

	public void add(K key, Object item) {
		if (!containsKey(key)) {
			put(key, (V) new ArrayList());
		}
		get(key).add(item);
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

	public void remove(K key, Object item) {
		if (containsKey(key)) {
			get(key).remove(item);
		}
	}

	public void addAll(Multimap<K, V> otherMultimap) {
		for (K k : otherMultimap.keySet()) {
			getAndEnsure(k).addAll(otherMultimap.get(k));
		}
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

	public K minKey() {
		K min = null;
		for (K k : keySet()) {
			if (min == null || get(k).size() < get(min).size()) {
				min = k;
			}
		}
		return min;
	}

	public void removeValue(Object value) {
		for (V v : values()) {
			v.remove(value);
		}
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

	public Collection getForKeys(List keys) {
		Set dedupe = new LinkedHashSet();
		for (Object key : keys) {
			if (containsKey(key)) {
				dedupe.addAll(get(key));
			}
		}
		return dedupe;
	}

	public <T extends Comparable> Map<K, T> maxMap() {
		Map<K, T> result = new LinkedHashMap<K, T>();
		for (java.util.Map.Entry<K, V> e : entrySet()) {
			result.put(e.getKey(), (T) CollectionFilters.max(e.getValue()));
		}
		return result;
	}
	public <T extends Comparable> Map<K, T> minMap() {
		Map<K, T> result = new LinkedHashMap<K, T>();
		for (java.util.Map.Entry<K, V> e : entrySet()) {
			result.put(e.getKey(), (T) CollectionFilters.min(e.getValue()));
		}
		return result;
	}
}

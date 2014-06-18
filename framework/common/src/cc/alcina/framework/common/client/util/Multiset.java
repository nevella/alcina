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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <K>
 * @param <V>
 */
@SuppressWarnings("unchecked")
public class Multiset<K, V extends Set> implements Serializable {
	private LinkedHashMap<K, V> map = new LinkedHashMap<K, V>();

	public boolean add(K key, Object item) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		return map.get(key).add(item);
	}

	protected Set createSet() {
		return new LinkedHashSet();
	}

	public void remove(K key, Object item) {
		if (map.containsKey(key)) {
			map.get(key).remove(item);
		}
	}

	public V getAndEnsure(K key) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		return map.get(key);
	}

	public void addCollection(K key, Collection collection) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		map.get(key).addAll(collection);
	}

	public V get(Object key) {
		return this.map.get(key);
	}

	public void clear() {
		this.map.clear();
	}

	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	public V put(K key, V value) {
		return this.map.put(key, value);
	}

	public Set<K> keySet() {
		return this.map.keySet();
	}

	public Collection<V> values() {
		return this.map.values();
	}

	public int size() {
		return this.map.size();
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public V remove(Object key) {
		return this.map.remove(key);
	}

	public Set<Entry<K, V>> entrySet() {
		return this.map.entrySet();
	}

	public void addAll(Multiset<K, V> other) {
		for (Entry<K, V> entry : other.entrySet()) {
			addCollection(entry.getKey(), entry.getValue());
		}
	}
	public void addAll(Multimap<K, List> other) {
		for (Entry<K, List> entry : other.entrySet()) {
			addCollection(entry.getKey(), entry.getValue());
		}
	}

	public V allItems() {
		Set set = createSet();
		for (V v : values()) {
			set.addAll(v);
		}
		return (V) set;
	}
}

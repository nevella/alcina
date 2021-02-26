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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import cc.alcina.framework.common.client.domain.DomainCollections;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <K>
 * @param <V>
 */
public class Multiset<K, V extends Set> implements Serializable {
	static final transient long serialVersionUID = -1L;

	protected Map<K, V> map;

	public Multiset() {
		createTopMap();
	}

	public boolean add(K key, Object item) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		return map.get(key).add(item);
	}

	public void addAll(Multimap<K, List> other) {
		for (Entry<K, List> entry : other.entrySet()) {
			addCollection(entry.getKey(), entry.getValue());
		}
	}

	public void addAll(Multiset<K, V> other) {
		for (Entry<K, V> entry : other.entrySet()) {
			addCollection(entry.getKey(), entry.getValue());
		}
	}

	public void addCollection(K key, Collection collection) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		map.get(key).addAll(collection);
	}

	public V allItems() {
		Set set = createSet();
		for (V v : values()) {
			set.addAll(v);
		}
		return (V) set;
	}

	public void clear() {
		this.map.clear();
	}

	public boolean contains(K key, Object value) {
		if (map.containsKey(key)) {
			return map.get(key).contains(value);
		}
		return false;
	}

	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	public Set<Entry<K, V>> entrySet() {
		return this.map.entrySet();
	}

	public void forEach(BiConsumer<? super K, ? super V> action) {
		this.map.forEach(action);
	}

	public V get(Object key) {
		return this.map.get(key);
	}

	public V getAndEnsure(K key) {
		if (!map.containsKey(key)) {
			map.put(key, (V) createSet());
		}
		return map.get(key);
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public Set<K> keySet() {
		return this.map.keySet();
	}

	public V put(K key, V value) {
		return this.map.put(key, value);
	}

	public void remove(K key, Object item) {
		if (map.containsKey(key)) {
			map.get(key).remove(item);
		}
	}

	public V remove(Object key) {
		return this.map.remove(key);
	}

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

	public Collection<V> values() {
		return this.map.values();
	}

	protected Set createSet() {
		return DomainCollections.get().createLightSet();
	}

	protected void createTopMap() {
		map = DomainCollections.get().createUnsortedMap();
	}
}

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
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class SortedMultimap<K, V extends List> extends TreeMap<K, V> {
	public SortedMultimap() {
		super();
	}

	public SortedMultimap(Comparator<? super K> comparator) {
		super(comparator);
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

	public void subtract(K key, Object item) {
		if (containsKey(key)) {
			get(key).remove(item);
		}
	}

	public void addAll(Multimap<K, V> otherMultimap) {
		for (K k : otherMultimap.keySet()) {
			getAndEnsure(k).addAll(otherMultimap.get(k));
		}
	}

	@Override
	public String toString() {
		return CommonUtils.join(entrySet(), "\n");
	}
}

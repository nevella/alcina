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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class SortedMultimap<K, V extends List> extends TreeMap<K, V> {
	private transient KeysHelper keysHelper;

	public SortedMultimap() {
		super();
	}

	public SortedMultimap(Comparator<? super K> comparator) {
		super(comparator);
	}

	public void add(K key, Object item) {
		if (!containsKey(key)) {
			put(key, (V) new ArrayList());
		}
		get(key).add(item);
	}

	public void addAll(Multimap<K, V> otherMultimap) {
		for (K k : otherMultimap.keySet()) {
			getAndEnsure(k).addAll(otherMultimap.get(k));
		}
	}

	public V allItems() {
		List list = new ArrayList();
		for (V v : values()) {
			list.addAll(v);
		}
		return (V) list;
	}

	public Multimap<K, V> asMultimap() {
		Multimap<K, V> result = new Multimap<K, V>();
		result.putAll(this);
		return result;
	}

	public V getAndEnsure(K key) {
		if (!containsKey(key)) {
			put(key, (V) new ArrayList());
		}
		return get(key);
	}

	public KeysHelper keysHelper() {
		if (keysHelper == null) {
			keysHelper = new KeysHelper();
		}
		return keysHelper;
	}

	public void subtract(K key, Object item) {
		if (containsKey(key)) {
			get(key).remove(item);
		}
	}

	@Override
	public String toString() {
		return CommonUtils.join(entrySet(), "\n");
	}

	public class KeysHelper {
		List<K> keys = new ArrayList<K>(SortedMultimap.this.keySet());

		Map<K, Integer> indicies = new LinkedHashMap<K, Integer>();
		{
			for (int i = 0; i < keys.size(); i++) {
				indicies.put(keys.get(i), i);
			}
		}

		public int firstIndex(K key, int dir, boolean equals) {
			int idx = Collections.binarySearch(keys, key, comparator());
			idx = idx < 0 ? -idx - 1 : idx;
			if (idx == -1 || idx == keys.size()) {
				/*
				 * intention - if gt and key lt all, return 0 - basically make
				 * iterator behave nicely
				 */
				return idx + ((equals) ? 0 : dir);
			}
			if (!equals && keys.get(idx).equals(key)) {
				idx += dir;
			} else {
				if (dir == -1) {
					idx--;
				}
			}
			return idx;
		}

		public Iterator<K> iterator(K key, int dir, boolean equals) {
			return new KeysHelperItr(key, dir, equals);
		}

		class KeysHelperItr implements Iterator<K> {
			int idx = -1;

			private int dir;

			public KeysHelperItr(K key, int dir, boolean equals) {
				this.dir = dir;
				idx = firstIndex(key, dir, equals);
			}

			@Override
			public boolean hasNext() {
				return idx >= 0 && idx < keys.size();
			}

			@Override
			public K next() {
				if (idx < 0 || idx >= keys.size()) {
					throw new NoSuchElementException();
				}
				K k = keys.get(idx);
				idx += dir;
				return k;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
	}
}

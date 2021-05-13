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
package cc.alcina.framework.common.client.util.trie;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CountingMap;

/**
 *
 * @author Nick Reddel
 * 
 *         Initial population is better pushed to a standard map then
 *         transferred to the Trie at the end of the population phase (Map
 *         lookup is a lot faster than trie lookup)
 */
public class MultiTrie<K, V extends Set<? extends Entity>>
		extends PatriciaTrie<K, V> {
	private boolean loadingOnly;

	private LoadingCache loadingCache = new LoadingCache();

	public MultiTrie(KeyAnalyzer<? super K> arg0) {
		super(arg0);
	}

	public boolean add(K key, Entity item) {
		if (loadingOnly) {
			return loadingCache.addCached(key, item);
		}
		if (!containsKey(key)) {
			put(key, (V) createNewSet());
		}
		Set set = (Set) get(key);
		if (set.contains(item)) {
			return false;
		} else {
			set.add(item);
			return true;
		}
	}

	public V allItems() {
		Set set = new LiSet();
		for (V v : values()) {
			set.addAll(v);
		}
		return (V) set;
	}

	public void dumpDensity() {
		Ax.out("All values: %s", size());
		CountingMap<Integer> sizeCount = new CountingMap<>();
		keySet().stream().map(k -> get(k).size()).forEach(sizeCount::add);
		sizeCount.toLinkedHashMap(true).entrySet().forEach(Ax::out);
	}

	public V getAndEnsure(K key) {
		if (!containsKey(key)) {
			put(key, (V) createNewSet());
		}
		return get(key);
	}

	public MultiTrieResult<K, V> getPrefixedByTrieResult(K key) {
		SortedMap<K, V> map = super.prefixMap(key);
		return new MultiTrieResult(key, map);
	}

	public boolean isLoadingOnly() {
		return this.loadingOnly;
	}

	public void removeKeyItem(K key, Entity item) {
		if (containsKey(key)) {
			get(key).remove(item);
			if (get(key).isEmpty()) {
				// FIXME - this causes infinite trie loops (for transactional
				// tries).
				// mostly occurs during reindex cycles - leaving the trie entry
				// in *seems* harmless
				// remove(key);
				//
				// FIXME - mvcc.2021
			}
		}
	}

	public void setLoadingOnly(boolean loadingOnly) {
		if (loadingOnly == false && this.loadingOnly == true) {
			loadingCache.pushToTrie();
			loadingCache = null;
		}
		this.loadingOnly = loadingOnly;
	}

	protected V createNewSet() {
		return (V) new LiSet();
	}

	public static class MultiTrieResult<K, V> {
		public SortedMap<K, V> map;

		public K key;

		public int size = -1;

		public MultiTrieResult(K key, SortedMap<K, V> map) {
			this.key = key;
			this.map = map;
		}

		public int size() {
			if (size == -1) {
				size = map.values().stream()
						.collect(Collectors.summingInt(v -> ((Set) v).size()));
			}
			return size;
		}
	}

	class LoadingCache {
		private Map<K, Set<Entity>> cache = new LinkedHashMap<>();

		public boolean addCached(K key, Entity item) {
			if (!cache.containsKey(key)) {
				cache.put(key, (Set<Entity>) createNewSet());
			}
			return cache.get(key).add(item);
		}

		public void pushToTrie() {
			cache.entrySet().forEach(e -> {
				put(e.getKey(), (V) e.getValue());
			});
		}
	}
}

package cc.alcina.extras.collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import cc.alcina.framework.common.client.util.SortedMultimap;

public class SortedCountingMap<K> extends TreeMap<K, Integer> {
	public void add(K key) {
		if (!containsKey(key)) {
			put(key, 1);
		} else {
			put(key, get(key) + 1);
		}
	}
	public void add(K key,int i) {
		if (!containsKey(key)) {
			put(key, i);
		} else {
			put(key, get(key) + i);
		}
	}
	public int size(K key) {
		if (!containsKey(key)) {
			return 0;
		}
		return get(key);
	}

	public K max() {
		K max = null;
		Integer maxCount = 0;
		for (K k : keySet()) {
			if (max == null) {
				max = k;
				maxCount = get(k);
			} else {
				if (get(k).compareTo(maxCount) > 0) {
					max = k;
					maxCount = get(k);
				}
			}
		}
		return max;
	}

	public SortedMultimap<Integer, List<K>> reverseMap(boolean descending) {
		SortedMultimap<Integer, List<K>> result = descending ? new SortedMultimap<Integer, List<K>>(
				Collections.reverseOrder())
				: new SortedMultimap<Integer, List<K>>();
		for (K key : keySet()) {
			result.add(get(key), key);
		}
		return result;
	}
	
}

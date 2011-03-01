package cc.alcina.extras.collections;

import java.util.HashMap;

public class TotallingMap<K> extends HashMap<K, Integer> {
	public void add(K key,int i) {
		if (!containsKey(key)) {
			put(key, i);
		} else {
			put(key, get(key) + i);
		}
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
}

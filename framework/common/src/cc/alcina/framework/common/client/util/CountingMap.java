package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CountingMap<K> extends LinkedHashMap<K, Integer> {
	public int add(K key) {
		return add(key, 1);
	}

	public int add(K key, int i) {
		int nextValue = i;
		if (containsKey(key)) {
			nextValue = get(key) + i;
		}
		put(key, nextValue);
		return nextValue;
	}

	public int countFor(K key) {
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
	public K min() {
		K min = null;
		Integer minCount = Integer.MAX_VALUE;
		for (K k : keySet()) {
			if (min == null) {
				min = k;
				minCount = get(k);
			} else {
				if (get(k).compareTo(minCount) < 0) {
					min = k;
					minCount = get(k);
				}
			}
		}
		return min;
	}

	public int sum() {
		int result = 0;
		for (Integer v : values()) {
			result += v;
		}
		return result;
	}

	public int weightedAvg() {
		Set<java.util.Map.Entry<K, Integer>> es = entrySet();
		int weight = 0;
		for (Map.Entry<K, Integer> e : es) {
			weight += ((Number) e.getKey()).intValue() * e.getValue();
		}
		int sum = sum();
		return sum == 0 ? 0 : weight / sum;
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

	public LinkedHashMap<K, Integer> toLinkedHashMap(boolean descending) {
		SortedMultimap<Integer, List<K>> m = reverseMap(descending);
		List<K> allItems = m.allItems();
		LinkedHashMap<K, Integer> result = new LinkedHashMap<K, Integer>();
		for (K k : allItems) {
			result.put(k, get(k));
		}
		return result;
	}

	public void addMultimap(Multimap<K, List> mm) {
		for (Map.Entry<K, List> entry : mm.entrySet()) {
			add(entry.getKey(), entry.getValue().size());
		}
	}

	public CountingMap() {
	}

	public CountingMap(Multimap<K, List> mm) {
		addMultimap(mm);
	}

	@Override
	public String toString() {
		return CommonUtils.join(entrySet(), "\n");
	}
}

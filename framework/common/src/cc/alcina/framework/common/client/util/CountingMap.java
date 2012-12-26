package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class CountingMap<K> extends HashMap<K, Integer> {
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
	public int sum(){
		int result=0;
		for(Integer v:values()){
			result+=v;
		}
		return result;
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
	public LinkedHashMap<K,Integer> toLinkedHashMap(boolean descending){
		SortedMultimap<Integer, List<K>> m = reverseMap(descending);
		List<K> allItems = m.allItems();
		LinkedHashMap<K, Integer> result = new LinkedHashMap<K, Integer>();
		for (K k : allItems) {
			result.put(k, get(k));
		}
		return result;
	}
	public void addMultimap(Multimap<K,List> mm){
		for (Map.Entry<K, List> entry :  mm.entrySet()) {
			add(entry.getKey(),entry.getValue().size());
		}
	}
	public CountingMap() {
	}
	public CountingMap(Multimap<K,List> mm) {
		addMultimap(mm);
	}
	
}

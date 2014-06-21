package cc.alcina.framework.entity.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.CountingMap;

public class LimitCountingMap<K> extends CountingMap<K> {
	private Map<K, Integer> limits = new LinkedHashMap<K, Integer>();

	public boolean ignore(K key) {
		int i = add(key, 1);
		return !limits.containsKey(key) || i <= limits.get(key);
	}

	public void setLimit(K key, int limit) {
		limits.put(key, limit);
	}
}
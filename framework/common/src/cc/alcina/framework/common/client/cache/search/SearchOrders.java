package cc.alcina.framework.common.client.cache.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SearchOrders<T> implements Comparator<T>, Serializable {
	Map<SearchOrder<T>, Boolean> cmps = new LinkedHashMap<>();

	public SearchOrders() {
	}

	@Override
	public int compare(T o1, T o2) {
		for (Entry<SearchOrder<T>, Boolean> entry : cmps.entrySet()) {
			SearchOrder<T> cmp = entry.getKey();
			int i = cmp.compare(o1, o2);
			if (i != 0) {
				return i * (entry.getValue() ? 1 : -1);
			}
		}
		return 0;
	}

	public void addOrder(SearchOrder sortFunction, boolean ascending) {
		cmps.put(sortFunction, ascending);
	}
}

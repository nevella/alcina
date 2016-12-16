package cc.alcina.framework.common.client.cache.search;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class SearchOrders<T> implements Comparator<T>, Serializable {
	Map<SearchOrder<T>, Boolean> cmps = new LinkedHashMap<>();

	public SearchOrders() {
	}

	public SearchOrders<T> addHiliOrder() {
		addOrder(new IdOrder(), true);
		return this;
	}

	private transient Entry<SearchOrder<T>, Boolean> soleOrder = null;

	@Override
	public int compare(T o1, T o2) {
		if (soleOrder == null && cmps.size() == 1) {
			soleOrder = CommonUtils.first(cmps.entrySet());
		}
		if (soleOrder != null) {
			return soleOrder.getKey().compare(o1, o2)
					* (soleOrder.getValue() ? 1 : -1);
		}
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

	public static class IdOrder<H extends HasId> implements SearchOrder<H> {
		@Override
		public Comparable apply(H t) {
			return t.getId();
		}
	}
}

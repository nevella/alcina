package cc.alcina.framework.common.client.cache.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;

@ClientInstantiable
@Introspectable
public class SearchOrders<T> implements Comparator<T>, Serializable,
		HasEquivalence<SearchOrders<T>> {
	private Map<SearchOrder<T, ?>, Boolean> cmps = new LinkedHashMap<>();

	private List<SerializableSearchOrder> serializableSearchOrders = new ArrayList<>();

	private transient Entry<SearchOrder<T, ?>, Boolean> soleOrder = null;

	public SearchOrders() {
	}

	public SearchOrders<T> addHiliDescOrder() {
		addOrder(new IdOrder(), false);
		return this;
	}

	public SearchOrders<T> addHiliOrder() {
		addOrder(new IdOrder(), true);
		return this;
	}

	public void addOrder(SearchOrder sortFunction, boolean ascending) {
		_getCmps().put(sortFunction, ascending);
		refreshSerializable();
	}

	@Override
	public int compare(T o1, T o2) {
		if (soleOrder == null && _getCmps().size() == 1) {
			soleOrder = CommonUtils.first(_getCmps().entrySet());
		}
		if (soleOrder != null) {
			return soleOrder.getKey().compare(o1, o2)
					* (soleOrder.getValue() ? 1 : -1);
		}
		for (Entry<SearchOrder<T, ?>, Boolean> entry : _getCmps().entrySet()) {
			SearchOrder<T, ?> cmp = entry.getKey();
			int i = cmp.compare(o1, o2);
			if (i != 0) {
				return i * (entry.getValue() ? 1 : -1);
			}
		}
		return 0;
	}

	@Override
	public boolean equivalentTo(SearchOrders<T> other) {
		refreshSerializable();
		other.refreshSerializable();
		return HasEquivalenceHelper.equivalent(serializableSearchOrders,
				other.serializableSearchOrders);
	}

	public List<SerializableSearchOrder> getSerializableSearchOrders() {
		return this.serializableSearchOrders;
	}

	public boolean isEmpty() {
		return _getCmps().size() == 0;
	}

	public void setSerializableSearchOrders(
			List<SerializableSearchOrder> serializableSearchOrders) {
		this.serializableSearchOrders = serializableSearchOrders;
	}

	private void refreshSerializable() {
		serializableSearchOrders = cmps.entrySet().stream()
				.map(e -> new SerializableSearchOrder(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	Map<SearchOrder<T, ?>, Boolean> _getCmps() {
		if (cmps.isEmpty() && serializableSearchOrders.size() > 0) {
			cmps = (Map) serializableSearchOrders.stream()
					.collect(Collectors.toMap(sso -> {
						Class clazz = Reflections.classLookup()
								.getClassForName(sso.getSearchOrderClassName());
						return (SearchOrder) Reflections.classLookup()
								.newInstance(clazz);
					}, sso -> sso.isAscending()));
		}
		return cmps;
	}

	public static class IdOrder<H extends HasId>
			implements SearchOrder<H, Long> {
		@Override
		public Long apply(H t) {
			return t.getId();
		}
	}

	@ClientInstantiable
	@Introspectable
	public static class SerializableSearchOrder implements Serializable,
			HasReflectiveEquivalence<SerializableSearchOrder> {
		private String searchOrderClassName;

		private boolean ascending;

		public SerializableSearchOrder() {
		}

		public SerializableSearchOrder(SearchOrder searchOrder,
				Boolean ascending) {
			setSearchOrderClassName(searchOrder.getClass().getName());
			setAscending(ascending);
		}

		public String getSearchOrderClassName() {
			return this.searchOrderClassName;
		}

		public boolean isAscending() {
			return this.ascending;
		}

		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}

		public void setSearchOrderClassName(String searchOrderClassName) {
			this.searchOrderClassName = searchOrderClassName;
		}
	}
}

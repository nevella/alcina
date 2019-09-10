package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;

@ClientInstantiable
@Introspectable
public class SearchOrders<T> implements Comparator<T>, Serializable,
		HasEquivalence<SearchOrders<T>> {
	/*
	 * Don't access directly - even when altering (call refreshSerializable when
	 * altering)
	 */
	private Map<SearchOrder<T, ?>, Boolean> cmps = new LinkedHashMap<>();

	private List<SerializableSearchOrder> serializableSearchOrders = new ArrayList<>();

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

	public boolean hasOrder(Class<SearchOrder> clazz) {
		return _getCmps().keySet().stream()
				.anyMatch(cmp -> cmp.getClass() == clazz);
	}

	public boolean isEmpty() {
		return _getCmps().size() == 0;
	}

	public boolean removeOrder(Class<SearchOrder> clazz) {
		int size = _getCmps().size();
		boolean result = _getCmps().keySet()
				.removeIf(cmp -> cmp.getClass() == clazz);
		refreshSerializable();
		return result;
	}

	public void setSerializableSearchOrders(
			List<SerializableSearchOrder> serializableSearchOrders) {
		this.serializableSearchOrders = serializableSearchOrders;
	}

	public boolean startsWith(SearchOrder order) {
		return _getCmps().size() > 0 && _getCmps().keySet().iterator().next()
				.getClass() == order.getClass();
	}

	@Override
	public String toString() {
		return _getCmps().entrySet().isEmpty() ? ""
				: new FormatBuilder().prefix("Order by: ").separator(", ")
						.appendIfNotBlank(_getCmps().entrySet().stream()
								.map(this::cmpMapper))
						.toString();
	}

	private String cmpMapper(Entry<SearchOrder<T, ?>, Boolean> entry) {
		return Ax.format("%s %s", entry.getKey(),
				entry.getValue() ? "desc" : "asc");
	}

	private void refreshSerializable() {
		// direct access ok
		serializableSearchOrders = cmps.entrySet().stream()
				.map(e -> new SerializableSearchOrder(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	Map<SearchOrder<T, ?>, Boolean> _getCmps() {
		// direct access ok
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

	@ClientInstantiable
	@Introspectable
	public static class ColumnSearchOrder implements Serializable {
		private String columnName;

		private boolean ascending;

		public String getColumnName() {
			return columnName;
		}

		public boolean isAscending() {
			return ascending;
		}

		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}

		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		@Override
		public String toString() {
			return Ax.format("order by '%s' %s", columnName,
					ascending ? "asc" : "desc");
		}
	}

	@ClientInstantiable
	public static class IdOrder<H extends HasId> extends SearchOrder<H, Long> {
		public IdOrder() {
		}

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

	public static class SpecificIdOrder<H extends HasId>
			extends SearchOrder<H, Integer> {
		private List<Long> sorted;

		public SpecificIdOrder() {
		}

		public SpecificIdOrder(Collection<Long> sorted) {
			this.sorted = sorted.stream().collect(Collectors.toList());
		}

		@Override
		public Integer apply(H t) {
			return sorted.indexOf(Long.valueOf(t.getId()));
		}
	}
}

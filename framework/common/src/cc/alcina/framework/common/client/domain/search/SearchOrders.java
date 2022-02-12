package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;

@Bean
@Introspectable
public class SearchOrders<T> implements Comparator<T>, Serializable,
		HasEquivalence<SearchOrders<T>>, TreeSerializable {
	/*
	 * Don't access directly - even when altering (call refreshSerializable when
	 * altering). The boolean value is true ascending; false descending
	 */
	private Map<SearchOrder<T, ?>, Boolean> cmps = new LinkedHashMap<>();

	private List<SerializableSearchOrder> serializableSearchOrders = new ArrayList<>();

	public SearchOrders() {
	}

	public SearchOrders<T> addEntityDescOrder() {
		addOrder(new IdOrder(), false);
		return this;
	}

	public SearchOrders<T> addEntityOrder() {
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

	public Optional<SearchOrder<T, ?>> getFirstOrder() {
		return _getCmps().keySet().stream().findFirst();
	}

	@PropertySerialization(defaultProperty = true, types = SerializableSearchOrder.class)
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

	public boolean provideIsAscending() {
		return cmps.values().stream().findFirst().orElse(true);
	}

	public String provideSearchOrderFieldName() {
		return cmps.keySet().stream()
				.filter(so -> so instanceof DisplaySearchOrder).findFirst()
				.map(so -> ((DisplaySearchOrder) so).getFieldName())
				.orElse(null);
	}

	public void putFirstOrder(SearchOrder order) {
		_getCmps().clear();
		this.serializableSearchOrders.clear();
		addOrder(order, true);
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
		return _getCmps().size() > 0
				&& _getCmps().keySet().iterator().next().equivalentTo(order);
	}

	public void toggleFirstOrder() {
		_getCmps().entrySet().stream().findFirst()
				.ifPresent(e -> e.setValue(!e.getValue()));
		refreshSerializable();
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
				!entry.getValue() ? "desc" : "asc");
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
						if (sso.getKey().contains(".")) {
							Class clazz = Reflections.forName(sso.getKey());
							return (SearchOrder) Reflections.newInstance(clazz);
						} else {
							DisplaySearchOrder displaySearchOrder = new DisplaySearchOrder();
							displaySearchOrder.setFieldName(sso.getKey());
							return displaySearchOrder;
						}
					}, sso -> sso.isAscending()));
		}
		return cmps;
	}

	@Bean
	public static class ColumnSearchOrder
			implements Serializable, TreeSerializable {
		private String columnName;

		private boolean ascending;

		public String getColumnName() {
			return columnName;
		}

		@PropertySerialization(path = "asc")
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

	@Bean
	@Introspectable
	public static class SerializableSearchOrder implements Serializable,
			HasReflectiveEquivalence<SerializableSearchOrder>,
			TreeSerializable {
		private String key;

		private boolean ascending;

		public SerializableSearchOrder() {
		}

		public SerializableSearchOrder(SearchOrder searchOrder,
				Boolean ascending) {
			setKey(searchOrder.provideKey());
			setAscending(ascending);
		}

		@PropertySerialization(defaultProperty = true)
		public String getKey() {
			return this.key;
		}

		@PropertySerialization(path = "asc")
		public boolean isAscending() {
			return this.ascending;
		}

		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}

		public void setKey(String key) {
			this.key = key;
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

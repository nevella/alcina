package cc.alcina.framework.gwt.client.cell;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.search.grouping.GroupedResult.Cell;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.RowKey;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRendererFunction;

public class GroupingMapper<V> {
	private GroupingClassifier<V, ? extends Comparable> columnClassifier;

	private GroupingClassifier<V, ? extends Comparable> rowClassifier;

	private GroupingClassifier<V, ? extends Comparable> sectionClassifier;

	private boolean totalRow;

	private boolean totalColumn;

	private Function<List<V>, Object> valueTotaller;

	private Function<Object, String> valueRenderer = new FriendlyEnumRendererFunction();

	public Function<Object, String> columnNameRenderer = new FriendlyEnumRendererFunction();

	public Function<Object, String> rowNameRenderer = new FriendlyEnumRendererFunction();

	public Function<Object, String> sectionNameRenderer = new FriendlyEnumRendererFunction();

	private List<? extends Comparable> columnKeys;

	public GroupingMapperResult apply(List<V> data) {
		Multimap<? extends Comparable, List<V>> byRow = data.stream()
				.collect(AlcinaCollectors.toKeyMultimap(rowClassifier));
		List<GroupingMapperRow<V>> rows = byRow.entrySet().stream()
				.map(GroupedTuple::new).sorted().map(GroupingMapperRow::new)
				.collect(Collectors.toList());
		if (totalRow) {
			rows.add(new GroupingMapperRow<>(GroupedTuple.<V> totalRow(data)));
		}
		GroupingMapperResult result = new GroupingMapperResult();
		result.rowModels = (Stream) rows.stream();
		this.columnKeys = columnClassifier.allKeys(data);
		rows.forEach(this::generateCells);
		result.columnMapper = new GroupingColumnMapper();
		return result;
	}

	public GroupingMapper<V> withColumnClassifier(
			GroupingClassifier<V, ? extends Comparable> columnClassifier) {
		this.columnClassifier = columnClassifier;
		return this;
	}

	public GroupingMapper<V> withRowClassifier(
			GroupingClassifier<V, ? extends Comparable> rowClassifier) {
		this.rowClassifier = rowClassifier;
		return this;
	}

	public GroupingMapper<V> withSectionClassifier(
			GroupingClassifier<V, ? extends Comparable> sectionClassifier) {
		this.sectionClassifier = sectionClassifier;
		return this;
	}

	public GroupingMapper<V> withTotalColumn() {
		this.totalColumn = true;
		return this;
	}

	public GroupingMapper<V> withTotalRow() {
		this.totalRow = true;
		return this;
	}

	public GroupingMapper<V>
			withValueRenderer(Function<Object, String> valueRenderer) {
		this.valueRenderer = valueRenderer;
		return this;
	}

	public GroupingMapper<V>
			withValueTotaller(Function<List<V>, Object> valueTotaller) {
		this.valueTotaller = valueTotaller;
		return this;
	}

	private String applyNameRenderer(Function<Object, String> nameRenderer,
			Comparable key) {
		if (key == null) {
			return "Total";
		}
		return nameRenderer.apply(key);
	}

	void generateCells(GroupingMapperRow<V> row) {
		List<V> values = row.tuple.values;
		Multimap<Comparable, List<V>> byColumn = values.stream()
				.collect(AlcinaCollectors.toKeyMultimap(columnClassifier));
		List<GroupedTuple<V>> columns = byColumn.entrySet().stream()
				.map(GroupedTuple::new).sorted().collect(Collectors.toList());
		{
			Cell cell = new Cell();
			cell.value = applyNameRenderer(rowNameRenderer, row.tuple.key);
			row.cells.add(cell);
		}
		if (totalColumn) {
			Cell cell = new Cell();
			cell.rawValue = valueTotaller.apply(values);
			cell.value = valueRenderer.apply(cell.rawValue);
			row.cells.add(cell);
		}
		for (Comparable columnKey : columnKeys) {
			Cell cell = new Cell();
			cell.rawValue = valueTotaller
					.apply(byColumn.getAndEnsure(columnKey));
			cell.value = valueRenderer.apply(cell.rawValue);
			row.cells.add(cell);
			// TODO - place/href function
		}
		if (sectionClassifier != null) {
			Comparable sectionKey = sectionClassifier.apply((V) row.in);
			row.section = applyNameRenderer(sectionNameRenderer, sectionKey);
		}
	}

	public static class GroupingMapperResult {
		public Stream<GroupingMapperRow> rowModels;

		public ColumnMapper<GroupingMapperRow> columnMapper;

		public Function<GroupingMapperRow, RowKey> keyMapper = gmr -> null;
	}

	public static class GroupingMapperRow<V> extends Row {
		private transient GroupedTuple<V> tuple;

		public GroupingMapperRow() {
		}

		public GroupingMapperRow(GroupedTuple<V> tuple) {
			this.tuple = tuple;
		}
	}

	static class GroupedTuple<V> implements Comparable {
		static <V> GroupedTuple<V> totalRow(List<V> values) {
			GroupedTuple<V> tuple = new GroupedTuple<>();
			tuple.values = values;
			return tuple;
		}

		Comparable key;

		List<V> values;

		public GroupedTuple() {
		}

		public GroupedTuple(Comparable key, List<V> values) {
			Preconditions.checkNotNull(key);
			this.key = key;
			this.values = values;
		}

		public GroupedTuple(Entry<? extends Comparable, List<V>> entry) {
			this(entry.getKey(), entry.getValue());
		}

		@Override
		public int compareTo(Object o) {
			return key.compareTo(((GroupedTuple) o).key);
		}
	}

	class GroupingColumnMapper extends ColumnMapper<GroupingMapperRow> {
		@Override
		protected void defineMappings() {
			int idx = 0;
			{
				int f_idx = idx++;
				builder.col("Key")
						.function(row -> ((Cell) row.cells.get(f_idx)).value)
						.add();
			}
			if (totalColumn) {
				int f_idx = idx++;
				builder.col("Total")
						.function(row -> ((Cell) row.cells.get(f_idx)).value)
						.numeric().add();
			}
			for (Comparable key : columnKeys) {
				int f_idx = idx++;
				builder.col(columnNameRenderer.apply(key))
						.function(row -> ((Cell) row.cells.get(f_idx)).value)
						.numeric().add();
			}
		}

		@Override
		protected Class<GroupingMapperRow> mappedClass() {
			return GroupingMapperRow.class;
		}
	}
}

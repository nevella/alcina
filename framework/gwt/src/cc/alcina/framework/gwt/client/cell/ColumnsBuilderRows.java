package cc.alcina.framework.gwt.client.cell;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Cell;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Col;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.RowKey;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ColumnsBuilderRows {
	public BiConsumer<ColumnsBuilder<Row>.ColumnBuilder, Integer>
			additionalMapper() {
		return new AdditionalMapper();
	}

	public <V> GroupedResult toGroupedResult(Stream<V> rowModels,
			ColumnMapper<V> columnMapper, String name,
			Function<V, RowKey> keyMapper) {
		GroupedResult groupedResult = new GroupedResult();
		List<ColumnsBuilder<V>.ColumnBuilder> mappings = columnMapper
				.getMappings();
		mappings.stream().map(cm -> new Col().withName(cm.getName()))
				.forEach(groupedResult.getCols()::add);
		List<V> list = rowModels.collect(Collectors.toList());
		list.stream().map(rowModel -> {
			Row<V> resultRow = new Row<>(rowModel);
			resultRow.key = keyMapper.apply(rowModel);
			mappings.stream().forEach(cm -> {
				Cell cell = new Cell();
				resultRow.cells.add(cell);
				mapValue(cm, cell, rowModel);
			});
			return resultRow;
		}).forEach(groupedResult.getRows()::add);
		List<ColumnsBuilder<List<V>>.ColumnBuilder> totalMappings = columnMapper
				.getTotalMappings();
		if (totalMappings.size() > 0) {
			Map<String, ColumnsBuilder<List<V>>.ColumnBuilder> totalBuildersByName = totalMappings
					.stream()
					.collect(AlcinaCollectors.toKeyMap(tm -> tm.getName()));
			Row<V> totalRow = new Row<>(null);
			groupedResult.setTotalRow(totalRow);
			groupedResult.getRows().add(totalRow);
			for (ColumnsBuilder<V>.ColumnBuilder mapping : mappings) {
				Cell cell = new Cell();
				totalRow.cells.add(cell);
				if (totalBuildersByName.containsKey(mapping.getName())) {
					mapValue(totalBuildersByName.get(mapping.getName()), cell,
							list);
				} else {
					cell.value = "";
				}
			}
		}
		groupedResult.name = name;
		return groupedResult;
	}

	private <T> void mapValue(ColumnsBuilder<T>.ColumnBuilder cm, Cell cell,
			T rowModel) {
		cell.value = CommonUtils
				.nullSafeToString(cm.provideValueFunction().apply(rowModel));
		if (cm.getTitleFunction() != null) {
			cell.title = cm.getTitleFunction().apply(rowModel);
		}
		if (cm.getHrefFunction() != null) {
			cell.href = cm.getHrefFunction().apply(rowModel);
		}
	}

	static class AdditionalMapper
			implements BiConsumer<ColumnsBuilder<Row>.ColumnBuilder, Integer> {
		@Override
		public void accept(ColumnsBuilder<Row>.ColumnBuilder builder,
				Integer idx) {
			builder.function(row -> ((Cell) row.cells.get(idx)).value);
			builder.titleFunction(row -> ((Cell) row.cells.get(idx)).title);
		}
	}
}

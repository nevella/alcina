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
import cc.alcina.framework.common.client.search.grouping.GroupedResult.GroupKey;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.group.GroupingMapper.GroupingMapperResult;
import cc.alcina.framework.gwt.client.group.GroupingMapper.GroupingMapperRow;

public class ColumnsBuilderRows {
	public BiConsumer<ColumnsBuilder<Row>.ColumnBuilder, Integer>
			additionalMapper() {
		return new AdditionalMapper();
	}

	public GroupedResult toGroupedResult(GroupingMapperResult mapperResult,
			String name) {
		return toGroupedResult(mapperResult.rowModels,
				mapperResult.columnMapper, name, mapperResult.keyMapper);
	}

	public <V> GroupedResult toGroupedResult(Stream<V> rowModels,
			ColumnMapper<V> columnMapper, String name,
			Function<V, GroupKey> keyMapper) {
		return toGroupedResult(rowModels, columnMapper, name, keyMapper, false);
	}

	public <V> GroupedResult toGroupedResult(Stream<V> rowModels,
			ColumnMapper<V> columnMapper, String name,
			Function<V, GroupKey> keyMapper, boolean incomingTotalRow) {
		GroupedResult groupedResult = new GroupedResult();
		List<ColumnsBuilder<V>.ColumnBuilder> mappings = columnMapper
				.getMappings();
		mappings.stream()
				.map(cm -> new Col().withName(cm.getName())
						.withStyle(cm.getStyle())
						.withWidth(cm.getWidth(), cm.getUnit()))
				.forEach(groupedResult.getCols()::add);
		List<V> list = rowModels.collect(Collectors.toList());
		list.stream().map(rowModel -> {
			Row resultRow = new Row(rowModel);
			resultRow.key = keyMapper.apply(rowModel);
			int idx = 0;
			for (ColumnsBuilder<V>.ColumnBuilder cm : mappings) {
				Cell cell = new Cell();
				resultRow.cells.add(cell);
				mapValue(cm, cell, rowModel);
				// hacky - but we don't use a numericvaluemapper anywhere else
				if (rowModel instanceof GroupingMapperRow) {
					GroupingMapperRow typed = (GroupingMapperRow) rowModel;
					cell.numericValue = typed.cells.get(idx).numericValue;
				}
				idx++;
			}
			return resultRow;
		}).forEach(groupedResult.getRows()::add);
		List<ColumnsBuilder<List<V>>.ColumnBuilder> totalMappings = columnMapper
				.getTotalMappings();
		if (totalMappings.size() > 0) {
			Map<String, ColumnsBuilder<List<V>>.ColumnBuilder> totalBuildersByName = totalMappings
					.stream()
					.collect(AlcinaCollectors.toKeyMap(tm -> tm.getName()));
			Row totalRow = new Row(null);
			groupedResult.setTotalRow(totalRow);
			groupedResult.getRows().add(totalRow);
			int idx = 0;
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
		if (incomingTotalRow) {
			groupedResult.setTotalRow(
					(Row) CommonUtils.last(groupedResult.getRows()));
		}
		groupedResult.name = name;
		return groupedResult;
	}

	private <T> void mapValue(ColumnsBuilder<T>.ColumnBuilder cm, Cell cell,
			T rowModel) {
		Object value = cm.provideValueFunction().apply(rowModel);
		cell.rawValue = value;
		if (value instanceof Number) {
			cell.numericValue = ((Number) value).doubleValue();
		}
		cell.value = CommonUtils.nullSafeToString(value);
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
			// builder.titleFunction(row -> ((Cell) row.cells.get(idx)).title);
			builder.href(row -> ((Cell) row.cells.get(idx)).href);
		}
	}
}

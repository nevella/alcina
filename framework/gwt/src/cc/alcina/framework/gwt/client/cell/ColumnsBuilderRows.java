package cc.alcina.framework.gwt.client.cell;

import java.util.Arrays;
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
import cc.alcina.framework.common.client.util.ColumnMapper.RowModel_SingleCell;
import cc.alcina.framework.common.client.util.ColumnMapper.SingleCellColumnMapper;
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
						.withStyle(cm.getStyle()).withNumeric(cm.isNumeric())
						.withWidth(cm.getWidth(), cm.getUnit()))
				.forEach(groupedResult.getCols()::add);
		List<V> list = rowModels.collect(Collectors.toList());
		list.stream().map(rowModel -> {
			Row resultRow = new Row(rowModel);
			resultRow.setKey(keyMapper.apply(rowModel));
			int idx = 0;
			for (ColumnsBuilder<V>.ColumnBuilder cm : mappings) {
				Cell cell = new Cell();
				resultRow.getCells().add(cell);
				mapValue(cm, cell, rowModel);
				// hacky - but we don't use a numericvaluemapper anywhere else
				if (rowModel instanceof GroupingMapperRow) {
					GroupingMapperRow typed = (GroupingMapperRow) rowModel;
					cell.setNumericValue(
							typed.getCells().get(idx).getNumericValue());
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
				totalRow.getCells().add(cell);
				if (totalBuildersByName.containsKey(mapping.getName())) {
					mapValue(totalBuildersByName.get(mapping.getName()), cell,
							list);
				} else {
					cell.setValue("");
				}
			}
		}
		if (incomingTotalRow) {
			groupedResult.setTotalRow(
					(Row) CommonUtils.last(groupedResult.getRows()));
		}
		groupedResult.setName(name);
		return groupedResult;
	}

	public GroupedResult toSingleCellGroupedResult(String html, String name) {
		SingleCellColumnMapper mapper = new SingleCellColumnMapper();
		List<RowModel_SingleCell> model = Arrays
				.asList(new RowModel_SingleCell(html));
		return new ColumnsBuilderRows().toGroupedResult(model.stream(), mapper,
				name, row -> row.asRowKey());
	}

	private <T> void mapValue(ColumnsBuilder<T>.ColumnBuilder cm, Cell cell,
			T rowModel) {
		Object value = cm.provideValueFunction().apply(rowModel);
		cell.rawValue = value;
		if (value instanceof Number) {
			cell.setNumericValue(((Number) value).doubleValue());
		}
		cell.setValue(CommonUtils.nullSafeToString(value));
		if (cm.getTitleFunction() != null) {
			cell.setTitle(cm.getTitleFunction().apply(rowModel));
		}
		if (cm.getHrefFunction() != null) {
			cell.setHref(cm.getHrefFunction().apply(rowModel));
		}
	}

	static class AdditionalMapper
			implements BiConsumer<ColumnsBuilder<Row>.ColumnBuilder, Integer> {
		@Override
		public void accept(ColumnsBuilder<Row>.ColumnBuilder builder,
				Integer idx) {
			builder.function(
					row -> ((Cell) row.getCells().get(idx)).getValue());
			// builder.titleFunction(row -> ((Cell) row.cells.get(idx)).title);
			builder.href(row -> ((Cell) row.getCells().get(idx)).getHref());
		}
	}
}

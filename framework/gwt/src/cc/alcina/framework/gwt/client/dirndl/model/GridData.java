package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagText;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.ColumnName;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.ColumnsWidth;

@Directed.Transform(GridData.Table.class)
public class GridData extends Model.All {
	public static class Row extends Model.All {
		public String caption = "";

		public String className;

		public List<String> data = new ArrayList<>();

		List<String> toStringList() {
			List<String> result = new ArrayList<>();
			result.add(caption);
			result.addAll(data);
			return result;
		}
	}

	public Row header = new Row();

	public List<Row> rows = new ArrayList<>();

	@Directed.Exclude
	public boolean firstColumnFixedWidth;

	/*
	 * Constructs a multi-column table from a list of reflected objects. Column
	 * headers are either supplied or derived from fields
	 */
	public static class Table extends
			AbstractContextSensitiveModelTransform<GridData, Table.IntermediateModel> {
		@Override
		public IntermediateModel apply(GridData t) {
			return new IntermediateModel(t);
		}

		@Directed
		class IntermediateModel extends Model.All
				implements Directed.NonClassTag {
			@Binding(type = Type.STYLE_ATTRIBUTE)
			String gridTemplateColumns;

			@Directed.Wrap("column-names")
			List<Tables.ColumnName> columnNames;

			List<RowData> rows;

			@Directed(tag = "row")
			class RowData extends Model.All {
				@Binding(type = Type.CLASS_PROPERTY)
				String className;

				@Directed
				List<TagText> cells;

				RowData(Row row) {
					className = row.className;
					cells = row.toStringList().stream()
							.map(s -> new TagText("cell", s, s))
							.collect(Collectors.toList());
				}
			}

			IntermediateModel(GridData data) {
				this.rows = data.rows.stream().map(RowData::new)
						.collect(Collectors.toList());
				columnNames = data.header.toStringList().stream()
						.map(ColumnName::new).collect(Collectors.toList());
				ColumnsWidth columnsWidth = node.annotation(ColumnsWidth.class);
				gridTemplateColumns = columnNames.stream()
						.map(n -> data.firstColumnFixedWidth
								&& Objects.equals(n, columnNames.get(0))
										? "min-content"
										: "auto")
						.collect(Collectors.joining(" "));
			}
		}
	}

	public List<List<String>> toLists() {
		return Stream
				.concat(Stream.of(header.toStringList()),
						rows.stream().map(Row::toStringList))
				.collect(Collectors.toList());
	}
}

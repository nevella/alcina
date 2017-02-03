package cc.alcina.framework.gwt.client.cell;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;

import cc.alcina.framework.common.client.util.CommonUtils;

public class ColumnsBuilder<T> {
	private AbstractCellTable<T> table;

	@SuppressWarnings("unused")
	private Class<T> clazz;

	private List<String> columnsFilter = null;

	private Header<String> footer;

	private boolean edit;

	public ColumnsBuilder(AbstractCellTable<T> table, Class<T> clazz) {
		this.table = table;
		this.clazz = clazz;
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	}

	public ColumnBuilder col(Enum enumValue) {
		return new ColumnBuilder(CommonUtils.friendlyConstant(enumValue));
	}

	public ColumnBuilder col(String name) {
		return new ColumnBuilder(name);
	}

	public ColumnsBuilder columnsFilter(Collection validColumns) {
		columnsFilter = (List<String>) validColumns.stream()
				.map(o -> o.toString().replace("_", " ").toLowerCase())
				.collect(Collectors.toList());
		return this;
	}

	public ColumnsBuilder editable(boolean edit) {
		this.edit = edit;
		return this;
	}

	public ColumnsBuilder footer(Header<String> footer) {
		this.footer = footer;
		return this;
	}

	public class ColumnBuilder {
		private String name;

		private boolean sortable;

		private Function<T, Object> function;

		private Function<T, Comparable> sortFunction;

		private boolean reversed;

		private double width;

		private Unit unit;

		private String style;

		private DirectedComparator nativeComparator;

		private Function<T, String> styleFunction;

		private String editablePropertyName;

		private FieldUpdater fieldUpdater;

		private Cell editableCell;

		private boolean numeric;

		private Cell cell;

		public ColumnBuilder(String name) {
			this.name = name;
		}

		public SortableColumn<T> build() {
			EditInfo editInfo = new EditInfo();
			editInfo.propertyName = editablePropertyName;
			if (edit && editablePropertyName != null) {
				editInfo.cell = Optional.ofNullable(editableCell)
						.orElse(new PropertyTextCell());
				editInfo.fieldUpdater = fieldUpdater != null ? fieldUpdater
						: new PropertyFieldUpdater(editablePropertyName);
			}
			if (function == null) {
				function = (Function) sortFunction;
			}
			SortableColumn<T> col = new SortableColumn<T>(function,
					sortFunction, nativeComparator, styleFunction, editInfo,
					cell);
			// don't add if filtered
			if (columnsFilter == null
					|| columnsFilter.contains(name.toLowerCase())) {
				if (footer == null) {
					table.addColumn(col, name);
				} else {
					Header<String> header = new Header<String>(new TextCell()) {
						@Override
						public String getValue() {
							return name;
						}

						@Override
						public String getHeaderStyleNames() {
							if (numeric) {
								return "numeric";
							}
							return super.getHeaderStyleNames();
						}
					};
					table.addColumn(col, header, footer);
				}
			}
			if (width != 0) {
				table.setColumnWidth(col, width, unit);
			}
			if (style != null) {
				col.setCellStyleNames(style);
			}
			col.setSortable(sortable);
			col.setDefaultSortAscending(!reversed);
			return col;
		}

		public ColumnBuilder cell(Cell cell) {
			this.cell = cell;
			return this;
		}

		public ColumnBuilder editableCell(Cell editableCell) {
			this.editableCell = editableCell;
			return this;
		}

		public ColumnBuilder editableProperty(String editablePropertyName) {
			this.editablePropertyName = editablePropertyName;
			return this;
		}

		public ColumnBuilder fieldUpdater(FieldUpdater fieldUpdater) {
			this.fieldUpdater = fieldUpdater;
			return this;
		}

		public ColumnBuilder function(Function<T, ?> function) {
			this.function = (Function<T, Object>) function;
			return this;
		}

		public ColumnBuilder
				nativeComparator(DirectedComparator nativeComparator) {
			this.nativeComparator = nativeComparator;
			return this;
		}

		public ColumnBuilder numeric() {
			numeric = true;
			return this.style("numeric");
		}

		public ColumnBuilder reversed() {
			this.reversed = true;
			return this;
		}

		public ColumnBuilder
				sortFunction(Function<T, ? extends Comparable> sortFunction) {
			this.sortFunction = (Function<T, Comparable>) sortFunction;
			sortable = true;
			return this;
		}

		public ColumnBuilder style(String style) {
			this.style = style;
			return this;
		}

		public ColumnBuilder styleFunction(Function<T, String> styleFunction) {
			this.styleFunction = styleFunction;
			return this;
		}

		public ColumnBuilder width(double width, Unit unit) {
			this.width = width;
			this.unit = unit;
			return this;
		}

		public ColumnBuilder noWrap() {
			style("nowrap");
			return this;
		}
	}

	public static class SortableColumn<T> extends Column<T, Object> {
		private Function<T, Comparable> sortFunction;

		private Function<T, Object> function;

		private Function<T, String> styleFunction;

		private DirectedComparator nativeComparator;

		private EditInfo editInfo;

		private Cell cell;

		public SortableColumn(Function<T, Object> function,
				Function<T, Comparable> sortFunction,
				DirectedComparator nativeComparator,
				Function<T, String> styleFunction, EditInfo editInfo,
				Cell cell) {
			super(cell != null ? cell : editInfo.cell);
			this.function = function;
			this.sortFunction = sortFunction;
			this.nativeComparator = nativeComparator;
			this.styleFunction = styleFunction;
			this.editInfo = editInfo;
			this.cell = cell;
			if (editInfo.fieldUpdater != null) {
				setFieldUpdater(editInfo.fieldUpdater);
			}
		}

		@Override
		public String getCellStyleNames(Context context, T object) {
			String editable = editInfo.isEditable() ? " editable" : "";
			if (styleFunction != null) {
				String custom = styleFunction.apply(object);
				if (custom != null) {
					String superStyles = super.getCellStyleNames(context,
							object);
					if (CommonUtils.isNullOrEmpty(superStyles)) {
						return custom + editable;
					} else {
						return superStyles + " " + custom + editable;
					}
				}
			}
			return super.getCellStyleNames(context, object) + editable;
		}

		public DirectedComparator getNativeComparator() {
			return this.nativeComparator;
		}

		@Override
		public Object getValue(T t) {
			Object value = function.apply(t);
			if (cell == null && editInfo.cell.getClass() == TextCell.class) {
				value = value == null ? null : value.toString();
			}
			return value;
		}

		public Function<T, Comparable> sortFunction() {
			return sortFunction != null ? sortFunction : (Function) function;
		}
	}

	static class EditInfo {
		public FieldUpdater fieldUpdater;

		public String propertyName;

		public Cell cell = new TextCell();

		public boolean isEditable() {
			return propertyName != null;
		}
	}
}
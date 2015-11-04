package cc.alcina.framework.gwt.client.cell;

import java.util.function.Function;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;

public class ColumnsBuilder<T> {
	private AbstractCellTable<T> table;

	@SuppressWarnings("unused")
	private Class<T> clazz;

	public ColumnsBuilder(AbstractCellTable<T> table, Class<T> clazz) {
		this.table = table;
		this.clazz = clazz;
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	}

	public ColumnBuilder col(String name) {
		return new ColumnBuilder(name);
	}

	private Header<String> footer;

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

		public ColumnBuilder(String name) {
			this.name = name;
		}

		public TextColumn<T> build() {
			SortableTextColumn<T> col = new SortableTextColumn<T>(function,
					sortFunction, nativeComparator, styleFunction);
			if (footer == null) {
				table.addColumn(col, name);
			} else {
				Header<String> header = new Header<String>(new TextCell()) {
					@Override
					public String getValue() {
						return name;
					}
				};
				table.addColumn(col, header, footer);
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

		public ColumnBuilder function(Function<T, Object> function) {
			this.function = function;
			return this;
		}

		public ColumnBuilder reversed() {
			this.reversed = true;
			return this;
		}

		public ColumnBuilder sortable() {
			this.sortable = true;
			return this;
		}

		public ColumnBuilder sortFunction(Function<T, Comparable> sortFunction) {
			this.sortFunction = sortFunction;
			return sortable();
		}

		public ColumnBuilder styleFunction(Function<T, String> styleFunction) {
			this.styleFunction = styleFunction;
			return this;
		}

		public ColumnBuilder nativeComparator(
				DirectedComparator nativeComparator) {
			this.nativeComparator = nativeComparator;
			return this;
		}

		public ColumnBuilder width(double width, Unit unit) {
			this.width = width;
			this.unit = unit;
			return this;
		}

		public ColumnBuilder style(String style) {
			this.style = style;
			return this;
		}
	}

	public static class SortableTextColumn<T> extends TextColumn<T> {
		private Function<T, Comparable> sortFunction;

		private Function<T, Object> function;

		private Function<T, String> styleFunction;

		private DirectedComparator nativeComparator;

		public DirectedComparator getNativeComparator() {
			return this.nativeComparator;
		}

		public SortableTextColumn(Function<T, Object> function,
				Function<T, Comparable> sortFunction,
				DirectedComparator nativeComparator,
				Function<T, String> styleFunction) {
			this.function = function;
			this.sortFunction = sortFunction;
			this.nativeComparator = nativeComparator;
			this.styleFunction = styleFunction;
		}

		@Override
		public String getValue(T t) {
			Object value = function.apply(t);
			return value == null ? null : value.toString();
		}

		public Function<T, Comparable> sortFunction() {
			return sortFunction != null ? sortFunction : (Function) function;
		}

		@Override
		public String getCellStyleNames(Context context, T object) {
			if (styleFunction != null) {
				String custom = styleFunction.apply(object);
				if (custom != null) {
					String superStyles = super.getCellStyleNames(context,
							object);
					if (CommonUtils.isNullOrEmpty(superStyles)) {
						return custom;
					} else {
						return superStyles + " " + custom;
					}
				}
			}
			return super.getCellStyleNames(context, object);
		}
	}
}
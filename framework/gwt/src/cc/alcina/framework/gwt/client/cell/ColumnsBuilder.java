package cc.alcina.framework.gwt.client.cell;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.common.client.util.ColumnMapper.ColumnMapping;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.BoundWidgetProviderTextBox;
import cc.alcina.framework.gwt.client.gwittir.customiser.DomainObjectSuggestCustomiser;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox.DateBoxProvider;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;

public class ColumnsBuilder<T> {
	private AbstractCellTable<T> table;

	private Class<T> clazz;

	private List<String> columnsFilter = null;

	private Header<String> footer;

	private boolean edit;

	private Map<SortableColumn, ColumnBuilder> built = new LinkedHashMap<>();

	private ColumnTotaller<T> totaller;

	public ColumnsBuilder(AbstractCellTable<T> table, Class<T> clazz) {
		this.table = table;
		this.clazz = clazz;
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	}

	public ColumnBuilder col(Enum enumValue) {
		return new ColumnBuilder(enumValue,
				HasDisplayName.displayName(enumValue));
	}

	public ColumnBuilder col(String name) {
		return new ColumnBuilder(name);
	}

	public Column colFor(Enum name) {
		return built.entrySet().stream()
				.filter(e -> e.getValue().enumKey == name).findFirst()
				.map(e -> e.getKey()).get();
	}

	public ColumnsBuilder columnsFilter(Collection validColumns) {
		columnsFilter = (List<String>) validColumns
				.stream().map(o -> HasDisplayName.displayName(o)
						.replace("_", " ").toLowerCase())
				.collect(Collectors.toList());
		return this;
	}

	public ColumnsBuilder columnTotaller(ColumnTotaller<T> totaller) {
		this.totaller = totaller;
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

	public Comparator<T> getComparator(Column<?, ?> column) {
		ColumnBuilder columnBuilder = built.get(column);
		return Comparator.comparing(columnBuilder.sortFunction);
	}

	public ColumnTotaller<T> getTotaller() {
		return this.totaller;
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

		private Enum enumKey;

		private Function<T, Place> placeFunction;

		private boolean asUnsafeHtml;

		public ColumnBuilder(Enum enumValue, String displayName) {
			this(displayName);
			this.enumKey = enumValue;
		}

		public ColumnBuilder(String name) {
			this.name = name;
		}

		public SortableColumn<T> build() {
			EditInfo editInfo = new EditInfo();
			editInfo.propertyName = editablePropertyName;
			if (edit && editablePropertyName != null) {
				setupEditInfo(editInfo);
			}
			if (function == null) {
				function = (Function) sortFunction;
			}
			if (placeFunction != null) {
				cell = new PlaceLinkCell();
			}
			if (asUnsafeHtml) {
				cell = new UnsafeHtmlCell();
				if (style == null) {
					style = "pre";
				}
			}
			SortableColumn<T> col = new SortableColumn<T>(function,
					sortFunction, placeFunction, nativeComparator,
					styleFunction, editInfo, cell, name, ColumnsBuilder.this);
			built.put(col, this);
			// don't add if filtered
			if (columnsFilter == null
					|| columnsFilter.contains(name.toLowerCase())) {
				if (footer == null) {
					table.addColumn(col, name);
				} else {
					Header<String> header = new Header<String>(new TextCell()) {
						@Override
						public String getHeaderStyleNames() {
							if (numeric) {
								return "numeric";
							}
							return super.getHeaderStyleNames();
						}

						@Override
						public String getValue() {
							return name;
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

		protected void setupEditInfo(EditInfo editInfo) {
			Field field = null;
			if (editableCell != null) {
				editInfo.cell = editableCell;
			} else {
				field = GwittirBridge.get().getField(clazz,
						editInfo.propertyName, true, true,
						GwittirBridge.SIMPLE_FACTORY_NO_NULLS, null);
				BoundWidgetProvider cellProvider = field.getCellProvider();
				if (cellProvider instanceof ListBoxEnumProvider) {
					ListBoxEnumProvider listBoxEnumProvider = (ListBoxEnumProvider) cellProvider;
					Class<? extends Enum> enumClass = listBoxEnumProvider
							.getEnumClass();
					Renderer renderer = listBoxEnumProvider.getRenderer();
					editInfo.cell = new PropertySingleSelectorCell(enumClass,
							renderer, new FlatSearchSelector(enumClass, 1,
									renderer, new EnumSupplier(enumClass)));
				} else if (cellProvider instanceof DomainObjectSuggestCustomiser) {
					DomainObjectSuggestCustomiser suggestCustomiser = (DomainObjectSuggestCustomiser) cellProvider;
					Renderer renderer = suggestCustomiser.getRenderer();
					editInfo.cell = new PropertyDomainSuggestCell(renderer,
							(BoundSuggestBox) suggestCustomiser.get());
				} else if (cellProvider instanceof DateBoxProvider) {
					editInfo.cell = new PropertyDateCell();
				} else if (cellProvider instanceof BoundWidgetProviderTextBox) {
					editInfo.cell = new PropertyTextCell();
				} else {
					throw new UnsupportedOperationException();
				}
			}
			editInfo.fieldUpdater = fieldUpdater != null ? fieldUpdater
					: new PropertyFieldUpdater(editablePropertyName, field);
			function = new PropertyFieldGetter(editablePropertyName, clazz);
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

		public ColumnBuilder noWrap() {
			style("nowrap");
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

		public ColumnBuilder place(Function<T, Place> placeFunction) {
			this.placeFunction = placeFunction;
			return this;
		}

		public ColumnBuilder asUnsafeHtml(boolean asUnsafeHtml) {
			this.asUnsafeHtml = asUnsafeHtml;
			return this;
		}
	}

	public interface ColumnTotaller<T> {
		List<T> getList();

		Object getTotalValue(String columnName);

		boolean isTotalRow(T t);
	}

	public static class SortableColumn<T> extends Column<T, Object> {
		private Function<T, Comparable> sortFunction;

		private Function<T, Object> function;

		private Function<T, String> styleFunction;

		private DirectedComparator nativeComparator;

		private EditInfo editInfo;

		private Cell cell;

		private String name;

		private ColumnsBuilder columnsBuilder;

		private Function<T, Place> placeFunction;

		public SortableColumn(Function<T, Object> function,
				Function<T, Comparable> sortFunction,
				Function<T, Place> placeFunction,
				DirectedComparator nativeComparator,
				Function<T, String> styleFunction, EditInfo editInfo, Cell cell,
				String name, ColumnsBuilder columnsBuilder) {
			super(cell != null ? cell : editInfo.cell);
			this.function = function;
			this.sortFunction = sortFunction;
			this.placeFunction = placeFunction;
			this.nativeComparator = nativeComparator;
			this.styleFunction = styleFunction;
			this.editInfo = editInfo;
			this.cell = cell;
			this.name = name;
			this.columnsBuilder = columnsBuilder;
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
			try {
				Object value = null;
				if (columnsBuilder.totaller != null) {
					if (columnsBuilder.totaller.isTotalRow(t)) {
						value = columnsBuilder.totaller.getTotalValue(name);
					} else {
						value = function.apply(t);
					}
				} else {
					value = function.apply(t);
				}
				if (cell == null && (editInfo.cell.getClass() == TextCell.class
						|| editInfo.cell
								.getClass() == PropertyTextCell.class)) {
					value = CommonUtils.nullSafeToString(value);
				}
				if (placeFunction != null) {
					TextPlaceTuple tuple = new TextPlaceTuple();
					tuple.text = (String) value;
					tuple.place = placeFunction.apply(t);
					value = tuple;
				}
				return value;
			} catch (Exception e) {
				String toString = null;
				try {
					toString = t.toString();
				} catch (Exception e1) {
					toString = "(exception generating string)";
					e1.printStackTrace();
				}
				throw new RuntimeException(
						Ax.format("Exception getting column value - %s - %s",
								name, toString),
						e);
			}
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

	public void buildFromColumnMappings(ColumnMapper<T> mapper) {
		List<ColumnMapper<T>.ColumnMapping> mappings = mapper.getMappings();
		for (ColumnMapping columnMapping : mappings) {
			col(columnMapping.name).function(columnMapping.mapping)
					.asUnsafeHtml(columnMapping.asHtml).build();
		}
	}
}
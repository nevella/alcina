package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.DisplaySearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ModalResolver;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedBindableSearchActivity;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedCategoriesActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.ContextSensitiveTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnMetadata.ColumnMetadata;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.CellClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowsModelAttached;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.SortTable;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.DirectedEntitySearchActivityTransformer.TableContainer;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn.CustomOrder;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.customiser.ModelPlaceCustomiser;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

/**
 * <p>
 * This class, and nested classes, are intended to be accompanied by a context
 * resolver which adds tag data to the models (header, row, cell, table), and
 * are intended for use as transformations of CRUD entity/category activities
 * rather than more general 'arbitrary collection to table' transforms.
 * 
 * <p>
 * Possibly the 'rendering' ContextResolver isn't the correct choice, instead
 * each UI model instance could be generated by ContextResolver.impl(), with the
 * impl subclassing a base impl. That would handle the need to sometimes
 * customise behaviour, as well as styling. Anyways...for the moment, behaviour
 * changes basically happen in the base (i.e. per-application/theme resolver)
 *
 * <p>
 * The arbitrary case doesn't really need a supporting model - see
 * {@link Tables}
 *
 * FIXME - dirndl - doc) this example of using transforms
 * 
 * <p>
 * Because of descendant events, it'd be fairly easy to add a selectionmodel (a
 * la gwt celltable) ... in fact, see RowsModel below
 *
 * <p>
 * There's also partial support for column filtering/sorting (working - e.g. in
 * the EntityBrowser, but sorting/filtering can relate to larger application
 * attributes such as the url doesn't have many examples)
 *
 */
public class TableModel extends Model
		implements NodeEditorContext, TableEvents.CellClicked.Handler {
	public static class DirectedCategoriesActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedCategoriesActivity<?>, TableModel> {
		@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE))
		public static class CategoryNamePlaceTableAdapter extends Model
				implements HasDisplayName {
			private CategoryNamePlace place;

			public CategoryNamePlaceTableAdapter() {
			}

			public CategoryNamePlaceTableAdapter(CategoryNamePlace place) {
				this.place = place;
			}

			@Override
			public String displayName() {
				return place.ensureAction().getDisplayName();
			}

			@Display(orderingHint = 20)
			public String getDescription() {
				return Objects.equals(place.ensureAction().getDisplayName(),
						place.ensureAction().getDescription()) ? ""
								: place.ensureAction().getDescription();
			}

			@Display(name = "Name", orderingHint = 10)
			@Custom(customiserClass = ModelPlaceCustomiser.class)
			public CategoryNamePlace getPlace() {
				return place;
			}
		}

		@Override
		public TableModel apply(DirectedCategoriesActivity<?> activity) {
			TableModel tableModel = new TableModel();
			ModalResolver resolver = ModalResolver.multiple(node, true);
			node.setResolver(resolver);
			resolver.setTableModel(tableModel);
			List<CategoryNamePlace> places = activity.getPlace()
					.getNamedPlaces();
			places.removeIf(p -> !isPermitted(p));
			Class<? extends Bindable> resultClass = CategoryNamePlaceTableAdapter.class;
			BeanFields.query().forClass(resultClass)
					.forMultipleWidgetContainer(true)
					.withResolver(node.getResolver()).listFields().stream()
					.map(TableColumn::new)
					.forEach(tableModel.header.columns::add);
			places.stream().map(CategoryNamePlaceTableAdapter::new).map(
					bindable -> new TableModel.TableRow(tableModel, bindable))
					.forEach(tableModel::addRow);
			return tableModel;
		}

		protected boolean isPermitted(CategoryNamePlace place) {
			return true;
		}
	}

	/**
	 * Used by treetable to construct a TableModel from a generic Bindable class
	 * 
	 */
	public static class BindableClassTransformer extends
			AbstractContextSensitiveModelTransform<Class<? extends Bindable>, TableModel> {
		@Override
		public TableModel apply(Class<? extends Bindable> clazz) {
			TableModel tableModel = new TableModel();
			BeanFields.query().forClass(clazz).forMultipleWidgetContainer(true)
					.withResolver(node.getResolver()).listFields().stream()
					.map(TableColumn::new)
					.forEach(tableModel.header.columns::add);
			tableModel.editableFields = BeanFields.query().forClass(clazz)
					.forMultipleWidgetContainer(true)
					.withResolver(node.getResolver()).withEditable(true)
					.withValidationFeedbackProvider(
							new FormModel.ValidationFeedbackProvider())
					.listFields().stream()
					.collect(AlcinaCollectors.toKeyMap(Field::getProperty));
			/*
			 * do not populate rows, to handle a variable row set
			 */
			return tableModel;
		}
	}

	public static class DirectedEntitySearchActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable>, TableContainer> {
		@Directed.Delegating
		public class TableContainer extends Model.All
				implements TableEvents.SortTable.Handler {
			TableModel tableModel;

			TableContainer(TableModel tableModel) {
				this.tableModel = tableModel;
			}

			@Override
			public void onSortTable(SortTable event) {
				TableColumn column = event.getModel();
				Place rawPlace = Client.currentPlace();
				if (!(rawPlace instanceof BindablePlace)) {
					return;
				}
				BindablePlace<?> place = Client.currentPlace();
				place = place.copy();
				DisplaySearchOrder order = new DisplaySearchOrder();
				order.setFieldName(column.getField().getPropertyName());
				SearchOrders searchOrders = place.def.getSearchOrders();
				Optional<SearchOrder> firstOrder = searchOrders.getFirstOrder();
				if (firstOrder.isPresent()
						&& firstOrder.get().equivalentTo(order)) {
					searchOrders.toggleFirstOrder();
				} else {
					searchOrders.putFirstOrder(order);
				}
				place.go();
			}
		}

		@Override
		public TableContainer apply(
				DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable> activity) {
			TableModel tableModel = new TableModel();
			TableContainer tableContainer = new TableContainer(tableModel);
			if (activity.getSearchResults() == null) {
				return tableContainer;
			}
			ModalResolver resolver = tableModel.init(node);
			BindableSearchDefinition def = activity.getSearchResults().getDef();
			String sortFieldName = def.getSearchOrders()
					.provideSearchOrderFieldName();
			SortDirection sortDirection = def.getSearchOrders()
					.provideIsAscending() ? SortDirection.ASCENDING
							: SortDirection.DESCENDING;
			Class<? extends Bindable> resultClass = activity.getSearchResults()
					.resultClass();
			List<Field> fields = BeanFields.query().forClass(resultClass)
					.forMultipleWidgetContainer(true).withResolver(resolver)
					.listFields();
			fields.stream().map(field -> {
				SortDirection fieldDirection = field.getPropertyName()
						.equals(sortFieldName) ? sortDirection : null;
				return new TableColumn(field, fieldDirection);
			}).forEach(tableModel.header.columns::add);
			List<? extends Bindable> rowObjects = activity.getSearchResults()
					.getQueriedResultObjects();
			if (rowObjects.size() == 0) {
				Registry.impl(EmptyResultHandler.class)
						.getEmptyResultPlaceholder(fields)
						.forEach(tableModel::addRow);
			} else {
				rowObjects.stream()
						.map(bindable -> new TableModel.TableRow(tableModel,
								bindable))
						.forEach(tableModel::addRow);
			}
			// add actions if editable and adjunct
			return tableContainer;
		}
	}

	/**
	 * Define a table row transformer
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface RowTransformer {
		public static class Impl implements RowTransformer {
			private Class<? extends ModelTransform> value;

			@Override
			public Class<? extends Annotation> annotationType() {
				return RowTransformer.class;
			}

			@Override
			public Class<? extends ModelTransform> value() {
				return value;
			}

			public Impl withValue(Class<? extends ModelTransform> value) {
				this.value = value;
				return this;
			}
		}

		/**
		 * The transformer
		 */
		Class<? extends ModelTransform> value();
	}

	/**
	 * Models row selection + styling. Because it's really adjunct to the dom
	 * structure, it doesn't fire directed events - except onBind, when its
	 * _existence_ is announced via an event. Note that the underlying rows (the
	 * array of models rendered as rows) does not change during the lifetime of
	 * the tablemodel, rather the tablemodel is replaced if the rows change
	 */
	public static class RowsModel {
		public abstract static class Support
				implements TableEvents.RowsModelAttached.Handler {
			protected RowsModel rowsModel;

			@Override
			public void onRowsModelAttached(RowsModelAttached event) {
				this.rowsModel = event.getModel();
				this.rowsModel.topicSelectedRowsChanged
						.add(this::onSelectedRowsChanged);
				updateRowDecoratorsAndScroll();
			}

			protected abstract void onSelectedRowsChanged();

			protected abstract void updateRowDecoratorsAndScroll();

			protected void selectAndScroll(int selectedElementIdx,
					List<?> rowElements) {
				if (selectedElementIdx != -1
						&& selectedElementIdx < rowElements.size()) {
					rowsModel.select(selectedElementIdx);
					rowsModel.scrollSelectedIntoView();
				}
			}
		}

		public class RowMeta implements TableEvents.RowClicked.Handler {
			public TableRow row;

			private boolean selected;

			Set<Object> flags = AlcinaCollections.newUniqueSet();

			int index;

			RowMeta(TableRow row) {
				this.row = row;
				row.rowMeta = this;
				this.index = meta.size();
			}

			public boolean isSelected() {
				return selected;
			}

			public void setFlag(Object flag, boolean present) {
				if (present) {
					flags.add(flag);
				} else {
					flags.remove(flag);
				}
				updateRow();
			}

			@Override
			public void onRowClicked(RowClicked event) {
				int currentIndex = getSelectedRowIndex();
				int toIndex = event.getModel().rowMeta.index;
				boolean shiftKey = event.getContext()
						.getOriginatingNativeEvent().getShiftKey();
				if (shiftKey && currentIndex != -1) {
					IntPair pair = new IntPair(toIndex, currentIndex)
							.toLowestFirst();
					meta.forEach(r -> r.setSelected(pair.contains(r.index)));
					// does not change selectedRowIndex
				} else {
					if (selected) {
						deSelect(toIndex);
					} else {
						select(toIndex);
					}
				}
			}

			public void setSelected(boolean selected) {
				this.selected = selected;
				setFlag("selected", selected);
			}

			void updateRow() {
				String className = null;
				if (!flags.isEmpty()) {
					className = flags.stream().map(Object::toString)
							.collect(Collectors.joining(" "));
				}
				row.setClassName(className);
			}
		}

		int selectedRowIndex = -1;

		public Topic<Void> topicSelectedRowsChanged = Topic.create();

		public List<RowMeta> meta = new ArrayList<>();

		public int getSelectedRowIndex() {
			return selectedRowIndex;
		}

		public void addRow(TableRow row) {
			meta.add(new RowMeta(row));
		}

		public IntPair getSelectedRowsRange() {
			int i1 = -1;
			int i2 = -1;
			for (RowMeta rm : meta) {
				if (rm.selected) {
					if (i1 == -1) {
						i1 = rm.index;
					}
					i2 = rm.index;
				} else {
					if (i1 == -1) {
						//
					} else {
						break;
					}
				}
			}
			if (i1 == -1) {
				return null;
			} else {
				return new IntPair(i1, i2);
			}
		}

		public void deSelect(int selectedIndex) {
			if (this.selectedRowIndex == -1) {
				return;
			}
			meta.get(selectedIndex).setSelected(false);
			this.selectedRowIndex = -1;
			topicSelectedRowsChanged.signal();
		}

		public void select(int selectedIndex) {
			if (selectedRowIndex == selectedIndex) {
				return;
			}
			meta.forEach(r -> r.setSelected(false));
			meta.get(selectedIndex).setSelected(true);
			this.selectedRowIndex = selectedIndex;
			topicSelectedRowsChanged.signal();
		}

		public void scrollSelectedIntoView() {
			Scheduler.get().scheduleDeferred(() -> {
				if (meta.size() <= selectedRowIndex || selectedRowIndex == -1) {
					return;
				}
				TableRow row = meta.get(selectedRowIndex).row;
				if (row.provideIsBound()) {
					row.provideElement().scrollIntoView();
				}
			});
		}
	}

	@Registration(EmptyResultHandler.class)
	@Reflected
	public static class EmptyResultHandler {
		public List<TableModel.TableRow>
				getEmptyResultPlaceholder(List<Field> fields) {
			return Collections.emptyList();
		}
	}

	public enum SortDirection {
		ASCENDING, DESCENDING
	}

	@Directed(
		reemits = { DomEvents.Click.class, TableEvents.CellClicked.class })
	public static class TableCell extends Model {
		public static transient boolean trackColumnValues = false;

		protected TableValueModel value;

		protected TableColumn column;

		protected TableModel.TableRow row;

		boolean editing;

		public TableCell() {
		}

		public TableCell(TableColumn column, TableModel.TableRow row) {
			this.column = column;
			this.row = row;
			this.value = new TableValueModel(this);
			if (trackColumnValues) {
				column.onValueAdded(value.getBindable());
			}
		}

		@Binding(type = Type.CSS_CLASS)
		public boolean isEditing() {
			return editing;
		}

		public void setEditing(boolean editing) {
			set("editing", this.editing, editing, () -> this.editing = editing);
		}

		@Directed
		public TableValueModel getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", column.toString(),
					Ax.trimForLogging(value.boundValue(), 15));
		}
	}

	@Feature.Ref(Feature_Dirndl_TableModel._TableColumn.class)
	@TypeSerialization(reflectiveSerializable = false)
	@TypedProperties
	@PropertyOrder(value = {}, custom = CustomOrder.class)
	public static class TableColumn extends Model implements
			DomEvents.Click.Handler, TableColumnMetadata.Change.Handler {
		public static class CustomOrder implements PropertyOrder.Custom {
			List<TypedProperty> defaultOrder = List.of(properties.caption,
					properties.columnFilter, properties.sortDirection);

			@Override
			public int compare(String o1, String o2) {
				return ordinal(o1) - ordinal(o2);
			}

			int ordinal(String name) {
				int idx = TypedProperty.indexOf(defaultOrder, name);
				return idx == -1 ? defaultOrder.size() : idx;
			}
		}

		@TypedProperties
		public class ColumnFilter extends Model.Fields
				implements DomEvents.Click.Handler, Property.Has {
			@Property.Not
			Field field;

			@Binding(type = Type.PROPERTY)
			boolean filtered;

			@Binding(type = Type.PROPERTY)
			boolean filterOpen;

			protected ColumnFilter(Field field) {
				this.field = field;
			}

			@Override
			public void onClick(Click event) {
				event.getContext().getOriginatingNativeEvent()
						.stopPropagation();
				event.reemitAs(this, TableColumnMetadata.EditFilter.class,
						this);
			}

			@Override
			public Property provideProperty() {
				return field.getProperty();
			}
		}

		@Directed(tag = "sort-direction")
		public static class SortDirectionModel extends Model.All
				implements ModelTransform<SortDirection, SortDirectionModel> {
			@Binding(type = Type.PROPERTY)
			SortDirection direction;

			@Override
			public SortDirectionModel apply(SortDirection direction) {
				this.direction = direction;
				return this;
			}
		}

		static PackageProperties._TableModel_TableColumn properties = PackageProperties.tableModel_tableColumn;

		static PackageProperties._TableModel_TableColumn_ColumnFilter _ColumnFilter_properties = PackageProperties.tableModel_tableColumn_columnFilter;

		private Field field;

		private SortDirection sortDirection;

		private String caption;

		private Class valueClass;

		private ColumnFilter columnFilter;

		public TableColumn() {
		}

		public TableColumn(String caption) {
			/*
			 * A placeholder column
			 */
		}

		public TableColumn(Field field) {
			this(field, null);
		}

		public TableColumn(Field field, SortDirection sortDirection) {
			this.field = field;
			this.sortDirection = sortDirection;
			this.caption = field.getLabel();
			this.columnFilter = new ColumnFilter(field);
		}

		public ColumnFilter getColumnFilter() {
			return columnFilter;
		}

		public String getCaption() {
			return this.caption;
		}

		public Field getField() {
			return this.field;
		}

		public SortDirection getSortDirection() {
			return this.sortDirection;
		}

		public Class getValueClass() {
			return this.valueClass;
		}

		@Override
		public void onClick(Click event) {
			event.reemitAs(this, TableEvents.SortTable.class, this);
		}

		public void onValueAdded(Object rowValue) {
			if (rowValue == null) {
				return;
			}
			Object value = field.getProperty().get(rowValue);
			if (value == null) {
				return;
			}
			Class clazz = value.getClass();
			if (valueClass == null) {
				valueClass = clazz;
			} else {
				if (Reflections.isAssignableFrom(clazz, valueClass)) {
					valueClass = clazz;
				} else if (Reflections.isAssignableFrom(clazz, valueClass)) {
					// preserve
				} else {
					// common ancestor
					throw new UnsupportedOperationException();
				}
			}
		}

		public void setField(Field field) {
			this.field = field;
		}

		public void setSortDirection(SortDirection sortDirection) {
			set("sortDirection", this.sortDirection, sortDirection,
					() -> this.sortDirection = sortDirection);
		}

		@Override
		public void
				onTableColumnMetadataChange(TableColumnMetadata.Change event) {
			TableColumnMetadata metadata = event.getModel();
			ColumnMetadata columnMetadata = metadata
					.getColumnMetadata(field.getProperty());
			SortDirection columnDirection = columnMetadata.getSortDirection();
			if (columnDirection != null) {
				setSortDirection(columnDirection);
			}
			_ColumnFilter_properties.filtered.set(columnFilter,
					columnMetadata.isFiltered());
			_ColumnFilter_properties.filterOpen.set(columnFilter,
					columnMetadata.isFilterOpen());
		}

		@Override
		public String toString() {
			return field.getProperty().toString();
		}
	}

	public static class TableColumnClicked
			extends ModelEvent<TableColumn, TableColumnClicked.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onTableColumnClicked(TableColumnClicked TableColumnClicked);
		}

		@Override
		public void dispatch(TableColumnClicked.Handler handler) {
			handler.onTableColumnClicked(this);
		}
	}

	public static class TableHeader extends Model {
		List<TableColumn> columns = new ArrayList<>();

		public TableHeader() {
		}

		@Directed
		public List<TableColumn> getColumns() {
			return this.columns;
		}
	}

	public static class TableValueModel extends Model implements ValueModel {
		protected TableCell cell;

		public TableValueModel() {
		}

		public TableValueModel(TableCell cell) {
			this.cell = cell;
		}

		public TableCell getCell() {
			return cell;
		}

		public void setCell(TableCell cell) {
			this.cell = cell;
		}

		@Override
		public Bindable getBindable() {
			return (Bindable) cell.row.rowModel;
		}

		@Override
		public Field getField() {
			return cell.column.field;
		}

		@Override
		public String getGroupName() {
			return null;
		}

		@Override
		public void onChildBindingCreated(
				com.totsp.gwittir.client.beans.Binding binding) {
			// NOOP
		}

		public Object boundValue() {
			return getField().getProperty().get(getBindable());
		}
	}

	@Directed(reemits = { DomEvents.Click.class, TableEvents.RowClicked.class })
	public static class TableRow extends Model
			implements TableEvents.RowClicked.Handler {
		List<TableCell> cells = new ArrayList<>();

		Object rowModel;

		Object originalRowModel;

		public RowsModel.RowMeta rowMeta;

		String className;

		public TableRow() {
		}

		public TableRow(TableModel model, Object originalRowModel) {
			this.originalRowModel = originalRowModel;
			this.rowModel = model.rowTransformer == null ? originalRowModel
					: model.rowTransformer.apply(originalRowModel);
			model.header.columns.stream()
					.map(column -> new TableCell(column, this))
					.forEach(cells::add);
		}

		@Binding(type = Type.CLASS_PROPERTY)
		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			set("className", this.className, className,
					() -> this.className = className);
		}

		public Object getRowModel() {
			return rowModel;
		}

		public Object getOriginalRowModel() {
			return originalRowModel;
		}

		@Directed
		public List<TableCell> getCells() {
			return this.cells;
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			if (originalRowModel instanceof Model) {
				((Model) originalRowModel).onBeforeRender(event);
			}
			super.onBeforeRender(event);
		}

		@Override
		public void onBind(Bind event) {
			super.onBind(event);
			if (originalRowModel instanceof Model) {
				((Model) originalRowModel).onBind(event);
			}
		}

		@Override
		public void onRowClicked(RowClicked event) {
			if (rowMeta != null) {
				// FIXME - treetable
				rowMeta.onRowClicked(event);
			}
			event.bubble();
		}

		@Override
		public String toString() {
			return cells.toString();
		}
	}

	static class Attributes {
		boolean detached;

		boolean nodeEditors = true;

		boolean editable;

		boolean adjunct;
	}

	class CellEditor {
		class ValueEditor extends Model.All implements ContextResolver.Has {
			class Resolver extends ContextResolver.DelegateToParent
					implements NodeEditorContext.Has {
				class NodeEditorContextImpl implements NodeEditorContext {
					@Override
					public boolean isEditable() {
						return true;
					}

					@Override
					public boolean isRenderAsNodeEditors() {
						return true;
					}
				}

				Resolver(ContextResolver logicalParent) {
					super(logicalParent);
				}

				@Override
				@Property.Not
				public NodeEditorContext getNodeEditorContext() {
					return new NodeEditorContextImpl();
				}
			}

			class EditableCell extends TableCell {
				EditableCell() {
					this.row = cell.row;
					this.column = new TableColumn(editableFields
							.get(cell.column.field.getProperty()));
					this.value = cell.value;
				}
			}

			TableValueModel value;

			ValueEditor() {
				value = Reflections.newInstance(cell.getValue().getClass());
				value.setCell(new EditableCell());
			}

			@Override
			@Property.Not
			public ContextResolver
					getContextResolver(AnnotationLocation location) {
				return new Resolver(cell.provideNode().getResolver());
			}

			@Override
			public void onBind(Bind event) {
				super.onBind(event);
				cell.setEditing(event.isBound());
			}
		}

		CellClicked event;

		TableCell cell;

		ValueEditor valueEditor;

		Overlay overlay;

		CellEditor(CellClicked event) {
			this.event = event;
			cell = event.getModel();
		}

		void open() {
			valueEditor = new ValueEditor();
			overlay = Overlay.attributes().overlay(cell, valueEditor).create();
			overlay.open();
		}
	}

	Map<Property, Field> editableFields;

	protected TableHeader header = new TableHeader();

	protected List<TableRow> rows = new ArrayList<>();

	protected List<Link> actions = new ArrayList<>();

	private Model emptyResults;

	private Attributes attributes;

	ModelTransform rowTransformer;

	protected RowsModel rowsModel = new RowsModel();

	public TableModel() {
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			event.reemitAs(this, RowsModelAttached.class, rowsModel);
			// a dirndl theme - fire an initialising event on attach
			rowsModel.topicSelectedRowsChanged.signal();
		}
	}

	public void addRow(TableRow row) {
		rows.add(row);
		rowsModel.addRow(row);
	}

	public List<Link> getActions() {
		return this.actions;
	}

	// @Directed
	public Model getEmptyResults() {
		return emptyResults;
	}

	public TableHeader getHeader() {
		return this.header;
	}

	public List<TableModel.TableRow> getRows() {
		return this.rows;
	}

	@Override
	public boolean isDetached() {
		return attributes.detached;
	}

	@Override
	public boolean isEditable() {
		/*
		 * return attributes.editable;
		 * 
		 * table editing should always be via overlay
		 */
		return false;
	}

	@Override
	public boolean isRenderAsNodeEditors() {
		return attributes.nodeEditors;
	}

	public void setEmptyResults(Model emptyResults) {
		this.emptyResults = emptyResults;
	}

	public void setRows(List<TableModel.TableRow> rows) {
		set("rows", this.rows, rows, () -> this.rows = rows);
	}

	@Override
	public void onCellClicked(CellClicked event) {
		if (attributes.editable) {
			new CellEditor(event).open();
		}
	}

	ModalResolver init(Node node) {
		RowTransformer rowTransformerAnn = node
				.annotation(RowTransformer.class);
		if (rowTransformerAnn != null) {
			rowTransformer = Reflections.newInstance(rowTransformerAnn.value());
			if (rowTransformer instanceof ContextSensitiveTransform) {
				((ContextSensitiveTransform) rowTransformer)
						.withContextNode(node);
			}
		}
		ModalResolver resolver = ModalResolver.multiple(node, true);
		resolver.setTableModel(this);
		node.setResolver(resolver);
		attributes = new Attributes();
		BeanViewModifiers args = node.annotation(BeanViewModifiers.class);
		if (args != null) {
			attributes.adjunct = args.adjunct();
			attributes.nodeEditors = args.nodeEditors();
			attributes.editable = args.editable();
			attributes.detached = args.detached();
		}
		return resolver;
	}
}

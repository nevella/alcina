package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.beans.Binding;
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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedBindableSearchActivity;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedCategoriesActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.SortTable;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.DirectedEntitySearchActivityTransformer.TableContainer;
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
 * The arbitrary case doesn't really need a supporting model - see (
 *
 * FIXME - dirndl - doc) this example of using transforms
 *
 *
 *
 */
public class TableModel extends Model implements NodeEditorContext {
	protected TableHeader header = new TableHeader();

	protected List<TableRow> rows = new ArrayList<>();

	protected List<Link> actions = new ArrayList<>();

	private Model emptyResults;

	private Attributes attributes;

	public TableModel() {
	}

	public List<Link> getActions() {
		return this.actions;
	}

	@Directed
	public Model getEmptyResults() {
		return emptyResults;
	}

	public TableHeader getHeader() {
		return this.header;
	}

	public List<TableRow> getRows() {
		return this.rows;
	}

	ModalResolver init(Node node) {
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

	@Override
	public boolean isDetached() {
		return attributes.detached;
	}

	@Override
	public boolean isEditable() {
		return attributes.editable;
	}

	@Override
	public boolean isRenderAsNodeEditors() {
		return attributes.nodeEditors;
	}

	public void setEmptyResults(Model emptyResults) {
		this.emptyResults = emptyResults;
	}

	public void setRows(List<TableRow> rows) {
		set("rows", this.rows, rows, () -> this.rows = rows);
	}

	static class Attributes {
		boolean detached;

		boolean nodeEditors = true;

		boolean editable;

		boolean adjunct;
	}

	public static class DirectedCategoriesActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedCategoriesActivity<?>, TableModel> {
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
			places.stream().map(CategoryNamePlaceTableAdapter::new)
					.map(bindable -> new TableRow(tableModel, bindable))
					.forEach(tableModel.rows::add);
			return tableModel;
		}

		protected boolean isPermitted(CategoryNamePlace place) {
			return true;
		}

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
	}

	public static class DirectedEntitySearchActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable>, TableContainer> {
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
						.forEach(tableModel.rows::add);
			} else {
				rowObjects.stream()
						.map(bindable -> new TableRow(tableModel, bindable))
						.forEach(tableModel.rows::add);
			}
			// add actions if editable and adjunct
			return tableContainer;
		}

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
	}

	@Registration(EmptyResultHandler.class)
	@Reflected
	public static class EmptyResultHandler {
		public List<TableRow> getEmptyResultPlaceholder(List<Field> fields) {
			return Collections.emptyList();
		}
	}

	public enum SortDirection {
		ASCENDING, DESCENDING
	}

	public static class TableCell extends Model {
		public static transient boolean trackColumnValues = false;

		protected TableValueModel value;

		protected TableColumn column;

		protected TableRow row;

		public TableCell() {
		}

		public TableCell(TableColumn column, TableRow row) {
			this.column = column;
			this.row = row;
			this.value = new TableValueModel(this);
			if (trackColumnValues) {
				column.onValueAdded(value.getBindable());
			}
		}

		public TableValueModel getValue() {
			return this.value;
		}
	}

	@TypeSerialization(reflectiveSerializable = false)
	public static class TableColumn extends Model
			implements DomEvents.Click.Handler {
		private Field field;

		private SortDirection sortDirection;

		private String caption;

		private Class valueClass;

		public TableColumn() {
		}

		public TableColumn(Field field) {
			this(field, null);
		}

		public TableColumn(Field field, SortDirection sortDirection) {
			this.field = field;
			this.sortDirection = sortDirection;
			this.caption = field.getLabel();
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
			this.sortDirection = sortDirection;
		}
	}

	public static class TableColumnClicked
			extends ModelEvent<TableColumn, TableColumnClicked.Handler> {
		@Override
		public void dispatch(TableColumnClicked.Handler handler) {
			handler.onTableColumnClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onTableColumnClicked(TableColumnClicked TableColumnClicked);
		}
	}

	public static class TableHeader extends Model {
		private List<TableColumn> columns = new ArrayList<>();

		public TableHeader() {
		}

		@Directed
		public List<TableColumn> getColumns() {
			return this.columns;
		}
	}

	@Directed(reemits = { DomEvents.Click.class, FormEvents.RowClicked.class })
	public static class TableRow extends Model {
		private List<TableCell> cells = new ArrayList<>();

		private Bindable bindable;

		public TableRow() {
		}

		public TableRow(TableModel model, Bindable bindable) {
			this.bindable = bindable;
			model.header.columns.stream()
					.map(column -> new TableCell(column, this))
					.forEach(cells::add);
		}

		public Bindable getBindable() {
			return bindable;
		}

		@Directed
		public List<TableCell> getCells() {
			return this.cells;
		}
	}

	public static class TableValueModel extends Model implements ValueModel {
		protected TableCell cell;

		public TableValueModel() {
		}

		public TableValueModel(TableCell formElement) {
			this.cell = formElement;
		}

		@Override
		public Bindable getBindable() {
			return cell.row.bindable;
		}

		@Override
		public Field getField() {
			return cell.column.field;
		}

		public TableCell getFormElement() {
			return this.cell;
		}

		@Override
		public String getGroupName() {
			return null;
		}

		@Override
		public void onChildBindingCreated(Binding binding) {
			// NOOP
		}
	}
}

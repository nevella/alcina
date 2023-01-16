package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.DisplaySearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModalResolver;
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
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.customiser.ModelPlaceCustomiser;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

public class TableModel extends Model {
	protected TableHeader header = new TableHeader();

	protected List<TableRow> rows = new ArrayList<>();

	protected List<Link> actions = new ArrayList<>();

	public TableModel() {
	}

	public List<Link> getActions() {
		return this.actions;
	}

	public TableHeader getHeader() {
		return this.header;
	}

	public List<TableRow> getRows() {
		return this.rows;
	}

	public static class DirectedCategoriesActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedCategoriesActivity<?>, TableModel> {
		@Override
		public TableModel apply(DirectedCategoriesActivity<?> activity) {
			TableModel tableModel = new TableModel();
			BoundWidgetTypeFactory factory = Registry
					.impl(TableTypeFactory.class);
			ModalResolver resolver = ModalResolver.multiple(node, true);
			node.setResolver(resolver);
			resolver.setTableModel(tableModel);
			List<CategoryNamePlace> places = activity.getPlace()
					.getNamedPlaces();
			places.removeIf(p -> !isPermitted(p));
			Class<? extends Bindable> resultClass = CategoryNamePlaceTableAdapter.class;
			GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							Reflections.at(resultClass).templateInstance(),
							factory, false, true, node.getResolver())
					.stream().map(TableColumn::new)
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
			AbstractContextSensitiveModelTransform<DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable>, TableModel> {
		@Override
		public TableModel apply(
				DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable> activity) {
			TableModel tableModel = new TableModel();
			BoundWidgetTypeFactory factory = Registry
					.impl(TableTypeFactory.class);
			if (activity.getSearchResults() == null) {
				return tableModel;
			}
			ModalResolver resolver = ModalResolver.multiple(node, true);
			resolver.setTableModel(tableModel);
			node.setResolver(resolver);
			BindableSearchDefinition def = activity.getSearchResults().getDef();
			String sortFieldName = def.getSearchOrders()
					.provideSearchOrderFieldName();
			SortDirection sortDirection = def.getSearchOrders()
					.provideIsAscending() ? SortDirection.ASCENDING
							: SortDirection.DESCENDING;
			Class<? extends Bindable> resultClass = activity.getSearchResults()
					.resultClass();
			List<Field> fields = GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							Reflections.at(resultClass).templateInstance(),
							factory, false, true, resolver);
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
			return tableModel;
		}
	}

	@Registration(EmptyResultHandler.class)
	@Reflected
	public static class EmptyResultHandler {
		public List<TableRow> getEmptyResultPlaceholder(List<Field> fields) {
			return Collections.emptyList();
		}
	}

	public static class SearchTableColumnClickHandler
			implements DomEvents.Click.Handler {
		private TableColumn column;

		public SearchTableColumnClickHandler(TableColumn column) {
			this.column = column;
		}

		@Override
		public void onClick(Click Click) {
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
			Client.goTo(place);
		}
	}

	public enum SortDirection {
		ASCENDING, DESCENDING
	}

	public static class TableCell extends Model {
		protected TableValueModel value;

		protected TableColumn column;

		protected TableRow row;

		public TableCell() {
		}

		public TableCell(TableColumn column, TableRow row) {
			this.column = column;
			this.row = row;
			this.value = new TableValueModel(this);
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

		@Override
		public void onClick(Click event) {
			new SearchTableColumnClickHandler(this).onClick(event);
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

		@Override
		public Class<TableColumnClicked.Handler> getHandlerClass() {
			return TableColumnClicked.Handler.class;
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

		@Directed
		public List<TableCell> getCells() {
			return this.cells;
		}
	}

	@Reflected
	@Registration(TableTypeFactory.class)
	public static class TableTypeFactory extends BoundWidgetTypeFactory {
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
	}
}

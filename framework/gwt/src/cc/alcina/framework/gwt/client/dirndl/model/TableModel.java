package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.DisplaySearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ModalDisplay.ModalResolver;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedBindableSearchActivity;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedCategoriesActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.CollectionNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.customiser.ModelPlaceCustomiser;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

public class TableModel extends Model {
	protected TableHeader header = new TableHeader();

	protected List<TableRow> rows = new ArrayList<>();

	protected List<LinkModel> actions = new ArrayList<>();

	public List<LinkModel> getActions() {
		return this.actions;
	}

	public TableHeader getHeader() {
		return this.header;
	}

	public List<TableRow> getRows() {
		return this.rows;
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

	public enum SortDirection {
		ASCENDING, DESCENDING;
	}

	public static class TableColumn extends Model {
		private Field field;

		private SortDirection sortDirection;

		public SortDirection getSortDirection() {
			return this.sortDirection;
		}

		public void setSortDirection(SortDirection sortDirection) {
			this.sortDirection = sortDirection;
		}

		public Field getField() {
			return this.field;
		}

		public void setField(Field field) {
			this.field = field;
		}

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

	@ClientInstantiable
	public static class SearchTableColumnClickHandler
			implements NodeEvent.Handler {
		@Override
		public void onEvent(Context eventContext) {
			TableColumn column = eventContext.node.getModel();
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

	public static class DirectedEntitySearchActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable>, TableModel> {
		@Override
		public TableModel apply(
				DirectedBindableSearchActivity<? extends EntityPlace, ? extends Bindable> activity) {
			TableModel model = new TableModel();
			BoundWidgetTypeFactory factory = Registry
					.impl(TableTypeFactory.class);
			if (activity.getSearchResults() == null) {
				return model;
			}
			node.pushResolver(ModalResolver.multiple(true));
			BindableSearchDefinition def = activity.getSearchResults().getDef();
			String sortFieldName = def.getSearchOrders()
					.provideSearchOrderFieldName();
			SortDirection sortDirection = def.getSearchOrders()
					.provideIsAscending() ? SortDirection.ASCENDING
							: SortDirection.DESCENDING;
			Class<? extends Bindable> resultClass = activity.getSearchResults()
					.resultClass();
			GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							Reflections.classLookup()
									.getTemplateInstance(resultClass),
							factory, false, true, node.getResolver())
					.stream().map(field -> {
						SortDirection fieldDirection = field.getPropertyName()
								.equals(sortFieldName) ? sortDirection : null;
						return new TableColumn(field, fieldDirection);
					}).forEach(model.header.columns::add);
			activity.getSearchResults().getQueriedResultObjects().stream()
					.map(bindable -> new TableRow(model, bindable))
					.forEach(model.rows::add);
			// add actions if editable and adjunct
			return model;
		}
	}

	public static class DirectedCategoriesActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedCategoriesActivity<?>, TableModel> {
		@Override
		public TableModel apply(DirectedCategoriesActivity<?> activity) {
			TableModel model = new TableModel();
			BoundWidgetTypeFactory factory = Registry
					.impl(TableTypeFactory.class);
			node.pushResolver(ModalResolver.multiple(true));
			List<CategoryNamePlace> places = activity.getPlace()
					.getNamedPlaces();
			places.removeIf(p -> !isPermitted(p));
			Class<? extends Bindable> resultClass = CategoryNamePlaceTableAdapter.class;
			GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							Reflections.classLookup()
									.getTemplateInstance(resultClass),
							factory, false, true, node.getResolver())
					.stream().map(TableColumn::new)
					.forEach(model.header.columns::add);
			places.stream().map(CategoryNamePlaceTableAdapter::new)
					.map(bindable -> new TableRow(model, bindable))
					.forEach(model.rows::add);
			return model;
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

			@Display(name = "Name", orderingHint = 10)
			@Custom(customiserClass = ModelPlaceCustomiser.class)
			public CategoryNamePlace getPlace() {
				return place;
			}

			@Display(name = "Description", orderingHint = 20)
			public String getDescription() {
				return Objects.equals(place.ensureAction().getDisplayName(),
						place.ensureAction().getDescription()) ? ""
								: place.ensureAction().getDescription();
			}

			@Override
			public String displayName() {
				return place.ensureAction().getDisplayName();
			}
		}
	}

	@RegistryLocation(registryPoint = TableTypeFactory.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class TableTypeFactory extends BoundWidgetTypeFactory {
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

		@Directed(renderer = CollectionNodeRenderer.class)
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
		public String getValueId() {
			return null;
		}
	}
}

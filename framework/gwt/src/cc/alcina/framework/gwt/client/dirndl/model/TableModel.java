package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedMultipleBindableActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.CollectionNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.NotRenderedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

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

	public static class TableColumn extends Model {
		private Field field;

		private String caption;

		public TableColumn() {
		}

		public TableColumn(Field field) {
			this.field = field;
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

		public List<TableColumn> getColumns() {
			return this.columns;
		}
	}

	@ClientInstantiable
	public static class TableModelBindableTransformer implements
			Function<DirectedMultipleBindableActivity<? extends EntityPlace, ? extends Bindable>, TableModel> {
		@Override
		public TableModel apply(
				DirectedMultipleBindableActivity<? extends EntityPlace, ? extends Bindable> activity) {
			TableModel model = new TableModel();
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			if (activity.getSearchResults() == null) {
				return model;
			}
			Class<? extends Bindable> resultClass = activity.getSearchResults()
					.resultClass();
			GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactoryAsList(
							Reflections.classLookup().getTemplateInstance(
									resultClass),
							factory, false, true)
					.stream().map(TableColumn::new)
					.forEach(model.header.columns::add);
			activity.getSearchResults().queriedResultObjects.stream()
					.map(bindable -> new TableRow(model, bindable))
					.forEach(model.rows::add);
			// add actions
			return model;
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

		@Directed(renderer = NotRenderedNodeRenderer.class)
		@Override
		public Bindable getBindable() {
			return cell.row.bindable;
		}

		@Override
		public Field getField() {
			return cell.column.field;
		}

		@Directed(renderer = NotRenderedNodeRenderer.class)
		public TableCell getFormElement() {
			return this.cell;
		}

		@Override
		public String getValueId() {
			return null;
		}
	}
}

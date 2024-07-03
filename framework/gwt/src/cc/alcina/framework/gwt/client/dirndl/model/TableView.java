package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.SortTable;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableRow;
import cc.alcina.framework.gwt.client.dirndl.model.TableView.TableContainer;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

@Directed
public class TableView extends
		AbstractContextSensitiveModelTransform<Collection<? extends Bindable>, TableContainer> {
	@Override
	public TableContainer apply(Collection<? extends Bindable> bindables) {
		TableModel tableModel = new TableModel();
		TableContainer tableContainer = new TableContainer(tableModel);
		tableModel.init(node);
		if (bindables.size() == 0) {
			tableModel.setEmptyResults(new LeafModel.TagText("empty-results",
					"No matching results found"));
		} else {
			Bindable first = bindables.iterator().next();
			List<Field> fields = BeanFields.query()
					.forMultipleWidgetContainer(true).forBean(first)
					.withAllowNullWidgetProviders(true)
					.withResolver(node.getResolver()).listFields();
			fields.stream().map(TableColumn::new)
					.forEach(tableModel.header.getColumns()::add);
			bindables.stream()
					.map(bindable -> new TableRow(tableModel, bindable))
					.forEach(tableModel.rows::add);
		}
		return tableContainer;
	}

	@Directed.Delegating
	class TableContainer extends Model.All
			implements TableEvents.SortTable.Handler {
		@Directed(className = "bound")
		TableModel tableModel;

		TableColumn sortedBy = null;

		TableContainer(TableModel tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public void onSortTable(SortTable event) {
			TableColumn column = event.getModel();
			Property property = column.getField().getProperty();
			if (sortedBy == column) {
				column.setSortDirection(
						column.getSortDirection() == SortDirection.ASCENDING
								? SortDirection.DESCENDING
								: SortDirection.ASCENDING);
			} else {
				sortedBy = column;
				column.setSortDirection(SortDirection.ASCENDING);
			}
			int multiplier = sortedBy
					.getSortDirection() == SortDirection.ASCENDING ? 1 : -1;
			Comparator<TableRow> cmp = new Comparator<>() {
				@Override
				public int compare(TableRow o1, TableRow o2) {
					return CommonUtils.compareWithNullMinusOne(get(o1), get(o2))
							* multiplier;
				}

				private Comparable get(TableRow row) {
					Bindable o = row.getBindable();
					if (o == null) {
						return null;
					}
					Object value = property.get(o);
					if (value == null) {
						return null;
					}
					return value.toString();
				}
			};
			List<TableRow> sorted = tableModel.rows.stream().sorted(cmp)
					.collect(Collectors.toList());
			tableModel.setRows(sorted);
		}
	}
}

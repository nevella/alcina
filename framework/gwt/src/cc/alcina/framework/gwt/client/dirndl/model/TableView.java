package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.SortTable;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableView.TableContainer;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

@Directed
public class TableView extends
		AbstractContextSensitiveModelTransform<Collection<?>, TableContainer> {
	@Override
	public TableContainer apply(Collection<?> rowModels) {
		TableModel tableModel = new TableModel();
		TableContainer tableContainer = new TableContainer(tableModel);
		tableModel.init(node);
		if (rowModels.size() == 0) {
			tableModel.setEmptyResults(new LeafModel.TagText("empty-results",
					"No matching results found"));
		} else {
			Object first = rowModels.iterator().next();
			if (tableModel.rowTransformer != null) {
				first = tableModel.rowTransformer.apply(first);
			}
			List<Field> fields = BeanFields.query()
					.forMultipleWidgetContainer(true).forBean(first)
					.withAllowNullWidgetProviders(true)
					.withResolver(node.getResolver()).listFields();
			fields.removeIf(field -> {
				return new AnnotationLocation(null, field.getProperty(),
						node.getResolver())
								.hasAnnotation((Directed.Exclude.class));
			});
			fields.stream().map(TableColumn::new)
					.forEach(tableModel.header.getColumns()::add);
			rowModels.stream().map(
					rowModel -> new TableModel.TableRow(tableModel, rowModel))
					.forEach(tableModel::addRow);
		}
		return tableContainer;
	}

	/*
	 * Note that if an ancestor implements TableColumnMetadata.Emitter (and
	 * handles sort), this should just pass it through and not handle
	 * onSortTable.
	 * 
	 * But the initial use case of TableColumnMetadata.Emitter _doesn't_ handle
	 * sort (TraversalBrowser), so currently sort stops here
	 */
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
			Comparator<TableModel.TableRow> cmp = new Comparator<>() {
				@Override
				public int compare(TableModel.TableRow o1,
						TableModel.TableRow o2) {
					return CommonUtils.compareWithNullMinusOne(get(o1), get(o2))
							* multiplier;
				}

				private Comparable get(TableModel.TableRow row) {
					Object o = row.getRowModel();
					if (o == null) {
						return null;
					}
					Object value = property.get(o);
					if (value == null) {
						return null;
					}
					if (value instanceof Comparable) {
						return (Comparable) value;
					}
					return value.toString();
				}
			};
			List<TableModel.TableRow> sorted = tableModel.rows.stream()
					.sorted(cmp).collect(Collectors.toList());
			tableModel.setRows(sorted);
		}
	}
}

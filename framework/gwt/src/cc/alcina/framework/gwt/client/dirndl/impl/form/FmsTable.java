package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.util.List;

import cc.alcina.framework.common.client.reflection.AttributeTemplate;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn.ColumnFilter;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableHeader;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableValueModel;

/*
 * Mostly annotation templates for the corresponding dirndl tables class
 * (TableModel, TableColumn etc)
 */
public class FmsTable {
	@Directed(tag = "td")
	public static class FmsTableCell extends Model {
		public FmsTableCell() {
		}

		@Directed
		public TableValueModel getValue() {
			return null;
		}
	}

	@Directed(tag = "th")
	@Directed(tag = "ch-content")
	public static class FmsTableColumn extends Model {
		public FmsTableColumn() {
		}

		@Directed(tag = "span", renderer = LeafRenderer.Text.class)
		public String getCaption() {
			return null;
		}

		@Directed
		public ColumnFilter getColumnFilter() {
			return null;
		}

		@Directed.Transform(
			value = TableColumn.SortDirectionModel.class,
			transformsNull = true)
		public SortDirection getSortDirection() {
			return null;
		}
	}

	@Directed(tag = "table")
	@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
	public static class FmsTableModel extends Model
			implements AttributeTemplate {
		public FmsTableModel() {
		}

		@Directed.Multiple({ @Directed(tag = "thead"), @Directed(tag = "tr") })
		public TableHeader getHeader() {
			return null;
		}

		@Directed.Multiple({ @Directed(tag = "tbody"), @Directed(tag = "tr") })
		public List<TableModel.TableRow> getRows() {
			return null;
		}

		public void setRows(List<TableModel.TableRow> rows) {
		}
	}

	@Directed(tag = "cells")
	public static class FmsTreeTableRow extends TableModel.TableRow
			implements AttributeTemplate {
	}
}

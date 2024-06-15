package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.AttributeTemplate;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableHeader;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableRow;
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

	@Directed(
		tag = "th",
		bindings = @Binding(
			from = "sortDirection",
			type = Type.CSS_CLASS,
			transform = ToSortColumnTransform.class))
	public static class FmsTableColumn extends Model {
		public FmsTableColumn() {
		}

		@Directed(tag = "span", renderer = LeafRenderer.Text.class)
		public String getCaption() {
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
		public List<TableRow> getRows() {
			return null;
		}

		public void setRows(List<TableRow> rows) {
		}
	}

	@Reflected
	public static class ToSortColumnTransform
			implements ToStringFunction<SortDirection> {
		@Override
		public String apply(SortDirection t) {
			if (t == null) {
				return "";
			}
			switch (t) {
			case ASCENDING:
				return "-sort-ascending";
			case DESCENDING:
				return "-sort-descending";
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}

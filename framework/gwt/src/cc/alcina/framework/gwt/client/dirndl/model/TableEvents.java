package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;

public class TableEvents {
	public static class SortTable
			extends ModelEvent<TableColumn, SortTable.Handler> {
		@Override
		public void dispatch(SortTable.Handler handler) {
			handler.onSortTable(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSortTable(SortTable event);
		}
	}

	public static class RowClicked
			extends ModelEvent<TableModel.TableRow, RowClicked.Handler> {
		@Override
		public void dispatch(RowClicked.Handler handler) {
			handler.onRowClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRowClicked(RowClicked event);
		}
	}

	public static class RowsModelAttached extends
			ModelEvent<TableModel.RowsModel, RowsModelAttached.Handler> {
		@Override
		public void dispatch(RowsModelAttached.Handler handler) {
			handler.onRowsModelAttached(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRowsModelAttached(RowsModelAttached event);
		}
	}

	public static class CellClicked
			extends ModelEvent<TableModel.TableCell, CellClicked.Handler> {
		@Override
		public void dispatch(CellClicked.Handler handler) {
			handler.onCellClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onCellClicked(CellClicked event);
		}
	}

	public static class ColumnsBound
			extends ModelEvent<ColumnsBound.Data, ColumnsBound.Handler> {
		public static class Data {
			public boolean bound;

			public List<TableColumn> columns;

			public Data(boolean bound, List<TableColumn> columns) {
				this.bound = bound;
				this.columns = columns;
			}
		}

		@Override
		public void dispatch(ColumnsBound.Handler handler) {
			handler.onColumnsBound(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onColumnsBound(ColumnsBound event);
		}

		public interface Binding extends Handler {
			@Override
			default void onColumnsBound(ColumnsBound event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}
}

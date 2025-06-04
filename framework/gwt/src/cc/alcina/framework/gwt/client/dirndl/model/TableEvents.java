package cc.alcina.framework.gwt.client.dirndl.model;

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
}

package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;

import cc.alcina.framework.common.client.util.Ax;

public class RangeFooter extends Header<String> {
	private AbstractCellTable table;

	boolean loading = true;

	private boolean searchReturnsLastResults;

	// unused for the mo (since always false)
	boolean neverLoading = false;

	public RangeFooter(AbstractCellTable table) {
		super(new TextCell());
		this.table = table;
		table.addLoadingStateChangeHandler(
				evt -> refresh(evt.getLoadingState()));
	}

	@Override
	public String getValue() {
		boolean empty = table.getRowCount() == 0;
		if (empty) {
			String message = loading || neverLoading ? "Loading"
					: "No records match";
			return message;
		} else {
			String rangeMessage = Ax.format("%s-%s",
					table.getVisibleItemCount() == 0 ? 0 : 1,
					table.getVisibleItemCount());
			if (isSearchReturnsLastResults()
					&& table.getVisibleItemCount() > 0) {
				rangeMessage = Ax.format("%s-%s",
						table.getRowCount() - table.getVisibleItemCount(),
						table.getRowCount());
			}
			String message = Ax.format("%s of %s%s", rangeMessage,
					table.getRowCount(), loading ? " (Loading)" : "");
			return message;
		}
	}

	public boolean isSearchReturnsLastResults() {
		return this.searchReturnsLastResults;
	}

	public void setSearchReturnsLastResults(boolean searchReturnsLastResults) {
		this.searchReturnsLastResults = searchReturnsLastResults;
	}

	private void refresh(LoadingState loadingState) {
		loading = loadingState != LoadingState.LOADED;
		neverLoading &= !loading;
		Scheduler.get().scheduleDeferred(() -> table.redrawFooters());
	}
}
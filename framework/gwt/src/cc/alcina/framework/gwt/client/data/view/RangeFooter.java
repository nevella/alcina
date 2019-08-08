package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;

import cc.alcina.framework.common.client.util.CommonUtils;

public class RangeFooter extends Header<String> {
	private AbstractCellTable table;

	boolean loading = true;

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
			String message = CommonUtils.formatJ("%s-%s of %s%s",
					table.getVisibleItemCount() == 0 ? 0 : 1,
					table.getVisibleItemCount(), table.getRowCount(),
					loading ? " (Loading)" : "");
			return message;
		}
	}

	private void refresh(LoadingState loadingState) {
		loading = loadingState != LoadingState.LOADED;
		neverLoading &= !loading;
		Scheduler.get().scheduleDeferred(() -> table.redrawFooters());
	}
}
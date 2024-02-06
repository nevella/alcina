package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.client.ui.IsWidget;

public interface CellTableView<T> extends IsWidget {
	default void clearTableSelectionModel() {
		EntityClientUtils.clearSelection(table());
	}

	public AbstractCellTable<T> table();
}

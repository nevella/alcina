package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.client.ui.IsWidget;

public interface CellTableView<T> extends IsWidget {
    public AbstractCellTable<T> table();

    default void clearTableSelectionModel() {
        DataClientUtils.clearSelection(table());
    }
}

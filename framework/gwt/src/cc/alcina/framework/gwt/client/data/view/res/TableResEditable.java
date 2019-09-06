package cc.alcina.framework.gwt.client.data.view.res;

import com.google.gwt.user.cellview.client.DataGrid;

public interface TableResEditable extends TableRes {
    @Override
    @Source({ DataGrid.Style.DEFAULT_CSS, "datagrid.css",
            "datagrid-editable.css" })
    TableStyle dataGridStyle();

    interface TableStyle extends TableRes.TableStyle {
    }
}
package cc.alcina.framework.gwt.client.entity.view.res;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.DataGrid;

public interface TableRes extends DataGrid.Resources {
    @Override
    @Source("transparent.png")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridLoading();

    /**
     * Icon used when a column is sorted in ascending order.
     */
    @Override
    @Source("sortAscending.png")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridSortAscending();

    /**
     * Icon used when a column is sorted in descending order.
     */
    @Override
    @Source("sortDescending.png")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridSortDescending();

    @Override
    @Source({ DataGrid.Style.DEFAULT_CSS, "datagrid.css" })
    TableStyle dataGridStyle();

    interface TableStyle extends DataGrid.Style {
    }
}
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;

public interface GridFormCellRenderer {
	public void addButtonWidget(Widget widget);

	public <T extends Widget> T getBoundWidget(int row);

	public Widget getWidget();

	public void renderCell(Field field, int row, int col, Widget widget);

	public void setRowVisibility(int row, boolean visible);
}

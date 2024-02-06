package cc.alcina.framework.gwt.client.ide;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo.LayoutInfo;

public interface WorkspaceVisualiser extends IsWidget {
	void focusVisibleView();

	Widget getContentWidget();

	LayoutInfo getLayoutInfo();

	void redraw();

	TreeItem selectNodeForObject(Object singleObj, boolean b);

	void setContentWidget(Widget widget);

	void showView(WorkspaceView view);
}

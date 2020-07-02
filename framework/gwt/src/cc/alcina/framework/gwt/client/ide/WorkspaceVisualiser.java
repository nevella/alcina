package cc.alcina.framework.gwt.client.ide;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo.LayoutInfo;

public interface WorkspaceVisualiser extends IsWidget {
	void focusVisibleView();

	LayoutInfo getLayoutInfo();

	void showView(WorkspaceView view);

	void setContentWidget(Widget widget);

	TreeItem selectNodeForObject(Object singleObj, boolean b);

	void redraw();

	Widget getContentWidget();
}

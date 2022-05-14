package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@Registration({ DirectedNodeRenderer.class, Widget.class })
public class WidgetNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Widget render(Node node) {
		Widget widget = (Widget) node.model;
		return widget;
	}
}

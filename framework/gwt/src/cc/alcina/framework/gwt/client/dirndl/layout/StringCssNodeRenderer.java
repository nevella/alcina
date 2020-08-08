package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class StringCssNodeRenderer extends LeafNodeRenderer {
	@Override
	public Widget render(Node node) {
		Widget widget = super.render(node);
		String cssClass = node.model.toString();
		if (Ax.notBlank(cssClass)) {
			widget.addStyleName(cssClass);
		}
		return widget;
	}
}

package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class CssClassNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "div");
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setClassName(getText(node));
		return rendered;
	}

	protected String getText(Node node) {
		return node.model == null ? "" : node.model.toString();
	}
}

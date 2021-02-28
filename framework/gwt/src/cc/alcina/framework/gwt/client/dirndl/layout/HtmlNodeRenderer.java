package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class HtmlNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "div");
	}

	@Override
	public Widget render(Node node) {
		// TODO bind to the reflector;
		Widget rendered = super.render(node);
		rendered.getElement().setInnerHTML(getText(node));
		return rendered;
	}

	protected String getText(Node node) {
		return node.model == null ? "" : node.model.toString();
	}
}

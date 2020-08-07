package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class TextNodeRenderer extends LeafNodeRenderer {
	@Override
	public Widget render(Node node) {
		// TODO bind to the reflector;
		Widget rendered = super.render(node);
		Object propertyValue = node.model;
		rendered.getElement().setInnerText(propertyValue == null ? "<null text>"
				: propertyValue.toString());
		return rendered;
	}
}

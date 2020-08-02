package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class TextNodeRenderer extends LeafNodeRenderer {
	@Override
	public Optional<Widget> render(Node node) {
		// TODO bind to the reflector;
		Optional<Widget> rendered = super.render(node);
		Object propertyValue = node.model;
		rendered.get().getElement()
				.setInnerText(propertyValue == null ? "<null text>"
						: propertyValue.toString());
		return rendered;
	}
}

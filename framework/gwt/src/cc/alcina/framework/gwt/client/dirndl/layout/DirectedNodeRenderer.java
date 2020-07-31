package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public interface DirectedNodeRenderer {
	Optional<Widget> render(Node node);

	default Optional<Widget> renderWithDefaults(Node node) {
		Optional<Widget> rendered = render(node);
		if (rendered.isPresent()) {
			renderDefaults(node, rendered.get());
		}
		return rendered;
	}

	default void renderDefaults(Node node, Widget widget) {
		if (node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
	}
}

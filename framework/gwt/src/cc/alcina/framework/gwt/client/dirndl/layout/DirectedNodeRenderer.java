package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@ClientInstantiable
public abstract class DirectedNodeRenderer {
	public abstract Optional<Widget> render(Node node);

	public Optional<Widget> renderWithDefaults(Node node) {
		Optional<Widget> rendered = render(node);
		if (rendered.isPresent()) {
			renderDefaults(node, rendered.get());
		}
		return rendered;
	}

	protected void renderDefaults(Node node, Widget widget) {
		if (node.directed != null && node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
	}
}

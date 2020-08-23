package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/*
 * For holder objects in a node tree - delegate to the unique (modificable) child
 */
public class DelegatingNodeRenderer extends DirectedNodeRenderer implements RendersToParentContainer {
	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		for (Node child : node.children) {
			result.addAll(child.render().widgets);
		}
		return result;
	}

	// Not called
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}
}

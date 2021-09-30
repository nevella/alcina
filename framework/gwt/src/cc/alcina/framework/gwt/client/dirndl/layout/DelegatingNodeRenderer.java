package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * For holder objects in a node tree - delegate to the unique (modificable) child
 */
public class DelegatingNodeRenderer extends DirectedNodeRenderer
		implements RendersToParentContainer {
	// Not called
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		for (Node child : node.children) {
			result.addAll(child.render().widgets);
		}
		return result;
	}

	@Directed(renderer = DelegatingNodeRenderer.class)
	public static class SimpleDelegate extends Model {
		private Object delegate;

		public SimpleDelegate() {
		}

		public SimpleDelegate(Object delegate) {
			this.delegate = delegate;
		}

		@Directed
		public Object getDelegate() {
			return this.delegate;
		}
	}
}

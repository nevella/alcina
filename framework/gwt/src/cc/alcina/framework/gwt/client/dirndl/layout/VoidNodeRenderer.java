package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/*
 * Indicates that the annotation/resolution chain does not define a renderer. Fall back on the model class
 */
public class VoidNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Optional<Widget> render(Node node) {
		throw new UnsupportedOperationException();
	}
}

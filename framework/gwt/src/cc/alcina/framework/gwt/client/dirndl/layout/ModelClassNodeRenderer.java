package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/*
 * Indicates that the annotation/resolution chain does not define a renderer. Fall back on the model class
 */
public class ModelClassNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}
}

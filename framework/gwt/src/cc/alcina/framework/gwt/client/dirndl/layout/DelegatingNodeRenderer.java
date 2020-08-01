package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/*
 * For holder objects in a node tree - delegate to the unique (modificable) child
 */
public class DelegatingNodeRenderer implements DirectedNodeRenderer{

	@Override
	public Optional<Widget> render(Node node) {
		Preconditions.checkArgument(node.descriptor.getProperties().length==1);
		return Optional.empty();
	}
}

package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class MockupNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Widget render(Node node) {
		Label label = new Label(Ax.format("[%s]", node.pathSegment()));
		NodeRendererStyle.MOCKUP_NODE.set(label.getElement());
		return label;
	}
}

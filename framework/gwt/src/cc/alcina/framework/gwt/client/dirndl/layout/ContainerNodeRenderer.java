package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class ContainerNodeRenderer extends DirectedNodeRenderer {
	protected String getTag(Node node) {
		return node.directed.tag();
	}

	@Override
	public Widget render(Node node) {
		String tag = getTag(node);
		Preconditions.checkArgument(Ax.notBlank(tag));
		FlowPanel panel = new FlowPanel(tag);
		for (Node child : node.children) {
			child.render().forEach(panel::add);
		}
		return panel;
	}
}

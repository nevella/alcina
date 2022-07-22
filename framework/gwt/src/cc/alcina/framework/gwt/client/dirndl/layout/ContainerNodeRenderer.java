package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class ContainerNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Widget render(Node node) {
		String tag = getTag(node);
		Preconditions.checkArgument(Ax.notBlank(tag));
		FlowPanel panel = new FlowPanel(tag);
		for (Node child : node.children) {
			child.render().widgets.forEach(panel::add);
		}
		return panel;
	}

	protected String getTag(Node node) {
		if (node.model instanceof HasTag) {
			String tag = ((HasTag) node.model).provideTag();
			if (Ax.notBlank(tag)) {
				return tag;
			}
		}
		return node.directed.tag();
	}
}

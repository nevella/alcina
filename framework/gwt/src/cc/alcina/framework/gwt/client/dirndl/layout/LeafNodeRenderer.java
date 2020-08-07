package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.widget.SimpleWidget;

public class LeafNodeRenderer extends DirectedNodeRenderer {
	protected String getTag(Node node) {
		return node.directed.tag();
	}

	@Override
	public Widget render(Node node) {
		String tag = getTag(node);
		Preconditions.checkArgument(Ax.notBlank(tag));
		return new SimpleWidget(tag);
	}
}

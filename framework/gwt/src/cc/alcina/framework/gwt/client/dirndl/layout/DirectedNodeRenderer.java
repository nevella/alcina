package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@Reflected
public abstract class DirectedNodeRenderer {
	public abstract Widget render(Node node);

	public List<Widget> renderWithDefaults(Node node) {
		Widget rendered = render(node);
		renderDefaults(node, rendered);
		return Collections.singletonList(rendered);
	}

	protected void renderDefaults(Node node, Widget widget) {
		if (node.directed != null) {
			if (node.directed.cssClass().length() > 0) {
				widget.addStyleName(node.directed.cssClass());
			}
		}
	}
}

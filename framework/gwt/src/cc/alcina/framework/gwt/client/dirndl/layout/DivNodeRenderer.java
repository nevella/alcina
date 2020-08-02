package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class DivNodeRenderer extends ContainerNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return "div";
	}
}

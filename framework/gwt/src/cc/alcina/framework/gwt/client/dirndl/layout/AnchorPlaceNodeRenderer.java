package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.place.BasePlace;

public class AnchorPlaceNodeRenderer extends TextNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return "a";
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		BasePlace place = (BasePlace) node.model;
		rendered.getElement().setAttribute("href", place.toHrefString());
		return rendered;
	}
}

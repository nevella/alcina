package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class SafeHtmlNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "span");
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setInnerSafeHtml((SafeHtml) node.model);
		if(getTag(node).equals("a")) {
			rendered.getElement().setAttribute("href", "#");	
		}
		return rendered;
	}

}

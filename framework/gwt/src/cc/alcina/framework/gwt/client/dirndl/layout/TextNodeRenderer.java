package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = String.class)
public class TextNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "span");
	}

	@Override
	public Widget render(Node node) {
		// TODO bind to the reflector;
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(getText(node));
		return rendered;
	}

	protected String getText(Node node) {
		return node.model == null ? "<null text>" : node.model.toString();
	}
}

package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

//@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = Entity.class)
//not registered - because entities are often the backing objects for actions. Subclass with a filter if you want to mockup
public class MockupEntityNodeRenderer extends LeafNodeRenderer {
	@Override
	public Widget render(Node node) {
		// TODO bind to the reflector;
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(Ax.format("[%s] :: %s",
				node.model.getClass().getSimpleName(), node.model.toString()));
		NodeRendererStyle.MOCKUP_ENTITY_NODE.set(rendered);
		return rendered;
	}

	@Override
	protected String getTag(Node node) {
		return "div";
	}
}

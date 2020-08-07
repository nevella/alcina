package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = Entity.class)
public class MockupEntityNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return "div";
	}

	@Override
	public Optional<Widget> render(Node node) {
		// TODO bind to the reflector;
		Optional<Widget> rendered = super.render(node);
		rendered.get().getElement().setInnerText(Ax.format("[%s] :: %s",
				node.model.getClass().getSimpleName(), node.model.toString()));
		NodeRendererStyle.MOCKUP_ENTITY_NODE.set(rendered.get());
		return rendered;
	}
}

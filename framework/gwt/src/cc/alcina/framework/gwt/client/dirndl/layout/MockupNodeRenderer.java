package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class)
public class MockupNodeRenderer extends ContainerNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return "div";
	}

	@Override
	public Widget render(Node node) {
		Widget container = super.render(node);
		NodeRendererStyle.MOCKUP_NODE.set(container);
		Label label = new Label(Ax.format("mockup:%s :: %s",
				node.model.getClass().getSimpleName(),
				node.directed.cssClass()));
		NodeRendererStyle.MOCKUP_NODE_LABEL.set(label);
		((FlowPanel) container).insert(label, 0);
		return container;
	}
}

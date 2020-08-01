package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@ClientInstantiable
public class MockupNodeRenderer extends ContainerNodeRenderer {
	@Override
	public Optional<Widget> render(Node node) {
		Optional<Widget> container = super.render(node);
		NodeRendererStyle.MOCKUP_NODE.set(container.get());
		Label label = new Label(
				Ax.format("mockup:%s :: %s", node.model.getClass().getSimpleName(),node.directed.cssClass()));
		NodeRendererStyle.MOCKUP_NODE_LABEL.set(label);
		((FlowPanel) container.get()).insert(label	, 0);
		return container;
	}
}

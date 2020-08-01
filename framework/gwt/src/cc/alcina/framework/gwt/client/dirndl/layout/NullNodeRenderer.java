package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@ClientInstantiable
public class NullNodeRenderer extends DirectedNodeRenderer {
	@Override
	public Optional<Widget> render(Node node) {
		Label label = new Label("Null model");
		NodeRendererStyle.MOCKUP_NODE.set(label.getElement());
		return Optional.of(label);
	}
}

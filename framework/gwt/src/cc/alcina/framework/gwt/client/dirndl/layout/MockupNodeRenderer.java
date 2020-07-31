package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class MockupNodeRenderer implements DirectedNodeRenderer{
	@Override
	public Optional<Widget> render(Node node) {
		Label label = new Label(node.model.getClass().getSimpleName());
		label.setStyleName("");
		NodeRendererStyle.MOCKUP_NODE.addTo(label);
		return Optional.of(label);
	}
}

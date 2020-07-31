package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class MissingNodeRenderer implements DirectedNodeRenderer{

	@Override
	public Optional<Widget> render(Node node) {
		Label label = new Label(Ax.format("Missing renderer - model class %s",node.model.getClass().getSimpleName()));
		label.setStyleName("");
		NodeRendererStyle.MISSING_NODE.addTo(label);
		return Optional.of(label);
	}
}

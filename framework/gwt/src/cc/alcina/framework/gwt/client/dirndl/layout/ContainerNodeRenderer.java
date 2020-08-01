package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@ClientInstantiable
public class ContainerNodeRenderer extends DirectedNodeRenderer{
	@Override
	public Optional<Widget> render(Node node) {
		FlowPanel widget = new FlowPanel();
		for(Node child:node.children) {
			widget.add(child.render().get());
		}
		return Optional.of(widget);
	}
}

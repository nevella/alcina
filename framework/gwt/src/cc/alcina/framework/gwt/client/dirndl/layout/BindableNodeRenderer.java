package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@Registration({ DirectedNodeRenderer.class, Bindable.class })
//marker for dirndl1.1 - for routing to DirectedRenderer.BindableRenderer
public class BindableNodeRenderer extends DirectedNodeRenderer {

	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}
}

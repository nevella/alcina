package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class NotRenderedNodeRenderer extends DirectedNodeRenderer {
	@Override
	public List<Widget> renderWithDefaults(Node node) {
		return Collections.emptyList();
	}

	// Not called
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}
}

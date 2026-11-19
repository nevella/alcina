package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.List;

import cc.alcina.framework.common.client.collections.NotifyingList;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/*
 * ex-package access for FragmentNode
 */
public class FragmentNodeAccess {
	public static Rendered getRendered(Node node) {
		return node.rendered;
	}

	public static NotifyingList<Node> ensureChildren(Node node) {
		return node.ensureChildren();
	}

	public static Node getParent(Node node) {
		return node.parent;
	}
}

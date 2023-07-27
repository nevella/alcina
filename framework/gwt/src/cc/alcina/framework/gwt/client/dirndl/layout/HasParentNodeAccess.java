package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * In general, models should not be able to access their parent nodes (since the
 * node structure is derived from the model, not the reverse).
 *
 * <p>
 * The exception is editable documents - so the corresponding model
 * (FragmentNode) implements this interface
 *
 * @author nick@alcina.cc
 *
 */
public interface HasParentNodeAccess {
	default List<Node> provideChildNodes() {
		return ((Model) this).provideNode().children;
	}

	default Node provideParentNode() {
		return ((Model) this).provideNode().parent;
	}
}
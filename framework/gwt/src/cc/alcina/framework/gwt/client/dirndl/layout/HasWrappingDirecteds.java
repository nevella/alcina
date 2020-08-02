package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public interface HasWrappingDirecteds {
	List<Directed> getWrappingDirecteds(Node node);
}

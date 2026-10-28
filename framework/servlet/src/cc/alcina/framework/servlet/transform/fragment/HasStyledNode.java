package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.traversal.Selection;

public interface HasStyledNode<T> extends Selection<T> {
	StyledNode styledNode();
}

package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import com.google.common.base.Preconditions;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

public interface HasPutText {
	default void putText(String text) {
		FragmentNode fragmentNode = (FragmentNode) this;
		Preconditions.checkState(fragmentNode.children().count() == 0);
		fragmentNode.nodes().append(new Inline.Text(text));
	}
}
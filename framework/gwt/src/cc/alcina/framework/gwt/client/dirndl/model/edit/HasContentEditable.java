package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;

public interface HasContentEditable {
	boolean provideIsContentEditable();

	public static boolean isUneditableSibling(FragmentNode sibling) {
		if (sibling == null) {
			return true;
		}
		return sibling instanceof HasContentEditable
				&& !((HasContentEditable) sibling).provideIsContentEditable();
	}
}

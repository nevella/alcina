package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

public interface HasContentEditable {
	boolean provideIsContentEditable();

	public static boolean isUneditable(FragmentNode node) {
		return node instanceof HasContentEditable
				&& !((HasContentEditable) node).provideIsContentEditable();
	}
}

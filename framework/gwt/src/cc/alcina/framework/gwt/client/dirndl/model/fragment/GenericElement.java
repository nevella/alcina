package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode.Transformer;

/*
 * Reverse transformation falls back on this model if no other matches exist
 */
@Transformer(NodeTransformer.GenericElementTransformer.class)
public class GenericElement extends FragmentNode implements HasTag {
	public String tag;

	public GenericElement() {
		int debugh = 4;
	}

	public GenericElement(String tag) {
		this.tag = tag;
	}

	@Override
	public void copyFromExternal(FragmentNode external) {
		tag = ((GenericElement) external).tag;
	}

	@Override
	public String provideTag() {
		return tag;
	}
}
package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode.Leaf;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode.Transformer;

/*
 * Models a w3c Text node
 */
@Transformer(NodeTransformer.Text.class)
@Directed(renderer = LeafRenderer.TextNode.class)
public class TextNode extends FragmentNode implements Leaf {
	private String value;

	public TextNode() {
	}

	public TextNode(String value) {
		this.value = value;
	}

	@Override
	public void copyFromExternal(FragmentNode external) {
		value = ((TextNode) external).value;
	}

	@Binding(type = Type.INNER_TEXT)
	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		set("value", this.value, value, () -> this.value = value);
	}

	// FIXME - FN - fix (remove)once rv text update works
	public String liveValue() {
		return provideNode().getRendered().getNode().getNodeValue();
	}
}
package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = String.class)
public class TextNodeRenderer extends LeafNodeRenderer {
	@Override
	public Widget render(Node node) {
		// FIXME - dirndl1.3 - bind to the reflector (does it already?)
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(getText(node));
		return rendered;
	}

	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "span");
	}

	protected String getText(Node node) {
		return node.model == null ? "<null text>" : node.model.toString();
	}

	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = Number.class)
	public static class NumberNodeRenderer extends TextNodeRenderer {
	}

	@Directed(tag = "div", bindings = @Binding(from = "text", type = Type.INNER_TEXT))
	public static class TextString extends Model {
		private String text;

		public TextString() {
		}

		public TextString(String text) {
			setText(text);
		}

		public String getText() {
			return this.text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}

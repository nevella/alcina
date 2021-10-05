package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class TextNodeRenderer extends LeafNodeRenderer {
	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(getText(node));
		return rendered;
	}

	protected String getModelText(Object model) {
		return model.toString();
	}

	@Override
	protected String getTag(Node node) {
		return Ax.blankTo(super.getTag(node), "span");
	}

	protected String getText(Node node) {
		return node.model == null ? "<null text>" : getModelText(node.model);
	}

	/*
	 * Normally entities, if directly rendered, are the models for actions - so
	 * just some simple text...
	 */
	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = Entity.class)
	public static class EntityNodeRenderer extends TextNodeRenderer {
		@Override
		protected String getModelText(Object model) {
			return "";
		}
	}

	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = Enum.class)
	public static class EnumNodeRenderer extends TextNodeRenderer {
		@Override
		protected String getModelText(Object model) {
			if (model instanceof HasDisplayName) {
				return ((HasDisplayName) model).displayName();
			} else {
				return super.getModelText(model);
			}
		}
	}

	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = String.class)
	public static class StringNodeRenderer extends TextNodeRenderer {
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

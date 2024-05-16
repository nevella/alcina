package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasStringValue;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;

/**
 * <p>
 * Renderers that emit no new inputs, only a widget
 *
 * <p>
 * A quick note re the HtmlXxx classes:
 *
 * <ul>
 * <li>Use <code>@Directed(renderer=LeafRenderer.Html.class)</code> to render a
 * String property as an element containing html (by default a
 * <code>&lt;span&gt;</code> element)
 * <li>Use a property of type <code>LeafRenderer.HtmlBlock</code>, with a
 * default <code>@Directed</code> annotation to have a property whose value
 * property as an element containing html (by default a <code>&lt;div&gt;</code>
 * element)
 * <li>Use a transform of type <code>LeafRenderer.HtmlModel.Transform</code>,
 * with a default <code>@Directed</code> annotation to have a list of String
 * instances be rendered as a list of non-escaped html elements
 *
 * </ul>
 *
 *
 *
 */
public abstract class LeafRenderer extends DirectedRenderer {
	// for dirndl, mostly
	public static final Object OBJECT_INSTANCE = new Object();

	@Override
	protected void render(RendererInput input) {
		Node node = input.node;
		renderElement(node);
		renderNode(node);
	}

	protected void renderElement(Node node) {
		String tag = getTag(node, "span");
		Preconditions.checkArgument(Ax.notBlank(tag));
		node.resolver.renderElement(node, tag);
	}

	protected abstract void renderNode(Node node);

	public static class Blank extends LeafRenderer {
		@Override
		protected void renderNode(Node node) {
			// NOOP
		}
	}

	@Registration({ DirectedRenderer.class, Boolean.class })
	public static class BooleanRenderer extends Text {
	}

	@Registration({ DirectedRenderer.class, Date.class })
	public static class DateRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			return Ax.dateTimeSlash((Date) model);
		}

		public static class ShortMonth extends Text {
			@Override
			protected String getModelText(Object model) {
				return DateStyle.DM_SHORT_MONTH.format((Date) model);
			}
		}
	}

	@Registration({ DirectedRenderer.class, Entity.class })
	public static class EntityRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			return HasDisplayName.displayName(model);
		}
	}

	public static class EntityPlaceholderRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			return "";
		}
	}

	@Registration({ DirectedRenderer.class, Enum.class })
	public static class EnumRenderer extends HasDisplayNameRenderer {
	}

	public static class HasDisplayNameRenderer extends Text {
		@Override
		protected String getModelText(Object model) {
			if (model instanceof HasDisplayName) {
				return ((HasDisplayName) model).displayName();
			} else {
				return super.getModelText(model);
			}
		}
	}

	public static class Html extends LeafRenderer {
		protected String getText(Node node) {
			return node.model == null ? "" : node.model.toString();
		}

		@Override
		protected void renderNode(Node node) {
			node.rendered.asElement().setInnerHTML(getText(node));
		}
	}

	@Registration({ DirectedRenderer.class, Number.class })
	public static class NumberNode extends Text {
		public static class Currency extends Text {
			@Override
			protected String getModelText(Object model) {
				FormatBuilder format = new FormatBuilder();
				double value = (double) model;
				if (Ax.twoPlaces(value + 0.0001) != Ax.twoPlaces(value)) {
					// rounding issue somewhere in double creation - e.g
					// 27.994999997
					value = value + 0.0001;
				}
				if (value < 0) {
					format.append("-");
				}
				value = Math.abs(value);
				format.append((int) value);
				format.append(".");
				value = value - Math.floor(value);
				value = value * 100;
				Preconditions.checkArgument(value < 100);
				if (value < 10) {
					format.append("0");
				}
				format.append((int) value);
				return format.toString();
			}
		}
	}

	/**
	 * Renders the input model as a DOM processing instruction node. Requires
	 * the input model implement HasStringValue
	 */
	public static class ProcessingInstructionNode extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			String contents = ((HasStringValue) input.model).getStringValue();
			input.resolver.renderNode(input.node, rendersAsType(),
					input.soleDirected().tag(), contents);
		}

		@Override
		public DomNodeType rendersAsType() {
			return DomNodeType.PROCESSING_INSTRUCTION;
		}
	}

	public static class SafeHtml extends LeafRenderer {
		@Override
		protected String getTag(Node node, String defaultTag) {
			return super.getTag(node, "span");
		}

		@Override
		protected void renderNode(Node node) {
			Element element = node.rendered.asElement();
			element.setInnerSafeHtml(
					(com.google.gwt.safehtml.shared.SafeHtml) node.model);
			if (element.hasTagName("a")) {
				element.setAttribute("href", "#");
			}
		}
	}

	@Registration({ DirectedRenderer.class, String.class })
	public static class StringNode extends Text {
	}

	public static class Text extends LeafRenderer {
		protected String getModelText(Object model) {
			return model.toString();
		}

		protected String getText(Node node) {
			return node.model == null ? "<null text>"
					: getModelText(node.model);
		}

		@Override
		protected void renderNode(Node node) {
			node.rendered.asElement().setInnerText(getText(node));
		}
	}

	/**
	 * Renders the input model as a DOM text node. If the model is a string,
	 * binds the node text
	 */
	public static class TextNode extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			String contents = input.model instanceof String
					? input.model.toString()
					: "";
			input.resolver.renderText(input.node, contents);
		}

		@Override
		public DomNodeType rendersAsType() {
			return DomNodeType.TEXT;
		}
	}
}
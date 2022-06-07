package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.layout.TextNodeRenderer.FieldNamesAsTags;
import cc.alcina.framework.gwt.client.dirndl.widget.SimpleWidget;

public abstract class DirectedRenderer {
	protected abstract void render(DirectedLayout.RendererInput input);

	protected void renderDefaults(Node node, Widget widget) {
		if (node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
	}

	@Registration({ DirectedRenderer.class, TextNodeRenderer.class })
	public static class Text extends Leaf {
		@Override
		protected void render(RendererInput input) {
			super.render(input);
			Node node = input.node;
			node.rendered.widget.getElement().setInnerText(getText(node));
		}

		protected String getModelText(Object model) {
			return model.toString();
		}

		@Override
		protected String getTag(Node node) {
			if (node.parent != null && node.parent.has(FieldNamesAsTags.class)
					&& node.property.getName() != null) {
				return CommonUtils.deInfixCss(node.property.getName());
			}
			return Ax.blankTo(super.getTag(node), "span");
		}

		protected String getText(Node node) {
			return node.model == null ? "<null text>"
					: getModelText(node.model);
		}
	}

	static abstract class Leaf extends DirectedRenderer {
		protected String getTag(Node node) {
			return node.directed.tag();
		}

		@Override
		protected void render(RendererInput input) {
			Node node = input.node;
			String tag = getTag(node);
			Preconditions.checkArgument(Ax.notBlank(tag));
			node.rendered.widget = new SimpleWidget(tag);
		}
	}

	@Registration({ DirectedRenderer.class, DelegatingNodeRenderer.class })
	public static class Delegating extends DirectedRenderer
			implements GeneratesPropertyInputs {
		@Override
		protected void render(RendererInput input) {
			Preconditions.checkArgument(input.model instanceof Bindable);
			// NOOP, no container widget/node
			generatePropertyInputs(input);
		}
	}

	@Registration({ DirectedRenderer.class, ContainerNodeRenderer.class })
	public static class Container extends DirectedRenderer {
		protected String getTag(Node node) {
			String tag = node.directed.tag();
			return Ax.blankTo(tag, CommonUtils
					.deInfixCss(node.model.getClass().getSimpleName()));
		}

		@Override
		protected void render(RendererInput input) {
			Node node = input.node;
			String tag = getTag(node);
			if (tag.length() > 0) {
				FlowPanel widget = new FlowPanel(getTag(node));
				node.rendered.widget = widget;
			}
		}
	}

	interface GeneratesPropertyInputs {
		default void generatePropertyInputs(RendererInput input) {
			// Enqueue in reverse order because processed (added to parent) in
			// last-first order
			// FIXME - 1.1 - cache this reversed order
			List<Property> properties = Reflections.at((input.model.getClass()))
					.properties();
			properties = properties.stream().collect(Collectors.toList());
			Collections.reverse(properties);
			for (Property property : properties) {
				// FIXME - can probably get a location rather than property
				Property directedProperty = input.resolver
						.resolveDirectedProperty(property);
				if (directedProperty != null) {
					Object childModel = property.get(input.model);
					input.enqueueInput(input.resolver, childModel,
							new AnnotationLocation(input.model.getClass(),
									property, input.resolver),
							null, input.node);
				}
			}
		}
	}

	@Registration({ DirectedRenderer.class, BindableNodeRenderer.class })
	public static class BindableRenderer extends DirectedRenderer
			implements GeneratesPropertyInputs {
		@Override
		protected void render(RendererInput input) {
			// could subclass container - but we're going for composition
			new Container().render(input);
			generatePropertyInputs(input);
		}
	}
}

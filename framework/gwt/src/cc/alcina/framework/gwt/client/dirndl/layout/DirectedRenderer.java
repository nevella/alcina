package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.ContextSensitiveTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.widget.SimpleWidget;

/**
 * <p>
 * Processes a {@link DirectedLayout.RendererInput} - it:
 * <ul>
 * <li>Generates [0,1] widgets, adds to nearest ancestor in the
 * {@code RendererInput.node} ancestry chain
 * <li>Enqueues [0,n] renderinput children
 * </ul>
 *
 * <p>
 * These replace DirectedNodeRenderer, the transition uses a registry lookup to
 * translate from DirectedNodeRenderer
 *
 */
public abstract class DirectedRenderer {
	protected void applyCssClass(Node node, Widget widget) {
		if (node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
	}

	protected abstract void render(DirectedLayout.RendererInput input);

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

	/**
	 * The most-specific @Directed at the initiating AnnotationLocation will be
	 * applied to each Collection element
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Registration({ DirectedRenderer.class, CollectionNodeRenderer.class })
	public static class Collection extends DirectedRenderer
			implements GeneratesTransformModel {
		@Override
		protected void render(RendererInput input) {
			Preconditions
					.checkArgument(input.model instanceof java.util.Collection);
			// zero widgets for the container, generates input per child
			List list = (List) ((java.util.Collection) input.model).stream()
					.collect(Collectors.toList());
			// reverse for traversal order (see DirectedLayout.layout())
			Collections.reverse(list);
			list.forEach(model -> {
				Object transformedModel = transformModel(input, model);
				input.enqueueInput(input.resolver, transformedModel,
						input.location, Arrays.asList(input.soleDirected()),
						input.node, false);
			});
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
				applyCssClass(node, widget);
			}
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

	@Registration({ DirectedRenderer.class, TextNodeRenderer.class })
	public static class Text extends Leaf {
		protected String getModelText(Object model) {
			return model.toString();
		}

		@Override
		protected String getTag(Node node) {
			if (node.parent != null && node.parent.has(PropertyNameTags.class)
					&& node.property.getName() != null) {
				return CommonUtils.deInfixCss(node.property.getName());
			}
			return Ax.blankTo(super.getTag(node), "span");
		}

		protected String getText(Node node) {
			return node.model == null ? "<null text>"
					: getModelText(node.model);
		}

		@Override
		protected void render(RendererInput input) {
			super.render(input);
			Node node = input.node;
			node.rendered.widget.getElement().setInnerText(getText(node));
		}
	}

	/**
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Registration({ DirectedRenderer.class, ModelTransformNodeRenderer.class })
	public static class TransformRenderer extends DirectedRenderer
			implements GeneratesTransformModel {
		@Override
		protected void render(RendererInput input) {
			Object transformedModel = transformModel(input, input.model);
			input.enqueueInput(input.resolver, transformedModel, input.location,
					Arrays.asList(input.soleDirected()), input.node, true);
		}
	}

	interface GeneratesPropertyInputs {
		default void generatePropertyInputs(RendererInput input) {
			// Enqueue in reverse order because processed (added to parent) in
			// last-first order
			// FIXME - 1.1 - cache this reversed order (actually, use a
			// different structure, add to start and use 'add first'
			List<Property> properties = Reflections.at((input.model.getClass()))
					.properties();
			properties = properties.stream().collect(Collectors.toList());
			Collections.reverse(properties);
			for (Property property : properties) {
				HasAnnotations directedProperty = input.resolver
						.resolveDirectedProperty(property);
				if (directedProperty != null) {
					Object childModel = property.get(input.model);
					input.enqueueInput(input.resolver, childModel,
							new AnnotationLocation(input.model.getClass(),
									property, input.resolver),
							null, input.node, false);
				}
			}
		}
	}

	interface GeneratesTransformModel {
		default Object transformModel(RendererInput input, Object model) {
			Directed.Transform args = input.location
					.getAnnotation(Directed.Transform.class);
			if (args == null) {
				return model;
			}
			if (model == null && !args.transformsNull()) {
				// no output
				return null;
			}
			ModelTransform transform = (ModelTransform) Reflections
					.newInstance(args.value());
			// FIXME - dirndl 1x1 - can this be slimmed down? Since it allows
			// access to parent
			if (transform instanceof ContextSensitiveTransform) {
				((ContextSensitiveTransform) transform)
						.withContextNode(input.node);
			}
			Object transformedModel = transform.apply(model);
			return transformedModel;
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
			Widget widget = node.rendered.widget;
			node.rendered.widget = new SimpleWidget(tag);
			applyCssClass(node, widget);
		}
	}
}

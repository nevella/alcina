package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.ContextSensitiveTransform;

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
@Reflected
public abstract class DirectedRenderer {
	protected void applyCssClass(Node node, Widget widget) {
		if (node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
	}

	protected String getTag(Node node, boolean respectPropertyNameTags,
			String defaultTag) {
		String tag = null;
		if (node.model instanceof HasTag) {
			tag = ((HasTag) node.model).provideTag();
		}
		if (Ax.notBlank(tag)) {
			return tag;
		}
		tag = node.directed.tag();
		if (Ax.notBlank(tag)) {
			return tag;
		}
		if (respectPropertyNameTags) {
			if (node.parent != null && node.parent.has(PropertyNameTags.class)
					&& node.getProperty().getName() != null) {
				return CommonUtils.deInfixCss(node.getProperty().getName());
			}
		}
		if (Ax.notBlank(defaultTag)) {
			return defaultTag;
		}
		return Ax.blankTo(tag,
				CommonUtils.deInfixCss(node.model.getClass().getSimpleName()));
	}

	protected abstract void render(DirectedLayout.RendererInput input);

	@Registration({ DirectedRenderer.class, Bindable.class })
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
	@Registration({ DirectedRenderer.class, AbstractCollection.class })
	public static class Collection extends DirectedRenderer
			implements GeneratesTransformModel {
		@Override
		protected void render(RendererInput input) {
			Preconditions
					.checkArgument(input.model instanceof java.util.Collection);
			// zero widgets for the container, generates input per child
			List list = (List) ((java.util.Collection) input.model).stream()
					.collect(Collectors.toList());
			list.forEach(model -> {
				Object transformedModel = transformModel(input, model);
				// the @Directed for the collection element is merge (the input
				// @Directed :: the element's merged hierarchy @Directed)
				AnnotationLocation location = input.location
						.copyWithClassLocationOf(transformedModel);
				// FIXME - *definitely* optimise. Possibly Directed.Impl should
				// be only one instance per attribute permutation
				//
				// what we really want is to pass a consumed/modified arg here
				//
				// note that input.soleDirected() is really merge
				// (last(location.propertyDirected),
				// last(collectionClass.classDirected))
				//
				// but for a collection its class annotations (listeners etc)
				// should
				// be on the wrapper - in fact, probably just disallow @Directed
				// on Collection subclasses
				//
				// actually no - but we'll need Directed.transformPhase
				//
				// probably get it working, step back and fix
				location.resolvedPropertyAnnotations = Arrays
						.asList(input.soleDirected());
				input.enqueueInput(input.resolver, transformedModel, location,
						// force resolution
						null, input.node);
			});
		}
	}

	public static class Container extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			Node node = input.node;
			String tag = getTag(node, false, null);
			FlowPanel widget = new FlowPanel(tag);
			node.widget = widget;
			applyCssClass(node, widget);
		}
	}

	public static class Delegating extends DirectedRenderer
			implements GeneratesPropertyInputs {
		@Override
		protected void render(RendererInput input) {
			Preconditions.checkArgument(input.model instanceof Bindable);
			// NOOP, no container widget/node
			generatePropertyInputs(input);
		}
	}

	/*
	 * Indicates that the annotation/resolution chain does not define a
	 * renderer. Fall back on the model class
	 */
	public static class ModelClass extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 *
	 *
	 * @author nick@alcina.cc
	 *
	 */
	/*
	 * Note the difference to cDirectedRenderer.Collection is really that by
	 * default the @Directed generated for each collection element is *all* of
	 * the incoming directed (tag, class, bindings, evetns), wheras for
	 * TransformRenderer:
	 *
	 * tag, cssClass are passed down
	 *
	 * bindings[] must be empty
	 *
	 * renderer is ModelClassNodeRenderer
	 *
	 * events (emits(), receives(), reemits()) are not passed down (are
	 * registered on the non-transformed node)
	 *
	 */
	public static class TransformRenderer extends DirectedRenderer
			implements GeneratesTransformModel {
		@Override
		protected void render(RendererInput input) {
			Object transformedModel = transformModel(input, input.model);
			AnnotationLocation location = input.location
					.copyWithClassLocationOf(transformedModel);
			// FIXME - *definitely* optimise. Possibly Directed.Impl should
			// be only one instance per attribute permutation
			//
			// what we really want is to pass a consumed/modified arg here
			//
			// probably get it working, step back and fix
			//
			// note that input.soleDirected() is really merge
			// (last(location.propertyDirected),
			// last(collectionClass.classDirected))
			//
			// but for a collection its class annotations (listeners etc) should
			// be on the wrapper - in fact, probably just disallow @Directed on
			// Collection subclasses
			//
			// actually no - but we'll need Directed.transformPhase
			//
			// will merge to transformed
			//
			// note the special case when input.model == transformedModel
			Impl descendantResolvedPropertyAnnotation = Directed.Impl
					.wrap(input.soleDirected());
			descendantResolvedPropertyAnnotation.setRenderer(ModelClass.class);
			if (transformedModel == input.model) {
				// preserve all other attributes
			} else {
				Preconditions.checkArgument(descendantResolvedPropertyAnnotation
						.bindings().length == 0);
				descendantResolvedPropertyAnnotation
						.setEmits(Impl.EMPTY_CLASS_ARRAY);
				descendantResolvedPropertyAnnotation
						.setReceives(Impl.EMPTY_CLASS_ARRAY);
				descendantResolvedPropertyAnnotation
						.setReemits(Impl.EMPTY_CLASS_ARRAY);
			}
			location.resolvedPropertyAnnotations = Arrays
					.asList(descendantResolvedPropertyAnnotation);
			location.addConsumed(
					input.location.getAnnotation(Directed.Transform.class));
			input.enqueueInput(input.resolver, transformedModel, location, null,
					input.node);
		}
	}

	/**
	 * Transitional. Allows the model/directedlayout system access to (to wrap)
	 * a widget
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Registration({ DirectedRenderer.class, Widget.class })
	public static class WidgetRenderer extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			input.node.widget = (Widget) input.model;
		}
	}

	interface GeneratesPropertyInputs {
		default void generatePropertyInputs(RendererInput input) {
			List<Property> properties = Reflections.at((input.model.getClass()))
					.properties();
			properties = properties.stream().collect(Collectors.toList());
			for (Property property : properties) {
				HasAnnotations directedProperty = input.resolver
						.resolveDirectedProperty(property);
				if (directedProperty != null) {
					Object childModel = property.get(input.model);
					// add input even if childModel==null
					Class locationType = childModel == null ? void.class
							: childModel.getClass();
					input.enqueueInput(input.resolver, childModel,
							new AnnotationLocation(locationType, property,
									input.resolver),
							null, input.node);
				}
			}
		}
	}

	interface GeneratesTransformModel {
		default Object transformModel(RendererInput input, Object model) {
			Directed.Transform transform = input.location
					.getAnnotation(Directed.Transform.class);
			if (transform == null) {
				return model;
			}
			if (model == null && !transform.transformsNull()) {
				// null output
				return null;
			}
			ModelTransform modelTransform = (ModelTransform) Reflections
					.newInstance(transform.value());
			// FIXME - dirndl 1x1b - can this be slimmed down? Since it allows
			// access to the whole parent chain, not just the parent
			if (modelTransform instanceof ContextSensitiveTransform) {
				((ContextSensitiveTransform) modelTransform)
						.withContextNode(input.node);
			}
			Object transformedModel = modelTransform.apply(model);
			return transformedModel;
		}
	}
}

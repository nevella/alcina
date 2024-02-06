package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.AbstractCollection;
import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.HtmlDefaultTags;
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
 * <h2>Wrinkles</h2>
 * <p>
 * To prevent recursive/multiple application of Transform, the algorithm needs
 * to track the originating model of a Node, and apply a Transform at most once
 * per originating model
 *
 */
@Reflected
public abstract class DirectedRenderer {
	public static String defaultGetTag(Node node, String defaultTag) {
		String tag = null;
		/*
		 * Only apply HasTag (tag from model) to the last (deepest) node for the
		 * model
		 */
		if (node.model instanceof HasTag && node.lastForModel) {
			tag = ((HasTag) node.model).provideTag();
		}
		if (Ax.notBlank(tag)) {
			return tag;
		}
		tag = node.directed.tag();
		if (Ax.notBlank(tag)) {
			return tag;
		}
		// for models which have default tags (leaf models such as String,
		// Boolean, Enum), compute the tag
		// - property name by default, but optionally SPAN etc
		//
		// for models without default tags (e.g. Model subclasses), always use
		// the tag name unless the model implements Directed.NonClassTag
		if (Ax.notBlank(defaultTag)) {
			if ((node.parent != null && node.parent.has(HtmlDefaultTags.class))
					|| node.getProperty() == null) {
				return defaultTag;
			} else {
				return Ax.cssify(node.getProperty().getName());
			}
		} else {
			if (node.model instanceof Directed.NonClassTag
					&& node.getProperty() != null) {
				return Ax.cssify(node.getProperty().getName());
			} else {
				return tagName(node.model.getClass());
			}
		}
	}

	public static String getTag(String directedTag, Class modelClass) {
		return Ax.blankTo(directedTag, tagName(modelClass));
	}

	public static String tagName(Class clazz) {
		return Ax.cssify(clazz.getSimpleName());
	}

	protected void applyCssClass(Node node, Element element) {
		String cssClass = node.directed.className();
		if (cssClass.length() > 0) {
			element.addStyleName(cssClass);
		}
	}

	protected String getTag(Node node, String defaultTag) {
		return defaultGetTag(node, defaultTag);
	}

	protected abstract void render(DirectedLayout.RendererInput input);

	public cc.alcina.framework.common.client.dom.DomNodeType rendersAsType() {
		return cc.alcina.framework.common.client.dom.DomNodeType.ELEMENT;
	}

	/**
	 * Renders a container widget for the Bindable instance and layout nodes for
	 * the properties
	 *
	 *
	 *
	 */
	@Registration({ DirectedRenderer.class, Bindable.class })
	public static class BindableRenderer extends DirectedRenderer
			implements GeneratesPropertyInputs {
		@Override
		protected void render(RendererInput input) {
			Node node = input.node;
			String tag = getTag(node, null);
			node.resolver.renderElement(node, tag);
			generatePropertyInputs(input);
		}
	}

	/**
	 * The most-specific @Directed at the initiating AnnotationLocation will be
	 * applied to each Collection element. This renderer renders no widget for
	 * the collection itself, normally there will be one widget (or at least
	 * dirndl node) per collection element
	 *
	 *
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
			((java.util.Collection) input.model).forEach(model -> {
				Object transformedModel = transformModel(input, model, true);
				// the @Directed for the collection element is merge (the input
				// @Directed :: the element's merged hierarchy @Directed)
				AnnotationLocation location = input.location
						.copyWithClassLocationOf(transformedModel);
				/*
				 * FIXME - dirndl 1x1g - phase - *definitely* optimise. Possibly
				 * Directed.Impl should be only one instance per attribute
				 * permutation
				 *
				 * in 1x3, finalise the handling of resolved annotations etc -
				 * see AnnotationLocation.ResolutionState. Honestly, might want
				 * to check with a profiler that performance is *that* bad - the
				 * resolution code is executed for every collection member, but
				 * it's not that vast (that said, check with a 5,000 element
				 * collection...)
				 *
				 * optimise (possibly with per-resolver Type/Property nodes) in
				 * 1x1g if necessary
				 *
				 * what we really want is to pass a consumed/modified arg here
				 *
				 * note that input.soleDirected() is really merge
				 * (last(location.propertyDirected),
				 * last(collectionClass.classDirected))
				 *
				 * but for a collection its class annotations (listeners etc)
				 * should be on the wrapper - in fact, probably just
				 * disallow @Directed on Collection subclasses
				 *
				 * actually no - but we'll need Directed.transformPhase
				 *
				 * probably get it working, step back and fix
				 *
				 */
				location.resolutionState.resolvedPropertyAnnotations = Arrays
						.asList(input.soleDirected());
				// inelegant, but works to avoid double-transform
				location.resolutionState.addConsumed(input.location
						.getAnnotation(Directed.TransformElements.class));
				input.enqueueInput(input.resolver, transformedModel, location,
						// force resolution
						null, input.node);
			});
		}
	}

	/**
	 * Renders a container widget (dom element, by default a div)
	 *
	 *
	 *
	 */
	public static class Container extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			Node node = input.node;
			String tag = getTag(node, "div");
			Preconditions.checkState(tag.length() > 0);
			node.resolver.renderElement(node, tag);
		}
	}

	/**
	 * Renders no widget for the annotated object, but renders the object
	 * properties (so rendering is 'delegated' to the properties)
	 */
	public static class Delegating extends DirectedRenderer
			implements GeneratesPropertyInputs {
		@Override
		protected void render(RendererInput input) {
			Preconditions.checkArgument(input.model instanceof Bindable);
			// NOOP, no container widget/node
			generatePropertyInputs(input);
		}
	}

	interface GeneratesPropertyInputs {
		default void generatePropertyInputs(RendererInput input) {
			for (Property property : Reflections.at((input.model))
					.properties()) {
				Property directedProperty = input.resolver
						.resolveDirectedProperty(property);
				if (directedProperty != null) {
					Object childModel = property.get(input.model);
					// add input even if childModel==null
					Class locationType = childModel == null ? void.class
							: childModel.getClass();
					input.enqueueInput(input.resolver, childModel,
							new AnnotationLocation(locationType,
									directedProperty, input.resolver),
							null, input.node);
				}
			}
		}
	}

	interface GeneratesTransformModel {
		default Object transformModel(RendererInput input, Object model,
				boolean collectionElements) {
			Directed.Transform transform = collectionElements ? null
					: input.location.getAnnotation(Directed.Transform.class);
			Directed.TransformElements transformElements = collectionElements
					? input.location
							.getAnnotation(Directed.TransformElements.class)
					: null;
			if (transform == null && transformElements == null) {
				return model;
			}
			if (transform != null) {
				if (model == null && !transform.transformsNull()) {
					// null output
					return null;
				}
				ModelTransform modelTransform = (ModelTransform) Reflections
						.newInstance(transform.value());
				if (modelTransform instanceof ContextSensitiveTransform) {
					((ContextSensitiveTransform) modelTransform)
							.withContextNode(input.node);
				}
				Object transformedModel = modelTransform.apply(model);
				return transformedModel;
			} else {
				if (model == null && !transformElements.transformsNull()) {
					// null output
					return null;
				}
				ModelTransform modelTransform = (ModelTransform) Reflections
						.newInstance(transformElements.value());
				if (modelTransform instanceof ContextSensitiveTransform) {
					((ContextSensitiveTransform) modelTransform)
							.withContextNode(input.node);
				}
				Object transformedModel = modelTransform.apply(model);
				return transformedModel;
			}
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

	/*
	 * Renderer will attempt to render null
	 */
	interface RendersNull {
	}

	/**
	 *
	 *
	 *
	 *
	 */
	/*
	 * Note the difference to DirectedRenderer.Collection is really that by
	 * default the @Directed generated for each collection element is *all* of
	 * the incoming directed (tag, class, bindings, events), wheras for
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
			implements GeneratesTransformModel, RendersNull {
		@Override
		protected void render(RendererInput input) {
			Object transformedModel = transformModel(input, input.model, false);
			if (transformedModel == null) {
				return;
			}
			AnnotationLocation location = input.location
					.copyWithClassLocationOf(transformedModel);
			// FIXME - dirndl 1x1g - *definitely* optimise. Possibly
			// Directed.Impl should
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
			// note the special case when input.model == transformedModel -
			// assumes current directed.bindToModel=false
			Impl descendantResolvedPropertyAnnotation = Directed.Impl
					.wrap(input.soleDirected());
			descendantResolvedPropertyAnnotation.setRenderer(ModelClass.class);
			if (transformedModel == input.model) {
				descendantResolvedPropertyAnnotation.setBindToModel(true);
				// preserve all other attributes
			} else {
				Preconditions.checkArgument(descendantResolvedPropertyAnnotation
						.bindings().length == 0);
				descendantResolvedPropertyAnnotation
						.setEmits(CommonUtils.EMPTY_CLASS_ARRAY);
				descendantResolvedPropertyAnnotation
						.setReemits(CommonUtils.EMPTY_CLASS_ARRAY);
			}
			location.resolutionState.resolvedPropertyAnnotations = Arrays
					.asList(descendantResolvedPropertyAnnotation);
			location.resolutionState.addConsumed(
					input.location.getAnnotation(Directed.Transform.class));
			input.enqueueInput(input.resolver, transformedModel, location, null,
					input.node);
		}
	}

	/**
	 * Transitional. Allows the model/directedlayout system access to (to wrap)
	 * a widget
	 *
	 *
	 *
	 */
	@Registration({ DirectedRenderer.class, Widget.class })
	public static class WidgetRenderer extends DirectedRenderer {
		@Override
		protected void render(RendererInput input) {
			Widget model = (Widget) input.model;
			input.resolver.linkRenderedObject(input.node, input.model);
		}
	}
}

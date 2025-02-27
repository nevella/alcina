package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Transform;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

/**
 * <p>
 * Renders a form/table cell *either* as a legacy GWT widget, or as a Directed
 * node/model
 *
 * <p>
 * The directed transformation is relatively complex - it retains a parent
 * reference in the resolver until after child 'rendered' generation - since it
 * needs the parent ref to set up the binding (GWT attach worked via
 * child-before-parent, Dirndl is parent-before-child).
 *
 * <p>
 * The transformation also changes the annotation resolution location to that of
 * the Bean.Property that's being rendered so - if rendering a
 * Croissaint.filling in a table or form, the declarative resolution (render
 * customization) comes from there, not the generating table/form structure.
 * This allows concise rendering customization
 * 
 * <p>
 * The dirndl sequence is ::
 * <ol>
 * <li>Input[loc: FormElement.value, model: ValueModel, resolver: ?] -&gt;
 * <li>Input[loc: [ValueModel.Bindable].[ValueModel.Field.Property], model:
 * ValueModel.Bindable, resolver: ValueResolver]
 * </ol>
 *
 * <p>
 * The ValueResolver does the magic in the onBeforeRender, in terms of handling
 * the late setup of the bean/rendered model binding
 *
 * @author nick@alcina.cc
 *
 */
public class BridgingValueRenderer extends DirectedRenderer {
	static NodeEditorContext getEditorContext(Node node) {
		NodeEditorContext.Has contextSource = (NodeEditorContext.Has) node.resolver;
		NodeEditorContext editorContext = contextSource.getNodeEditorContext();
		return editorContext;
	}

	Node node;

	NodeEditorContext editorContext;

	ValueModel valueModel;

	Field field;

	SourcesPropertyChangeEvents target;

	FormModel formModel;

	boolean renderAsNodeEditors;

	boolean editable;

	RendererInput input;

	protected void customizeWidget(Widget widget) {
		// subclass customization
	}

	@Override
	protected void render(RendererInput input) {
		this.input = input;
		node = input.node;
		editorContext = getEditorContext(node);
		valueModel = (ValueModel) node.getModel();
		field = valueModel.getField();
		target = null;
		ContextResolver resolver = node.getResolver();
		if (resolver instanceof FormModel.Has) {
			formModel = ((FormModel.Has) resolver).getFormModel();
		}
		renderAsNodeEditors = editorContext.isRenderAsNodeEditors();
		editable = editorContext.isEditable();
		if (renderAsNodeEditors) {
			renderModel();
		} else {
			renderWidget();
		}
	}

	void renderModel() {
		ValueResolver valueResolver = new ValueResolver(this);
		AnnotationLocation valueLocation = new AnnotationLocation(void.class,
				field.getProperty(), valueResolver);
		valueResolver.valueLocation = valueLocation;
		input.enqueueInput(valueResolver, null, null, valueLocation, null,
				node);
	}

	void renderWidget() {
		BoundWidget<?> boundWidget = field.getCellProvider().get();
		boundWidget.setModel(valueModel.getBindable());
		if (field.getComparator() != null) {
			boundWidget.setComparator(field.getComparator());
		}
		Binding binding = setupBinding(boundWidget);
		boundWidget.setAction(new BasicBindingAction().withBinding(binding));
		Widget widget = (Widget) boundWidget;
		customizeWidget(widget);
		node.getResolver().linkRenderedObject(node, widget);
	}

	Binding setupBinding(SourcesPropertyChangeEvents target) {
		Binding binding = new Binding(target, "value", field.getValidator(),
				field.getFeedback(), valueModel.getBindable(),
				field.getPropertyName(), field.provideReverseValidator(), null);
		if (field.getConverter() != null) {
			binding.getRight().converter = field.getConverter();
		}
		Converter inverseConverter = BeanFields
				.getInverseConverter(field.getConverter());
		if (inverseConverter != null) {
			binding.getLeft().converter = inverseConverter;
		}
		binding.setDetached(editorContext.isDetached());
		valueModel.onChildBindingCreated(binding);
		return binding;
	}

	public static class RenderingModelTransform
			extends AbstractContextSensitiveModelTransform<Object, Model> {
		@Override
		public Model apply(Object t) {
			NodeEditorContext editorContext = getEditorContext(node);
			Property property = ((ValueResolver) node.resolver).valueLocation.property;
			Class marker = editorContext.isEditable() ? FormModel.Editor.class
					: FormModel.Viewer.class;
			return Registry.impl(Model.Value.class, marker, property.getType());
		}
	}

	/**
	 * Applies the transformation rules to generate binding models, which will
	 * then be bound to the form
	 * 
	 * Note that this system doesn't currently allow @Directed annotations with
	 * a strategy to be applied to bean properties
	 * (e.g. @Directed.TransformElements) - that would require yet another
	 * transformation layer
	 *
	 */
	static class ValueResolver extends ContextResolver
			implements NodeEditorContext.Has {
		AnnotationLocation valueLocation;

		BridgingValueRenderer renderer;
		// retained after renderer is nulled

		Field field;

		ValueResolver(BridgingValueRenderer renderer) {
			// this ref will be removed after child binding
			this.renderer = renderer;
			this.field = renderer.field;
			init(renderer.node);
			this.parent = renderer.input.resolver;
			annotationResolver = parent.annotationResolver;
			bindingsCache = parent.bindingsCache;
		}

		@Override
		protected MultikeyMap<List<? extends Annotation>> resolvedCache() {
			return field.getSharedAnnotationResolver().resolvedCache();
		}

		@Override
		public synchronized <A extends Annotation> List<A> resolveAnnotations(
				Class<A> annotationClass, AnnotationLocation location) {
			AnnotationLocation sharedLocation = location
					.copyWithResolver(field.getSharedAnnotationResolver());
			return (List<A>) resolvedCache().ensure(() -> {
				return resolveAnnotations0(annotationClass, location);
			}, sharedLocation, annotationClass);
		}

		@Override
		protected BindingsCache bindingsCache() {
			return field.getSharedAnnotationResolver().bindingsCache();
		}

		@Override
		public <A extends Annotation> A contextAnnotation(
				HasAnnotations reflector, Class<A> clazz,
				ResolutionContext resolutionContext) {
			A contextAnnotation = super.contextAnnotation(reflector, clazz,
					resolutionContext);
			if (clazz == Directed.Transform.class) {
				// assume this is not circular
				// if (reflector == valueLocation.property) {
				/*
				 * because of de-duping in
				 * AbstractMergeStrategy.resolveProperty(, the check needs to be
				 * more elaborate
				 */
				if (reflector.isOrIsPropertyAncestor(valueLocation.property)) {
					if (contextAnnotation == null) {
						Directed.Transform.Impl impl = new Directed.Transform.Impl();
						impl.withValue(RenderingModelTransform.class);
						impl.withTransformsNull(true);
						contextAnnotation = (A) impl;
					} else {
						/*
						 * The transform result must implement HasValue, and
						 * listen on changes (it will not be populated on
						 * initial render)
						 * 
						 * Note that this current requires the ModelTransform to
						 * be the first generic declaration - so:
						 * 
						 * CampaignSignupHtml extends Model.Fields implements
						 * ModelTransform<Campaign, CampaignSignupHtml>,
						 * HasValue<Campaign> {
						 * 
						 * not
						 * 
						 * CampaignSignupHtml extends Model.Fields implements
						 * 
						 * HasValue<Campaign>,ModelTransform<Campaign,
						 * CampaignSignupHtml> {
						 */
						Directed.Transform transform = (Transform) contextAnnotation;
						List<Class> bounds = Reflections.at(transform.value())
								.getGenericBounds().bounds;
						/*
						 * if you want to say transform a table column - say
						 * date to string - use a @ValueTransformer, not
						 * a @Directed.Transformer
						 */
						Preconditions.checkState(Reflections.isAssignableFrom(
								HasValue.class, bounds.get(1)));
						/*
						 * Restate the annotation with transformsNull:=true
						 */
						Directed.Transform.Impl impl = new Directed.Transform.Impl();
						impl.withValue(transform.value());
						impl.withTransformsNull(true);
						contextAnnotation = (A) impl;
					}
				}
			}
			/*
			 * This is none-too-clean (it requires knowledge that
			 * DelegatingValue will be the transformed rendering of the bean
			 * property/Field), but allows transferral of @Directed annotations
			 * from the bean properties to the renderer
			 */
			if (clazz == Directed.class || clazz == Directed.Exclude.class) {
				if (reflector instanceof Property && ((Property) reflector)
						.getOwningType() == DelegatingValue.class) {
					A directAnnotation = super.contextAnnotation(
							valueLocation.property, clazz, resolutionContext);
					if (directAnnotation != null) {
						contextAnnotation = directAnnotation;
					}
					A parentAnnotation = parent.contextAnnotation(
							valueLocation.property, clazz, resolutionContext);
					if (parentAnnotation != null) {
						contextAnnotation = parentAnnotation;
					}
				}
			}
			return contextAnnotation;
		}

		@Override
		public NodeEditorContext getNodeEditorContext() {
			return new Context();
		}

		@Override
		protected void initCaches() {
			// delegates to parent caches
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			Node node = event.getContext().node;
			// skip the transform node, bind the transform child to the
			// valueModel
			if (node.parent != null
					&& node.parent.annotationLocation == valueLocation) {
				/*
				 * The input model (has Field and Bindable refs)
				 */
				ValueModel inputModel = (ValueModel) renderer.input.model;
				/*
				 * The rendered model. No need to verify it implements HasValue
				 * (that's checked in
				 * BridgingValueRenderer.ValueResolver.contextAnnotation above)
				 */
				Model displayModel = (Model) event.model;
				/*
				 *
				 */
				Binding binding = renderer.setupBinding(displayModel);
				displayModel.bindings().addBinding(binding);
				renderer.input = null;
			}
		}

		class Context implements NodeEditorContext {
			@Override
			public Property getEditingProperty() {
				return field.getProperty();
			}

			@Override
			public boolean isEditable() {
				return parentContext().isEditable() && field.isEditable();
			}

			@Override
			public boolean isRenderAsNodeEditors() {
				return parentContext().isRenderAsNodeEditors();
			}

			public NodeEditorContext parentContext() {
				return ((NodeEditorContext.Has) ValueResolver.this.parent)
						.getNodeEditorContext();
			}
		}
	}
}
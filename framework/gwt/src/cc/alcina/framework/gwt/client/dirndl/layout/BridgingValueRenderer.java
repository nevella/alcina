package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Transform.Impl;
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
 * customization) comes from there, not the geneerating table/form structure.
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
		formModel = ((FormModel.Has) node.getResolver()).getFormModel();
		renderAsNodeEditors = editorContext.isRenderAsNodeEditors();
		editable = formModel != null && formModel.getState().editable;
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
		input.enqueueInput(valueResolver, null, valueLocation, null, node);
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
				field.getPropertyName(), null, null);
		if (field.getConverter() != null) {
			binding.getRight().converter = field.getConverter();
		}
		Converter inverseConverter = BeanFields
				.getInverseConverter(field.getConverter());
		if (inverseConverter != null) {
			binding.getLeft().converter = inverseConverter;
		}
		formModel.getState().formBinding.getChildren().add(binding);
		return binding;
	}

	public static class RenderingModelTransform extends
			AbstractContextSensitiveModelTransform<Object, Model.Value> {
		@Override
		public Model.Value apply(Object t) {
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
		public <A extends Annotation> A contextAnnotation(
				HasAnnotations reflector, Class<A> clazz,
				ResolutionContext resolutionContext) {
			A contextAnnotation = super.contextAnnotation(reflector, clazz,
					resolutionContext);
			if (clazz == Directed.Transform.class) {
				// assume this is not circular
				if (reflector == valueLocation.property) {
					// transform IS legal, but will require another layer of
					// resolution
					Preconditions.checkState(contextAnnotation == null);
					Impl impl = new Directed.Transform.Impl();
					impl.withValue(RenderingModelTransform.class);
					impl.withTransformsNull(true);
					contextAnnotation = (A) impl;
				}
			}
			return contextAnnotation;
		}

		@Override
		public NodeEditorContext getNodeEditorContext() {
			return new Context();
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			Node node = event.getContext().node;
			// skip the transform node, bind the transform child to the
			// valueModel
			if (node.parent.annotationLocation == valueLocation) {
				/*
				 * The input model (has Field and Bindinable refs)
				 */
				ValueModel inputModel = (ValueModel) renderer.input.model;
				/*
				 * The rendered model
				 */
				Model.Value displayModel = (Model.Value) event.model;
				/*
				 *
				 */
				Binding binding = renderer.setupBinding(displayModel);
				displayModel.bindings().addBinding(binding);
				renderer.input = null;
			}
		}

		@Override
		protected void initCaches() {
			// delegates to parent caches
		}

		class Context implements NodeEditorContext {
			@Override
			public Property getEditingProperty() {
				return field.getProperty();
			}

			@Override
			public boolean isEditable() {
				return parentContext().isEditable();
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
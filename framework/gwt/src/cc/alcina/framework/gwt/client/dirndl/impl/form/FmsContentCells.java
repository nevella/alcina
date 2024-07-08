package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.validator.ValidationFeedback;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;

public class FmsContentCells {
	// FIXME - dirndl 1x2 - remove
	public static class FmsCellsContextResolver extends FmsContextResolver
			implements FormModel.Has, NodeEditorContext.Has {
		@Override
		public FormModel getFormModel() {
			return getRootModel() instanceof FormModel ? getRootModel() : null;
		}

		@Override
		public NodeEditorContext getNodeEditorContext() {
			return getRootModel();
		}

		@Override
		public <T> T resolveRenderContextProperty(String key) {
			if (key == RenderContext.VALIDATION_FEEDBACK_SUPPLIER) {
				return (T) new FmsValidationFeedbackSupplier();
			}
			return (T) super.resolveRenderContextProperty(key);
		}

		public static class DisplayAllMixin extends FmsCellsContextResolver {
			@Override
			protected <A extends Annotation> List<A> resolveAnnotations0(
					Class<A> annotationClass, AnnotationLocation location) {
				List<A> mixinResult = DisplayAllPropertiesIfNoneExplicitlySet.Mixin
						.resolveAnnotations0(annotationClass, location);
				return mixinResult != null ? mixinResult
						: super.resolveAnnotations0(annotationClass, location);
			}
		}
	}

	public static class FmsValidationFeedbackSupplier
			implements Function<String, ValidationFeedback> {
		@Override
		public ValidationFeedback apply(String position) {
			FmsValidationFeedback validationFeedback = new FmsValidationFeedback();
			return validationFeedback;
		}
	}

	@Directed(renderer = FmsValueRenderer.class)
	public static class FmsValueModel extends Model implements ValueModel {
		public ValueModel delegate;

		public FmsValueModel() {
		}

		public FmsValueModel(ValueModel valueModel) {
			delegate = valueModel;
		}

		@Override
		public Bindable getBindable() {
			return this.delegate.getBindable();
		}

		@Override
		public Field getField() {
			return delegate.getField();
		}

		@Override
		public String getGroupName() {
			return this.delegate.getGroupName();
		}

		@Override
		public String toString() {
			return Ax.format("Field: %s :: Value: %s", getField(),
					getBindable());
		}

		@Override
		public void onChildBindingCreated(Binding binding) {
			delegate.onChildBindingCreated(binding);
		}
	}
}

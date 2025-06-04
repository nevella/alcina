package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.validator.ValidationFeedback;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsTable.FmsTableColumn;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsTable.FmsTreeTableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreeTable;

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

		/**
		 * Don't return FmsTableCell/FmsTableColumn, since the container is not
		 * a table
		 */
		public static class TreeTableResolver extends FmsContextResolver {
			@Override
			protected boolean returnTableCellElements() {
				return false;
			}

			/*
			 * location of AbstractNode.contents
			 */
			AnnotationLocation contentsLocation;

			@Override
			protected <A extends Annotation> List<A> resolveAnnotations0(
					Class<A> annotationClass, AnnotationLocation location) {
				Class incomingClass = location.classLocation;
				Class modifiedClass = location.classLocation;
				AnnotationLocation outgoingLocation = location;
				if (contentsLocation == null && location.property != null) {
					if (Reflections.isAssignableFrom(Tree.TreeNode.class,
							location.property.getDeclaringType())
							&& Objects.equals(location.property.getName(),
									Tree.TreeNode.properties.contents.name())) {
						contentsLocation = location;
					}
				}
				if (Objects.equals(location, contentsLocation)) {
					modifiedClass = FmsTable.FmsTreeTableRow.class;
				}
				if (incomingClass == TableModel.TableColumn.class) {
					modifiedClass = FmsTreeTableColumn.class;
				}
				if (modifiedClass != null) {
					outgoingLocation = new AnnotationLocation(modifiedClass,
							location.property, this);
					// note that we need to preserve the parent resolution state
					outgoingLocation.setResolutionState(
							location.ensureResolutionState());
				}
				return super.resolveAnnotations0(annotationClass,
						outgoingLocation);
			}

			@Override
			protected Property resolveDirectedProperty0(Property property) {
				Property override = null;
				if (property.getOwningType() == TableModel.TableRow.class) {
					override = Reflections.at(FmsTable.FmsTreeTableRow.class)
							.property(property.getName());
				}
				if (property.getOwningType() == TableModel.TableColumn.class) {
					override = Reflections.at(FmsTable.FmsTreeTableColumn.class)
							.property(property.getName());
				}
				if (override != null) {
					property = override;
				}
				return super.resolveDirectedProperty0(property);
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

	/*
	 * Never instantiated
	 */
	@Directed(renderer = FmsValueRenderer.class)
	public static class FmsValueModel extends Model implements ValueModel {
		public ValueModel delegate;

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

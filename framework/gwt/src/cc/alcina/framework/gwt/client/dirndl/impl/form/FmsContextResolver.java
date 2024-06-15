package cc.alcina.framework.gwt.client.dirndl.impl.form;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsValueModel;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm.FmsFormElement;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsForm.FmsLabelModel;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsTable.FmsTableCell;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsTable.FmsTableColumn;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsTable.FmsTableModel;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormElement;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.LabelModel;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableCell;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableValueModel;

// FIXME - dirndl 1x2 - probably remove
public class FmsContextResolver extends ContextResolver {
	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		// FIXME - jdk.17 - switch
		Class incomingClass = location.classLocation;
		Class modifiedClass = location.classLocation;
		AnnotationLocation outgoingLocation = location;
		if (incomingClass == TableModel.class) {
			modifiedClass = FmsTableModel.class;
		} else if (incomingClass == TableColumn.class) {
			modifiedClass = FmsTableColumn.class;
		} else if (incomingClass == FormElement.class) {
			modifiedClass = FmsFormElement.class;
		} else if (incomingClass == LabelModel.class) {
			modifiedClass = FmsLabelModel.class;
		} else if (incomingClass == TableCell.class) {
			modifiedClass = FmsTableCell.class;
		} else if (incomingClass == FormValueModel.class) {
			modifiedClass = FmsValueModel.class;
		} else if (incomingClass == TableValueModel.class) {
			modifiedClass = FmsValueModel.class;
		}
		if (modifiedClass != null) {
			outgoingLocation = new AnnotationLocation(modifiedClass,
					location.property, this);
			// note that we need to preserve the parent resolution state
			outgoingLocation.resolutionState = location.resolutionState;
		}
		return resolveAnnotations0Super(annotationClass, outgoingLocation);
	}

	protected <A extends Annotation> List<A> resolveAnnotations0Super(
			Class<A> annotationClass, AnnotationLocation location) {
		return super.resolveAnnotations0(annotationClass, location);
	}

	@Override
	protected Property resolveDirectedProperty0(Property property) {
		Property override = null;
		if (property.getOwningType() == TableModel.class) {
			override = Reflections.at(FmsTableModel.class)
					.property(property.getName());
		}
		if (property.getOwningType() == TableColumn.class) {
			override = Reflections.at(FmsTableColumn.class)
					.property(property.getName());
		}
		if (property.getOwningType() == TableCell.class) {
			override = Reflections.at(FmsTableCell.class)
					.property(property.getName());
		}
		if (override != null) {
			property = override;
		}
		return super.resolveDirectedProperty0(property);
	}

	public static class FmsContextResolverNonButtonLinks
			extends FmsContextResolver {
		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			if (location.property != null
					&& location.property.has(annotationClass)) {
				return resolveAnnotations0Super(annotationClass, location);
			}
			if (location.classLocation == Link.class) {
				return super.resolveAnnotations0Super(annotationClass,
						location);
			}
			return super.resolveAnnotations0(annotationClass, location);
		}
	}
}

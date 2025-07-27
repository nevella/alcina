package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;

public class DelegatingContextResolver extends ContextResolver {
	ContextResolver logicalParent;

	public DelegatingContextResolver() {
	}

	public DelegatingContextResolver(ContextResolver logicalParent) {
		this.logicalParent = logicalParent;
	}

	@Override
	protected void init(ContextResolver parent, DirectedLayout layout,
			Object rootModel) {
		super.init(parent, layout, rootModel);
		if (logicalParent != null) {
			this.parent = logicalParent;
		}
	}

	@Override
	public synchronized <A extends Annotation> List<A> resolveAnnotations(
			Class<A> annotationClass, AnnotationLocation location) {
		return parent.resolveAnnotations(annotationClass, location);
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		return parent.resolveAnnotations0(annotationClass, location);
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations1(
			Class<A> annotationClass, AnnotationLocation location) {
		return parent.resolveAnnotations1(annotationClass, location);
	}

	@Override
	Property resolveDirectedProperty(Property property) {
		return parent.resolveDirectedProperty(property);
	}

	@Override
	protected Property resolveDirectedProperty0(Property property) {
		return parent.resolveDirectedProperty0(property);
	}

	@Override
	protected Object resolveModel(AnnotationLocation location, Object model) {
		return parent.resolveModel(location, model);
	}

	@Override
	protected Class resolveLocationClass(AnnotationLocation location) {
		return parent.resolveLocationClass(location);
	}
}
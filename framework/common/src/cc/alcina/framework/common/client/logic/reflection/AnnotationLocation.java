package cc.alcina.framework.common.client.logic.reflection;

public class AnnotationLocation {
	public PropertyReflector propertyReflector;

	public Class containingClass;

	public AnnotationLocation(PropertyReflector propertyReflector,
			Class containingClass) {
		this.propertyReflector = propertyReflector;
		this.containingClass = containingClass;
	}
}
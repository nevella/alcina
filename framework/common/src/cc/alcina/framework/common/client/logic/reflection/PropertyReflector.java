package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

public interface PropertyReflector {
	public abstract <A extends Annotation> A
			getAnnotation(Class<A> annotationClass);

	public abstract String getPropertyName();

	public abstract Class getPropertyType();

	public abstract Object getPropertyValue(Object bean);

	public abstract void setPropertyValue(Object bean, Object newValue);

	public static class Location {
		public PropertyReflector propertyReflector;

		public Class containingClass;

		public Location(PropertyReflector propertyReflector,
				Class containingClass) {
			this.propertyReflector = propertyReflector;
			this.containingClass = containingClass;
		}
	}
}

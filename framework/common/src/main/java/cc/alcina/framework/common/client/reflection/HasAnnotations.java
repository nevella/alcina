package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

public interface HasAnnotations {
	<A extends Annotation> A annotation(Class<A> annotationClass);

	default <A extends Annotation> Optional<A>
			annotationOptional(Class<A> annotationClass) {
		return Optional.ofNullable(annotation(annotationClass));
	}

	<A extends Annotation> List<A> annotations(Class<A> annotationClass);

	/**
	 * 
	 * @param owningType
	 * @param propertyName
	 * @return true if the type is a PropertyReflector and is the reflector for
	 *         owningType.propertyName
	 */
	boolean isProperty(Class<?> owningType, String propertyName);

	default Property asProperty() {
		return (Property) this;
	}

	/**
	 * 
	 * @param locationClass
	 * @param propertyName
	 * @return true if the type is a ClassReflector amd is the reflector for
	 *         locationClass
	 */
	boolean isClass(Class locationClass, String propertyName);

	default boolean isOrIsPropertyAncestor(Property property) {
		return false;
	}
}

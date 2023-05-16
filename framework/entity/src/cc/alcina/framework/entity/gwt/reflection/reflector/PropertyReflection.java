package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesJavaType;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesTypeBounds;

/**
 * FIXME - reflection - as per policy, should not expose public fields as
 * mutable unless they *are* mutable (use getters or final)
 */
public class PropertyReflection extends ReflectionElement
		implements Comparable<PropertyReflection> {
	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	public final String name;

	public PropertyMethod getter;

	public PropertyMethod setter;

	public JType propertyType;

	private ReflectionVisibility reflectionVisibility;

	public final ClassReflection classReflection;

	public JClassType declaringType;

	private ProvidesTypeBounds providesTypeBounds;

	public List<? extends JClassType> jtypeBounds;

	public PropertyReflection(ClassReflection classReflection, String name,
			ReflectionVisibility reflectionVisibility,
			ProvidesTypeBounds providesTypeBounds) {
		this.classReflection = classReflection;
		this.name = name;
		this.reflectionVisibility = reflectionVisibility;
		this.providesTypeBounds = providesTypeBounds;
	}

	/*
	 * ignore methods if they're overridden by existing getter/setter
	 */
	public void addMethod(PropertyMethod method) {
		if (method.getter) {
			if (getter != null && method.method.getEnclosingType()
					.isAssignableFrom(getter.method.getEnclosingType())) {
				return;
			}
			getter = method;
			updatePropertyType(method.method.getReturnType());
		} else {
			if (setter != null && method.method.getEnclosingType()
					.isAssignableFrom(setter.method.getEnclosingType())) {
				return;
			}
			setter = method;
			updatePropertyType(method.method.getParameters()[0].getType());
		}
		propertyType = ClassReflection.erase(propertyType);
	}

	public Property asProperty() {
		List<Class> jdkBounds = jtypeBounds.stream()
				.map(t -> ((ProvidesJavaType) t).provideJavaType())
				.collect(Collectors.toList());
		TypeBounds typeBounds = new TypeBounds(jdkBounds);
		return new Property(name, ProvidesMethod.asMethod(getter),
				ProvidesMethod.asMethod(setter),
				((ProvidesJavaType) propertyType).provideJavaType(),
				((ProvidesJavaType) classReflection.type).provideJavaType(),
				((ProvidesJavaType) declaringType).provideJavaType(),
				typeBounds, new AnnotationProviderImpl());
	}

	@Override
	public int compareTo(PropertyReflection o) {
		return name.compareTo(o.name);
	}

	public List<AnnotationReflection> getAnnotationReflections() {
		return this.annotationReflections;
	}

	public String getName() {
		return this.name;
	}

	public boolean isSerializable() {
		return getter != null && setter != null
				&& !getter.method.isAnnotationPresent(AlcinaTransient.class);
	}

	@Override
	public void prepare() {
		annotationReflections = getter == null ? new ArrayList<>()
				: Arrays.stream(getter.method.getAnnotations())
						.filter(ann -> reflectionVisibility
								.isVisibleAnnotation(ann.annotationType()))
						.map(AnnotationReflection::new).sorted()
						.collect(Collectors.toList());
		declaringType = getter != null ? getter.method.getEnclosingType()
				: setter.method.getEnclosingType();
	}

	@Override
	public String toString() {
		JClassType declaringType = getter != null
				? getter.method.getEnclosingType()
				: setter.method.getEnclosingType();
		return Ax.format("%s.%s : %s", declaringType.getName(), name,
				propertyType.getSimpleSourceName());
	}

	private void updatePropertyType(JType type) {
		JType erased = ClassReflection.erase(type);
		if (this.propertyType != null
				&& this.propertyType instanceof JClassType) {
			JClassType existingClassType = (JClassType) this.propertyType;
			JClassType candidateClassType = (JClassType) erased;
			if (candidateClassType.isAssignableFrom(existingClassType)) {
				return;// covariant, do not update
			}
		}
		if (type instanceof JClassType) {
			jtypeBounds = providesTypeBounds
					.provideTypeBounds((JClassType) type);
		} else {
			jtypeBounds = List.of();
		}
		this.propertyType = erased;
	}

	public static class PropertyMethod {
		String propertyName;

		public boolean getter;

		public JMethod method;

		public JField field;

		PropertyMethod(String propertyName, boolean getter, JField field) {
			this.propertyName = propertyName;
			this.getter = getter;
			this.field = field;
		}

		PropertyMethod(String propertyName, boolean getter, JMethod method) {
			this.propertyName = propertyName;
			this.getter = getter;
			this.method = method;
		}

		@Override
		public String toString() {
			return method.toString();
		}
	}

	public interface ProvidesMethod {
		static Method asMethod(PropertyMethod propertyMethod) {
			if (propertyMethod == null) {
				return null;
			} else {
				return ((ProvidesMethod) propertyMethod.method).provideMethod();
			}
		}

		Method provideMethod();
	}

	class AnnotationProviderImpl implements AnnotationProvider {
		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return getter == null ? null
					: getter.method.getAnnotation(annotationClass);
		}
	}
}

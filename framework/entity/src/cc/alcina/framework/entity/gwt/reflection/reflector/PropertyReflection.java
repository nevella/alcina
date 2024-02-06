package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public PropertyAccessor getter;

	public PropertyAccessor setter;

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
	 * ignore methods (or field set/get) if they're overridden by existing
	 * getter/setter
	 */
	public void addMethod(PropertyAccessor method) {
		if (method.getter) {
			if (method.isOverriddenBy(getter)) {
				return;
			}
			getter = method;
			updatePropertyType(method.getPropertyType());
		} else {
			if (method.isOverriddenBy(setter)) {
				return;
			}
			setter = method;
			updatePropertyType(method.getPropertyType());
		}
		propertyType = ClassReflection.erase(propertyType);
	}

	public Property asProperty() {
		List<Class> jdkBounds = jtypeBounds.stream()
				.map(t -> ((ProvidesJavaType) t).provideJavaType())
				.collect(Collectors.toList());
		TypeBounds typeBounds = new TypeBounds(jdkBounds);
		return new Property(name, ProvidesPropertyMethod.asMethod(getter),
				ProvidesPropertyMethod.asMethod(setter),
				((ProvidesJavaType) propertyType).provideJavaType(),
				((ProvidesJavaType) classReflection.type).provideJavaType(),
				((ProvidesJavaType) declaringType).provideJavaType(),
				typeBounds, new AnnotationProviderImpl());
	}

	@Override
	public int compareTo(PropertyReflection o) {
		return name.compareTo(o.name);
	}

	public Stream<Class> getAnnotationAttributeTypes() {
		return annotationReflections.stream()
				.flatMap(AnnotationReflection::getAnnotationAttributeTypes);
	}

	public List<AnnotationReflection> getAnnotationReflections() {
		return this.annotationReflections;
	}

	public String getName() {
		return this.name;
	}

	public JType getPropertyType() {
		return this.propertyType;
	}

	public boolean has(Class<? extends Annotation> clazz) {
		return getter == null ? false : getter.has(clazz);
	}

	public boolean isSerializable() {
		return getter != null && setter != null
				&& !getter.has(AlcinaTransient.class);
	}

	@Override
	public void prepare() {
		annotationReflections = getter == null ? new ArrayList<>()
				: Arrays.stream(getter.getAnnotations())
						.filter(ann -> reflectionVisibility
								.isVisibleAnnotation(ann.annotationType()))
						.map(AnnotationReflection::new).sorted()
						.collect(Collectors.toList());
		declaringType = getter != null ? getter.getEnclosingType()
				: setter.getEnclosingType();
	}

	@Override
	public String toString() {
		JClassType declaringType = getter != null ? getter.getEnclosingType()
				: setter.getEnclosingType();
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

	class AnnotationProviderImpl implements AnnotationProvider {
		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return getter == null ? null
					: getter.getAnnotation(annotationClass);
		}

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			return getter == null ? List.of()
					: (List<A>) getter.getAnnotations(annotationClass);
		}
	}

	public static abstract class PropertyAccessor
			implements ProvidesPropertyMethod {
		String propertyName;

		public boolean getter;

		boolean firePropertyChangeEvents;

		PropertyAccessor(String propertyName, boolean getter,
				boolean firePropertyChangeEvents) {
			this.propertyName = propertyName;
			this.getter = getter;
			this.firePropertyChangeEvents = firePropertyChangeEvents;
		}

		protected abstract <A extends Annotation> A
				getAnnotation(Class<A> annotationClass);

		public abstract Annotation[] getAnnotations();

		protected abstract <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass);

		public abstract JClassType getEnclosingType();

		public abstract String getName();

		public abstract JType getPropertyType();

		public boolean has(Class<? extends Annotation> annotationClass) {
			return getAnnotation(annotationClass) != null;
		}

		protected abstract boolean isOverriddenBy(PropertyAccessor getter2);

		public static class Field extends PropertyAccessor {
			JField field;

			Field(JField field, boolean getter,
					boolean firePropertyChangeEvents) {
				super(field.getName(), getter, firePropertyChangeEvents);
				this.field = field;
			}

			@Override
			protected <A extends Annotation> A
					getAnnotation(Class<A> annotationClass) {
				return field.getAnnotation(annotationClass);
			}

			@Override
			public Annotation[] getAnnotations() {
				return field.getAnnotations();
			}

			@Override
			protected <A extends Annotation> List<A>
					getAnnotations(Class<A> annotationClass) {
				return field.getAnnotations(annotationClass);
			}

			@Override
			public JClassType getEnclosingType() {
				return field.getEnclosingType();
			}

			@Override
			public String getName() {
				return field.getName();
			}

			@Override
			public JType getPropertyType() {
				return field.getType();
			}

			@Override
			protected boolean isOverriddenBy(PropertyAccessor test) {
				return test != null;
			}

			@Override
			public cc.alcina.framework.common.client.reflection.Method
					providePropertyMethod(boolean getter,
							boolean firePropertyChangeEvents) {
				return ((ProvidesPropertyMethod) field).providePropertyMethod(
						getter, firePropertyChangeEvents);
			}
		}

		public static class Method extends PropertyAccessor {
			JMethod method;

			Method(String propertyName, boolean getter, JMethod method) {
				super(propertyName, getter, false);
				this.method = method;
			}

			@Override
			protected <A extends Annotation> A
					getAnnotation(Class<A> annotationClass) {
				return method.getAnnotation(annotationClass);
			}

			@Override
			public Annotation[] getAnnotations() {
				return method.getAnnotations();
			}

			@Override
			protected <A extends Annotation> List<A>
					getAnnotations(Class<A> annotationClass) {
				return method.getAnnotations(annotationClass);
			}

			@Override
			public JClassType getEnclosingType() {
				return method.getEnclosingType();
			}

			@Override
			public String getName() {
				return method.getName();
			}

			@Override
			public JType getPropertyType() {
				if (getter) {
					return method.getReturnType();
				} else {
					return method.getParameters()[0].getType();
				}
			}

			@Override
			protected boolean isOverriddenBy(PropertyAccessor test) {
				return test != null && getEnclosingType()
						.isAssignableFrom(test.getEnclosingType());
			}

			@Override
			public cc.alcina.framework.common.client.reflection.Method
					providePropertyMethod(boolean getter,
							boolean firePropertyChangeEvents) {
				return ((ProvidesPropertyMethod) method).providePropertyMethod(
						getter, firePropertyChangeEvents);
			}
		}
	}

	public interface ProvidesPropertyMethod {
		static cc.alcina.framework.common.client.reflection.Method
				asMethod(PropertyAccessor accessor) {
			if (accessor == null) {
				return null;
			} else {
				return accessor.providePropertyMethod(accessor.getter,
						accessor.firePropertyChangeEvents);
			}
		}

		Method providePropertyMethod(boolean getter,
				boolean providePropertyMethod);
	}
}

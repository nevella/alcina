package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
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
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JTypeParameter;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesJavaType;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesTypeBounds;

/**
 * FIXME - reflection - as per policy, should not expose public fields as
 * mutable unless they *are* mutable (use getters or final)
 */
public class PropertyReflection extends ReflectionElement
		implements Comparable<PropertyReflection> {
	/*
	 * return -1 is type1 is assignable from type2 (less specific), 1 if type1
	 * is assignable to type2 (more specific), 0 otherwise
	 */
	static int computeSpecicifity(JType type1, JType type2) {
		JClassType clazz1 = type1.isClassOrInterface();
		JClassType clazz2 = type2.isClassOrInterface();
		/*
		 * only compare class types
		 */
		if (clazz1 == null || clazz2 == null) {
			return 0;
		}
		if (clazz1 instanceof JTypeParameter) {
			if (clazz2 instanceof JTypeParameter) {
				return 0;
			} else {
				return -1;
			}
		}
		if (clazz2 instanceof JTypeParameter) {
			return 1;
		}
		if (clazz1 == clazz2) {
			return 0;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return -1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return 1;
		}
		return 0;
	}

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

	List<PropertyAccessor.Field> addedFieldGetterMethods = new ArrayList<>();

	List<PropertyAccessor.Field> addedFieldSetterMethods = new ArrayList<>();

	/*
	 * ignore methods (or field set/get) if they're contravariant to the
	 * existing getter/setter (i.e. it existing has a more specific
	 * parameter/return type )
	 */
	public void addMethod(PropertyAccessor method) {
		if (method instanceof PropertyAccessor.Field) {
			PropertyAccessor.Field fieldAccessor = (PropertyAccessor.Field) method;
			List<PropertyAccessor.Field> addedFieldMethods = method.getter
					? addedFieldGetterMethods
					: addedFieldSetterMethods;
			if (addedFieldMethods.size() > 0) {
				if (addedFieldMethods.contains(fieldAccessor)) {
					/*
					 * TODO - fix addition of identical accessor
					 */
					return;
				}
				String message = Ax.format(
						"Duplicate (overriding/shadowed) field accessors:\n\t%s - %s\n\t%s - %s",
						addedFieldMethods.get(0),
						addedFieldMethods.get(0).getEnclosingType(),
						fieldAccessor, fieldAccessor.getEnclosingType());
				throw new IllegalStateException(message);
			}
			addedFieldMethods.add(fieldAccessor);
		}
		if (method.getter) {
			if (method.isContravariantTo(getter)) {
				return;
			}
			getter = method;
			updatePropertyType(method.getPropertyType());
		} else {
			if (method.isContravariantTo(setter)) {
				return;
			}
			PropertyAccessor oldSetter = setter;
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
		if (type instanceof JClassType) {
			List<? extends JClassType> updatedBounds = providesTypeBounds
					.provideTypeBounds((JClassType) type);
			if (jtypeBounds != null && jtypeBounds.size() > 0
					&& updatedBounds.size() > 0) {
				if (jtypeBounds.size() == 1 && updatedBounds.size() == 1) {
					JClassType genCurrent = jtypeBounds.get(0);
					JClassType genUpdate = updatedBounds.get(0);
					if (genCurrent != genUpdate) {
						int specicifity = computeSpecicifity(genCurrent,
								genUpdate);
						if (specicifity >= 0) {
							// TODO - there's definitely a prettier way to do
							// this
							// (mismatch between getter and setter??)
							return;
						}
					}
				}
			}
		}
		if (this.propertyType != null
				&& this.propertyType instanceof JClassType) {
			JClassType existingClassType = (JClassType) this.propertyType;
			JClassType candidateClassType = (JClassType) erased;
			int specicifity = computeSpecicifity(existingClassType,
					candidateClassType);
			if (specicifity >= 0) {
				return;// covariant, do not update. in particular type
						// parameters are less specific than any real type
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

		protected abstract boolean isContravariantTo(PropertyAccessor getter2);

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
			protected boolean isContravariantTo(PropertyAccessor test) {
				/*
				 * There should only be one Field accessor per type (the
				 * precondition checks that) - the logic states 'any Method
				 * accessor will override the Field accessor'
				 */
				Preconditions.checkState(!(test instanceof Field));
				return test != null;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Field) {
					Field o = (Field) obj;
					return CommonUtils.equals(field, o.field, getter, o.getter);
				} else {
					return false;
				}
			}

			@Override
			public int hashCode() {
				return Objects.hash(field, getter);
			}

			@Override
			public cc.alcina.framework.common.client.reflection.Method
					providePropertyMethod(boolean getter,
							boolean firePropertyChangeEvents) {
				return ((ProvidesPropertyMethod) field).providePropertyMethod(
						getter, firePropertyChangeEvents);
			}

			@Override
			public String toString() {
				return Ax.format("Field (accessor) :: %s", field);
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
			protected boolean isContravariantTo(PropertyAccessor test) {
				// this test works for GWT typemodel but not JDK
				// return test != null && getEnclosingType()
				// .isAssignableFrom(test.getEnclosingType());
				if (test == null) {
					return false;
				}
				if (test instanceof Field) {
					return false;
				}
				Method otherMethod = (Method) test;
				/*
				 * compare return types, then parameter types (last must be
				 * equal length arrays), then enclosing types
				 */
				int returnTypeSpecicifity = computeSpecicifity(
						method.getReturnType(),
						otherMethod.method.getReturnType());
				if (returnTypeSpecicifity != 0) {
					if (returnTypeSpecicifity == -1) {
						return true;
					} else {
						return false;
					}
				}
				for (int idx = 0; idx < method.getParameters().length; idx++) {
					int parameterSpecicifity = computeSpecicifity(
							method.getParameters()[idx].getType(),
							otherMethod.method.getParameters()[idx].getType());
					if (parameterSpecicifity != 0) {
						if (parameterSpecicifity == -1) {
							return true;
						} else {
							return false;
						}
					}
				}
				int enclosingTypeSpecicifity = computeSpecicifity(
						getEnclosingType(), otherMethod.getEnclosingType());
				if (enclosingTypeSpecicifity != 0) {
					if (enclosingTypeSpecicifity == -1) {
						return true;
					} else {
						return false;
					}
				}
				/*
				 * Unable to determine which method wins (should never be hit)
				 */
				// throw new IllegalArgumentException();
				return false;
			}

			@Override
			public cc.alcina.framework.common.client.reflection.Method
					providePropertyMethod(boolean getter,
							boolean firePropertyChangeEvents) {
				return ((ProvidesPropertyMethod) method).providePropertyMethod(
						getter, firePropertyChangeEvents);
			}

			@Override
			public String toString() {
				return Ax.format("MethodAccessor: %s", method);
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

package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesAssignableTo;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesInterfaces;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesJavaType;

public abstract class JClassType
		implements com.google.gwt.core.ext.typeinfo.JClassType,
		ProvidesAssignableTo, ProvidesInterfaces, ProvidesJavaType {
	Class clazz;

	Members members;

	TypeOracle typeOracle;

	private JPackage jPackage;

	private int modifierBits;

	public JClassType(TypeOracle typeOracle, Class clazz) {
		this.typeOracle = typeOracle;
		this.clazz = clazz;
		this.modifierBits = clazz.getModifiers();
		this.jPackage = typeOracle.findPackage(clazz.getPackageName());
	}

	@Override
	public JParameterizedType asParameterizationOf(JGenericType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Annotation> T
			findAnnotationInTypeHierarchy(Class<T> annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JConstructor findConstructor(JType[] paramTypes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JField findField(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JMethod findMethod(String name, JType[] paramTypes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType findNestedType(String typeName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return (T) clazz.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return clazz.getAnnotations();
	}

	@Override
	public JConstructor getConstructor(JType[] paramTypes)
			throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JConstructor[] getConstructors() {
		return ensureMembers().constructors
				.toArray(new JConstructor[members.constructors.size()]);
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getEnclosingType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract JClassType getErasedType();

	@Override
	public JField getField(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JField[] getFields() {
		return ensureMembers().fields
				.toArray(new JField[members.fields.size()]);
	}

	@Override
	public Set<? extends JClassType> getFlattenedSupertypeHierarchy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getImplementedInterfaces() {
		return ensureMembers().implementedInterfaces
				.toArray(new JClassType[members.implementedInterfaces.size()]);
	}

	@Override
	public JMethod[] getInheritableMethods() {
		return ensureMembers().inheritableMethods
				.toArray(new JMethod[members.inheritableMethods.size()]);
	}

	@Override
	public String getJNISignature() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JType getLeafType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JMethod getMethod(String name, JType[] paramTypes)
			throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JMethod[] getMethods() {
		return ensureMembers().methods
				.toArray(new JMethod[members.methods.size()]);
	}

	@Override
	public String getName() {
		return clazz.getSimpleName();
	}

	@Override
	public JClassType getNestedType(String typeName) throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getNestedTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeOracle getOracle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JMethod[] getOverloads(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JMethod[] getOverridableMethods() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPackage getPackage() {
		return jPackage;
	}

	@Override
	public String getParameterizedQualifiedSourceName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQualifiedBinaryName() {
		return clazz.getName();
	}

	@Override
	public String getQualifiedSourceName() {
		String canonicalName = clazz.getCanonicalName();
		return canonicalName != null ? canonicalName : clazz.getName();
	}

	@Override
	public String getSimpleSourceName() {
		return clazz.getSimpleName();
	}

	@Override
	public JClassType[] getSubtypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getSuperclass() {
		return typeOracle.getType(clazz.getSuperclass());
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(modifierBits);
	}

	@Override
	public JAnnotationType isAnnotation() {
		return null;
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return clazz.isAnnotationPresent(annotationClass);
	}

	@Override
	public JArrayType isArray() {
		return null;
	}

	@Override
	public boolean isAssignableFrom(
			com.google.gwt.core.ext.typeinfo.JClassType possibleSubtype) {
		return clazz.isAssignableFrom(((JClassType) possibleSubtype).clazz);
	}

	@Override
	public boolean isAssignableTo(
			com.google.gwt.core.ext.typeinfo.JClassType possibleSupertype) {
		return ((JClassType) possibleSupertype).clazz.isAssignableFrom(clazz);
	}

	@Override
	public JClassType isClass() {
		return this;
	}

	@Override
	public JClassType isClassOrInterface() {
		return this;
	}

	@Override
	public boolean isDefaultInstantiable() {
		throw new UnsupportedOperationException();
		// return false;
	}

	@Override
	public boolean isEnhanced() {
		return false;
	}

	@Override
	public JEnumType isEnum() {
		return null;
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(modifierBits);
	}

	@Override
	public JGenericType isGenericType() {
		return null;
	}

	@Override
	public JClassType isInterface() {
		if (clazz.isInterface()) {
			return this;
		} else {
			return null;
		}
	}

	@Override
	public boolean isMemberType() {
		return clazz.getEnclosingClass() != null;
	}

	@Override
	public boolean isPackageProtected() {
		return !(Modifier.isPrivate(modifierBits)
				|| Modifier.isPublic(modifierBits)
				|| Modifier.isProtected(modifierBits));
	}

	@Override
	public JParameterizedType isParameterized() {
		return null;
	}

	@Override
	public JPrimitiveType isPrimitive() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPrivate() {
		return Modifier.isPrivate(modifierBits);
	}

	@Override
	public boolean isProtected() {
		return Modifier.isProtected(modifierBits);
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(modifierBits);
	}

	@Override
	public JRawType isRawType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(modifierBits);
	}

	@Override
	public JTypeParameter isTypeParameter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JWildcardType isWildcard() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Predicate<Class> provideAssignableTo() {
		return test -> clazz.isAssignableFrom(test);
	}

	@Override
	public List<Class> provideInterfaces() {
		return Arrays.asList(clazz.getInterfaces());
	}

	@Override
	public Class provideJavaType() {
		return clazz;
	}

	@Override
	public void setEnhanced() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return getQualifiedSourceName();
	}

	private Members ensureMembers() {
		if (members == null) {
			members = new Members();
		}
		return members;
	}

	class Members {
		List<JField> fields;

		List<JMethod> methods;

		List<JMethod> inheritableMethods;

		List<JConstructor> constructors;

		List<JClassType> implementedInterfaces;

		Members() {
			fields = Arrays.stream(clazz.getDeclaredFields())
					.map(f -> new JField(typeOracle, f))
					.collect(Collectors.toList());
			methods = Arrays.stream(clazz.getDeclaredMethods())
					.map(m -> new JMethod(typeOracle, m))
					.collect(Collectors.toList());
			constructors = Arrays.stream(clazz.getDeclaredConstructors())
					.map(c -> new JConstructor(typeOracle, c))
					.collect(Collectors.toList());
			implementedInterfaces = Arrays.stream(clazz.getInterfaces())
					.map(typeOracle::getType).collect(Collectors.toList());
			inheritableMethods = new ArrayList<>();
			JClassType cursor = JClassType.this;
			while (cursor != null) {
				Members members = cursor == JClassType.this ? this
						: cursor.ensureMembers();
				// as per JClassType.getInheritableMethods javadoc, only retain
				// the most-derived (i.e. subclass overides super)
				members.methods.stream().filter(m -> !m.isPrivate())
						.filter(candidateMethod -> !inheritableMethods.stream()
								.anyMatch(
										existingMethod -> sameCallingSignature(
												existingMethod,
												candidateMethod)))
						.forEach(inheritableMethods::add);
				cursor = cursor.getSuperclass();
			}
		}

		boolean sameCallingSignature(JMethod m1, JMethod m2) {
			if (!Objects.equals(m1.getName(), m2.getName())) {
				return false;
			}
			if (!Objects.equals(m1.getParameters().length,
					m2.getParameters().length)) {
				return false;
			}
			for (int idx = 0; idx < m1.getParameters().length; idx++) {
				JParameter p1 = m1.getParameters()[idx];
				JParameter p2 = m2.getParameters()[idx];
				if (p1.getType() != p2.getType()) {
					return false;
				}
			}
			// only allow exact (not covariant) matches
			return m1.getReturnType() == m2.getReturnType();
		}
	}
}

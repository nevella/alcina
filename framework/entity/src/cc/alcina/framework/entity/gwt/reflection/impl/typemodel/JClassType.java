package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesAssignableTo;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesInterfaces;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesJavaType;

public abstract class JClassType<T extends Type>
		implements com.google.gwt.core.ext.typeinfo.JClassType,
		ProvidesAssignableTo, ProvidesInterfaces, ProvidesJavaType {
	T type;

	Class clazz;

	Members members;

	TypeOracle typeOracle;

	private JPackage jPackage;

	private int modifierBits;

	public JClassType(TypeOracle typeOracle, T type) {
		this.typeOracle = typeOracle;
		this.type = type;
		if (type instanceof Class) {
			this.clazz = (Class) type;
		} else if (type instanceof ParameterizedType) {
			this.clazz = (Class) ((ParameterizedType) type).getRawType();
		}
		if (clazz != null) {
			this.modifierBits = clazz.getModifiers();
			this.jPackage = typeOracle
					.findPackage(Reflections.getPackageName(clazz));
		}
	}

	@Override
	public JParameterizedType asParameterizationOf(JGenericType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> A
			findAnnotationInTypeHierarchy(Class<A> annotationType) {
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
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return (A) clazz.getAnnotation(annotationClass);
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

	public JField[] getInheritableFields() {
		return ensureMembers().inheritableFields
				.toArray(new JField[members.inheritableFields.size()]);
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
		return typeOracle.getType(clazz.getGenericSuperclass());
	}

	public JClassType getType(Class jdkType) {
		return typeOracle.getType(jdkType);
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
		return test -> test.isAssignableFrom(clazz);
	}

	@Override
	public List<Class> provideInterfaces() {
		return Arrays.asList(clazz.getInterfaces());
	}

	@Override
	public Class provideJavaType() {
		return clazz;
	}

	/*
	 * Initially, only simple (direct generic superclass) bounds for types
	 */
	public TypeBounds provideJdkTypeBounds() {
		if (clazz == null) {
			return new TypeBounds(List.of());
		} else {
			return ensureMembers().typeBounds;
		}
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

		List<JField> inheritableFields;

		List<JConstructor> constructors;

		List<JClassType> implementedInterfaces;

		List<JClassType> resolvedSuperclassTypes;

		TypeParameterResolution resolution;

		TypeBounds typeBounds;

		Members() {
			resolution = new TypeParameterResolution(JClassType.this);
			fields = Arrays.stream(clazz.getDeclaredFields())
					.map(f -> new JField(typeOracle, f))
					.collect(Collectors.toList());
			if (TypeOracle.reverseFieldOrder) {
				// android
				Collections.reverse(fields);
			}
			methods = Arrays.stream(clazz.getDeclaredMethods())
					.map(m -> new JMethod(typeOracle, m))
					.collect(Collectors.toList());
			constructors = Arrays.stream(clazz.getDeclaredConstructors())
					.map(c -> new JConstructor(typeOracle, c))
					.collect(Collectors.toList());
			implementedInterfaces = Arrays.stream(clazz.getInterfaces())
					.map(typeOracle::getType).collect(Collectors.toList());
			computeBounds();
			/*
			 * populate inheritable fields/methods - these will have type
			 * parameters resolved if possible
			 */
			inheritableMethods = new ArrayList<>();
			inheritableFields = new ArrayList<>();
			JClassType cursor = JClassType.this;
			// split out into three loops - first construct type resolution,
			// then ensure methods (but only for erased types if superclass - to
			// avoid unnecessary JParameterizedType construction)
			while (cursor != null) {
				resolution.addResolution(cursor);
				cursor = cursor.getSuperclass();
			}
			cursor = JClassType.this;
			while (cursor != null) {
				Members members = cursor == JClassType.this ? this
						: cursor.getErasedType().ensureMembers();
				/*
				 * as per JClassType.getInheritableMethods javadoc, only retain
				 * the most-derived (i.e. subclass overides super)
				 *
				 * the stream calls resolution::resolve to replace the inherited
				 * method with a possibly specialised version, determined by
				 * type paremeter resolution
				 */
				List<JMethod> inheritableMembers = getDirectInheritableMethods(
						members.methods);
				inheritableMembers.forEach(inheritableMethods::add);
				members.fields.stream().filter(m -> !m.isPrivate())
						.map(resolution::resolve)
						.forEach(inheritableFields::add);
				cursor = cursor.getSuperclass();
			}
		}

		@Override
		public String toString() {
			return Ax.format("Members: %s", JClassType.this.toString());
		}

		private List<JMethod>
				getDirectInheritableMethods(List<JMethod> methods) {
			Multimap<NameCallingSignature, List<JMethod>> candidates = methods
					.stream().filter(m -> !m.isPrivate())
					.map(resolution::resolve).collect(AlcinaCollectors
							.toKeyMultimap(NameCallingSignature::new));
			return candidates.values().stream().map(this::findMostSpecific)
					.collect(Collectors.toList());
		}

		boolean assignableComparable(JClassType o1, JClassType o2) {
			if (o1.isAssignableFrom(o2)) {
				return true;
			} else if (o2.isAssignableFrom(o1)) {
				return true;
			} else {
				return false;
			}
		}

		/*
		 * This implementation assumes only one nearest generic ancestore
		 * (extends A<B> or implements C<D>)) - if there are multiple, it will
		 * return no bounds
		 * 
		 */
		void computeBounds() {
			List<Class> bounds = new ArrayList<>();
			// find the nearest ParameterizedType ancestor of type (including
			// self)
			ParameterizedType nearestGenericSupertype = null;
			Type cursor = type;
			while (cursor != null) {
				if (cursor instanceof ParameterizedType) {
					nearestGenericSupertype = (ParameterizedType) cursor;
					break;
				}
				if (cursor instanceof Class) {
					Class classCursor = (Class) cursor;
					cursor = null;
					Type superType = classCursor.getGenericSuperclass();
					ParameterizedType parameterizedSuperclass = superType instanceof ParameterizedType
							? (ParameterizedType) superType
							: null;
					List<ParameterizedType> parameterizedInterfaces = Arrays
							.stream(classCursor.getGenericInterfaces())
							.filter(intf -> intf instanceof ParameterizedType)
							.map(intf -> (ParameterizedType) intf)
							.collect(Collectors.toList());
					if (parameterizedSuperclass == null) {
						if (parameterizedInterfaces.size() == 0) {
							cursor = superType;
						} else if (parameterizedInterfaces.size() == 1) {
							cursor = parameterizedInterfaces.get(0);
						} else {
							// invalid, >1 generic superInterfaces
							break;
						}
					} else {
						if (parameterizedInterfaces.size() == 0) {
							cursor = superType;
						} else {
							// invalid, type has a generic superclass && >=1
							// generic superInterfaces
						}
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
			if (nearestGenericSupertype != null) {
				Arrays.stream(nearestGenericSupertype.getActualTypeArguments())
						.map(typeOracle::getType).map(resolution::resolve)
						.map(JClassType::getErasedType)
						.map(JClassType::provideJavaType)
						// confirm that it's a Class clazz (although
						// getErasedType probably forces this).
						//
						// maybe here because I like peek...
						.peek(c -> Preconditions
								.checkArgument(c instanceof Class))
						.forEach(bounds::add);
			}
			typeBounds = new TypeBounds(bounds);
		}

		// FIXME - reflection - revisit - a formal definition of what this
		// enforces (rather than 'property types work') would be good... but
		// probably a chunk of work.
		JMethod findMostSpecific(List<JMethod> methods) {
			if (methods.size() == 1) {
				return methods.get(0);
			} else {
				boolean orderable = true;
				for (int idx1 = 0; idx1 < methods.size(); idx1++) {
					JMethod m1 = methods.get(idx1);
					for (int idx2 = 0; idx2 < methods.size(); idx2++) {
						JMethod m2 = methods.get(idx2);
						if (!assignableComparable(
								(JClassType) m1.getReturnType().getErasedType(),
								(JClassType) m2.getReturnType()
										.getErasedType())) {
							orderable = false;
							break;
						}
					}
				}
				if (orderable) {
					Collections.sort(methods,
							new ReturnTypeAssignableComparator());
				}
				return Ax.last(methods);
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

	static class NameCallingSignature {
		private JMethod method;

		private int hash;

		NameCallingSignature(JMethod method) {
			this.method = method;
			this.hash = Objects.hash(method.getName(),
					Arrays.hashCode(method.getParameters()));
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NameCallingSignature) {
				NameCallingSignature o = (NameCallingSignature) obj;
				return Objects.equals(method.getName(), o.method.getName())
						&& Arrays.equals(method.getParameterTypes(),
								o.method.getParameterTypes());
			} else {
				return super.equals(obj);
			}
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	static class ReturnTypeAssignableComparator implements Comparator<JMethod> {
		TypeAssignableComparator typeAssignableComparator = new TypeAssignableComparator();

		@Override
		public int compare(JMethod o1, JMethod o2) {
			return typeAssignableComparator.compare(
					(JClassType) o1.getReturnType().getErasedType(),
					(JClassType) o2.getReturnType().getErasedType());
		}
	}

	static class TypeAssignableComparator implements Comparator<JClassType> {
		@Override
		public int compare(JClassType o1, JClassType o2) {
			Preconditions.checkState(o1 != o2);
			if (o1.isAssignableFrom(o2)) {
				return -1;
			} else if (o2.isAssignableFrom(o1)) {
				return 1;
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	// FIXME - reflection - revisit - a formal definition of what this enforces
	// (rather than 'property types work') would be good... but probably a chunk
	// of work
	//
	//
	static class TypeParameterResolution {
		Map<TypeVariable, Type> resolvedTypeParameters = new LinkedHashMap<>();

		// debug only
		List<JType> resolvedJTypes = new ArrayList<>();

		List<ParameterizedType> resolvedTypes = new ArrayList<>();

		private JClassType jClassType;

		public TypeParameterResolution(JClassType jClassType) {
			this.jClassType = jClassType;
		}

		/*
		 * These will be processed in most-specific to least-specific order, so
		 * will visit the specification (JParaameterizedType) before the
		 * declaration
		 */
		public void addResolution(JClassType jType) {
			resolvedJTypes.add(jType);
			// iterate over generic supertypes, add to resolution map
			Class clazz = jType.clazz;
			if (clazz == null) {
				return;
			}
			resolveIfParameterized(clazz.getGenericSuperclass());
			Stack<Class> hasResolvableInterfaces = new Stack();
			hasResolvableInterfaces.push(clazz);
			while (hasResolvableInterfaces.size() > 0) {
				Class resolvable = hasResolvableInterfaces.pop();
				resolveIfParameterized(resolvable);
				Type[] genericInterfaces = resolvable.getGenericInterfaces();
				for (Type type : genericInterfaces) {
					Class checkableClass = checkableClass(type);
					if (checkableClass != null) {
						hasResolvableInterfaces.push(checkableClass);
					}
				}
			}
		}

		private JClassType resolve(JType jType) {
			JClassType classType = (JClassType) jType;
			Type cursor = ((JClassType) jType).type;
			while (cursor instanceof TypeVariable) {
				Type test = resolvedTypeParameters.get(cursor);
				if (test != null) {
					cursor = test;
				} else {
					break;
				}
			}
			if (cursor != null) {
				return jClassType.typeOracle.getType(cursor);
			} else {
				return classType;
			}
		}

		private void resolveIfParameterized(Type possiblyParameterizedType) {
			if (!(possiblyParameterizedType instanceof ParameterizedType)) {
				return;
			}
			ParameterizedType parameterizedType = (ParameterizedType) possiblyParameterizedType;
			Type[] actualTypeArguments = parameterizedType
					.getActualTypeArguments();
			Class rawType = (Class) parameterizedType.getRawType();
			TypeVariable[] typeParameters = rawType.getTypeParameters();
			Preconditions.checkState(
					actualTypeArguments.length == typeParameters.length);
			for (int idx = 0; idx < actualTypeArguments.length; idx++) {
				resolvedTypeParameters.putIfAbsent(typeParameters[idx],
						actualTypeArguments[idx]);
			}
		}

		Class checkableClass(Type type) {
			if (type instanceof Class) {
				return (Class) type;
			} else if (type instanceof ParameterizedType) {
				return (Class) ((ParameterizedType) type).getRawType();
			} else {
				return null;
			}
		}

		JField resolve(JField field) {
			JType originalType = field.getType();
			JType resolved = resolve(originalType);
			if (resolved != originalType) {
				JField clone = field.clone();
				clone.setType(resolved);
				return clone;
			} else {
				return field;
			}
		}

		JMethod resolve(JMethod method) {
			JType originalReturnType = method.getReturnType();
			JType resolvedReturnType = resolve(originalReturnType);
			List<JType> originalParameterTypes = Arrays
					.stream(method.getParameters()).map(JParameter::getType)
					.collect(Collectors.toList());
			List<JType> resolvedParameterTypes = originalParameterTypes.stream()
					.map(this::resolve).collect(Collectors.toList());
			if (resolvedReturnType != originalReturnType || !Objects
					.equals(originalParameterTypes, resolvedParameterTypes)) {
				JMethod clone = method.clone();
				clone.setReturnType(resolvedReturnType);
				JParameter[] parameters = clone.getParameters();
				for (int idx = 0; idx < parameters.length; idx++) {
					JParameter jParameter = parameters[idx];
					jParameter.setType(resolvedParameterTypes.get(idx));
				}
				return clone;
			} else {
				return method;
			}
		}
	}
}

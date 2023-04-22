package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ClassReflector;

public class TypeOracle extends com.google.gwt.core.ext.typeinfo.TypeOracle {
	public static boolean reverseFieldOrder = false;

	private final Map<String, JPackage> packages = new HashMap<>();

	private final Map<String, JClassType> jclasses = new HashMap<>();

	private final Map<Type, JClassType> jclassesByType = new HashMap<>();

	@Override
	public synchronized JPackage findPackage(String pkgName) {
		return packages.computeIfAbsent(pkgName, name -> new JPackage(pkgName));
	}

	@Override
	public synchronized JClassType findType(String className) {
		JClassType existingValue = jclasses.get(className);
		if (existingValue != null) {
			return existingValue;
		}
		String regex = "(?:(.*?)\\.)?([A-Z].*)";
		Matcher matcher = Pattern.compile(regex).matcher(className);
		boolean matches = matcher.matches();
		if (matches) {
			int idx = className.lastIndexOf(".");
			String pkgName = matcher.group(1);
			String typeName = matcher.group(2);
			return findType(pkgName, typeName);
		} else {
			// default package
			return findType("", className);
		}
	}

	@Override
	public synchronized JClassType findType(String pkgName, String typeName) {
		String binaryClassName = pkgName.isEmpty() ? typeName
				: pkgName + "." + typeName.replace(".", "$");
		try {
			JClassType existingValue = jclasses.get(binaryClassName);
			if (existingValue != null) {
				return existingValue;
			}
			Class<?> clazz = null;
			if (binaryClassName.equals("void")) {
				clazz = void.class;
			} else if (ClassReflector.primitiveClassMap
					.containsKey(binaryClassName)) {
				clazz = ClassReflector.primitiveClassMap.get(binaryClassName);
			} else {
				ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				if (classLoader == null) {
					classLoader = getClass().getClassLoader();
				}
				clazz = classLoader.loadClass(binaryClassName);
			}
			return getType(clazz);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public synchronized JArrayType getArrayType(JType componentType) {
		String className = componentType.getQualifiedSourceName() + "[]";
		return (JArrayType) findType(className);
	}

	@Override
	public synchronized JClassType getJavaLangObject() {
		return findType(Object.class.getName());
	}

	@Override
	public synchronized JPackage getOrCreatePackage(String name) {
		return findPackage(name);
	}

	@Override
	public synchronized JPackage getPackage(String pkgName)
			throws NotFoundException {
		return findPackage(pkgName);
	}

	@Override
	public synchronized JPackage[] getPackages() {
		Collection<JPackage> values = packages.values();
		return (JPackage[]) values.toArray(new JPackage[values.size()]);
	}

	@Override
	public JParameterizedType getParameterizedType(
			com.google.gwt.core.ext.typeinfo.JGenericType genericType,
			com.google.gwt.core.ext.typeinfo.JClassType enclosingType,
			com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JParameterizedType getParameterizedType(
			com.google.gwt.core.ext.typeinfo.JGenericType genericType,
			com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public com.google.gwt.core.ext.typeinfo.JClassType
			getSingleJsoImpl(com.google.gwt.core.ext.typeinfo.JClassType intf) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns an unmodifiable, live view of all interface types that are
	 * implemented by exactly one JSO subtype.
	 */
	@Override
	public Set<? extends JClassType> getSingleJsoImplInterfaces() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JClassType getType(String name)
			throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JClassType getType(String pkgName,
			String topLevelTypeSimpleName) throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JClassType[] getTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JWildcardType getWildcardType(BoundType boundType,
			com.google.gwt.core.ext.typeinfo.JClassType typeBound) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized JType parse(String type) throws TypeOracleException {
		return findType(type);
	}

	synchronized JGenericType ensureGenericType(Class clazzWithTypeParameters) {
		JClassType type = jclassesByType.get(clazzWithTypeParameters);
		if (type == null) {
			JGenericType jGenericType = new JGenericType(this,
					clazzWithTypeParameters);
			type = jGenericType;
			jclassesByType.put(clazzWithTypeParameters, type);
			Type[] typeArguments = clazzWithTypeParameters.getTypeParameters();
			// type definitions can't be recursive, so any parameterized
			// type
			// model generation caused by
			// this mapping will be finite
			JTypeParameter[] parameterizedJTypeArguments = new JTypeParameter[typeArguments.length];
			for (int idx = 0; idx < typeArguments.length; idx++) {
				Type typeArgument = typeArguments[idx];
				parameterizedJTypeArguments[idx] = new JTypeParameter(this,
						typeArgument, idx);
			}
			jGenericType.setTypeParameters(parameterizedJTypeArguments);
		}
		return (JGenericType) type;
	}

	synchronized JParameterizedType
			ensureParameterizedType(ParameterizedType jdkType) {
		JClassType type = jclassesByType.get(jdkType);
		if (type == null) {
			JParameterizedType jParameterizedType = new JParameterizedType(this,
					jdkType);
			type = jParameterizedType;
			jclassesByType.put(jdkType, type);
			Type[] typeArguments = jdkType.getActualTypeArguments();
			// type definitions can't be recursive, so any parameterized
			// type
			// model generation caused by
			// this mapping will be finite
			JClassType[] jTypeArguments = new JClassType[typeArguments.length];
			for (int idx = 0; idx < typeArguments.length; idx++) {
				Type typeArgument = typeArguments[idx];
				JClassType jTypeArgument = getType(typeArgument, idx);
				jTypeArguments[idx] = jTypeArgument;
			}
			jParameterizedType.setTypeArguments(jTypeArguments);
		}
		return (JParameterizedType) type;
	}

	synchronized JWildcardType generateWildcardType(WildcardType jdkType) {
		JClassType type = jclassesByType.get(jdkType);
		if (type != null) {
			return (JWildcardType) type;
		} else {
			return new JWildcardType(this, jdkType);
		}
	}

	JClassType getType(Type jdkType) {
		return getType(jdkType, 0);
	}

	JClassType getType(Type jdkType, int ordinal) {
		if (jdkType == null) {
			return null;
		}
		JClassType type = jclassesByType.get(jdkType);
		if (type == null) {
			if (jdkType instanceof Class) {
				Class clazz = (Class) jdkType;
				String canonicalName = clazz.getCanonicalName();
				String binaryClassName = clazz.getName();
				Preconditions.checkNotNull(canonicalName);
				int rank = 0;
				String unmodifiedClassName = binaryClassName;
				while (binaryClassName.contains("[]")) {
					binaryClassName = binaryClassName.replaceFirst("\\[\\]",
							"");
					rank++;
				}
				if (rank == 0) {
					TypeVariable<?>[] typeVariables = clazz.getTypeParameters();
					if (typeVariables.length == 0) {
						type = new JRealClassType(this, clazz);
					} else {
						type = ensureGenericType(clazz);
					}
				} else {
					// FIXME - reflection - typemodel - doesn't handle generic
					// arrays
					Class<? extends Object> arrayClass = Array
							.newInstance(clazz, rank).getClass();
					type = new JArrayType(this, arrayClass);
				}
				jclasses.put(unmodifiedClassName, type);
			} else if (jdkType instanceof ParameterizedType) {
				type = ensureParameterizedType((ParameterizedType) jdkType);
			} else if (jdkType instanceof WildcardType) {
				type = generateWildcardType((WildcardType) jdkType);
			} else if (jdkType instanceof TypeVariable) {
				type = new JTypeParameter(this, jdkType, ordinal);
			} else if (jdkType instanceof GenericArrayType) {
				// edge case - currently not attempting to provide generic info
				GenericArrayType genericArrayType = (GenericArrayType) jdkType;
				Type genericComponentType = genericArrayType
						.getGenericComponentType();
				Class clazz = simpleBoundType(genericComponentType);
				Class<? extends Object> arrayClass = Array.newInstance(clazz, 1)
						.getClass();
				type = new JArrayType(this, arrayClass);
			} else {
				throw new UnsupportedOperationException();
			}
			jclassesByType.put(jdkType, type);
		}
		return type;
	}

	Class simpleBoundType(Type jdkType) {
		if (jdkType instanceof Class) {
			return (Class) jdkType;
		} else if (jdkType instanceof ParameterizedType) {
			return (Class) ((ParameterizedType) jdkType).getRawType();
		} else if (jdkType instanceof TypeVariable) {
			TypeVariable typeVariable = (TypeVariable) jdkType;
			Type[] bounds = typeVariable.getBounds();
			if (bounds.length == 0) {
				return Object.class;
			} else {
				Type firstBound = bounds[0];
				if (firstBound instanceof Class) {
					return (Class) firstBound;
				} else {
					return simpleBoundType(firstBound);
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}
}

package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;

public class TypeOracle extends com.google.gwt.core.ext.typeinfo.TypeOracle {
	public static boolean reverseFieldOrder = false;

	private final Map<String, JPackage> packages = new HashMap<>();

	private final Map<String, JClassType> jclasses = new HashMap<>();

	private final Map<Class, JClassType> jclassesByClass = new HashMap<>();

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
		String className = pkgName.isEmpty() ? typeName
				: pkgName + "." + typeName.replace(".", "$");
		try {
			JClassType existingValue = jclasses.get(className);
			if (existingValue != null) {
				return existingValue;
			}
			Class<?> clazz = null;
			int rank = 0;
			String unmodifiedClassName = className;
			while (className.contains("[]")) {
				className = className.replaceFirst("\\[\\]", "");
				rank++;
			}
			if (className.equals("void")) {
				clazz = void.class;
			} else if (ClassReflector.primitiveClassMap
					.containsKey(className)) {
				clazz = ClassReflector.primitiveClassMap.get(className);
			} else {
				clazz = Reflections.forName(className);
			}
			JClassType classType = null;
			if (rank == 0) {
				TypeVariable<?>[] typeVariables = clazz.getTypeParameters();
				if (typeVariables.length == 0) {
					classType = new JRealClassType(this, clazz);
				} else {
					classType = new cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JGenericType(
							this, clazz);
				}
			} else {
				Class<? extends Object> arrayClass = Array
						.newInstance(clazz, rank).getClass();
				classType = new JArrayType(this, arrayClass);
				clazz = arrayClass;
			}
			jclasses.put(unmodifiedClassName, classType);
			jclassesByClass.put(clazz, classType);
			return classType;
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
	public JParameterizedType getParameterizedType(JGenericType genericType,
			com.google.gwt.core.ext.typeinfo.JClassType enclosingType,
			com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JParameterizedType getParameterizedType(JGenericType genericType,
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
		/*
		 * if (type.contains(".")) { return findType(type); } else { return
		 * JPrimitiveType.valueOf(type); } nope - we need the backing JVM class
		 * in all cases, and JPrimitiveType (an enum) is final
		 */
	}

	JClassType getType(Class<?> clazz) {
		if (clazz == null) {
			return null;
		}
		JClassType type = jclassesByClass.get(clazz);
		if (type == null) {
			String canonicalName = clazz.getCanonicalName();
			Preconditions.checkNotNull(canonicalName);
			type = findType(canonicalName);
		}
		return type;
	}
}

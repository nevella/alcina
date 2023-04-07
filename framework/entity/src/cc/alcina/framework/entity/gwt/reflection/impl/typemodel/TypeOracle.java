package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ClassReflector;

public class TypeOracle extends com.google.gwt.core.ext.typeinfo.TypeOracle {
	private final Map<String, JPackage> packages = new HashMap<>();

	private final Map<String, JClassType> classes = new HashMap<>();

	@Override
	public synchronized JPackage findPackage(String pkgName) {
		return packages.computeIfAbsent(pkgName, name -> new JPackage(pkgName));
	}

	@Override
	public synchronized JClassType findType(String className) {
		JClassType existingValue = classes.get(className);
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
			JClassType existingValue = classes.get(className);
			if (existingValue != null) {
				return existingValue;
			}
			Class<?> clazz = null;
			if (ClassReflector.primitiveClassMap.containsKey(className)) {
				clazz = ClassReflector.primitiveClassMap.get(className);
			} else {
				clazz = Class.forName(className);
			}
			JRealClassType realClassType = new JRealClassType(this, clazz);
			classes.put(className, realClassType);
			return realClassType;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public JArrayType getArrayType(JType componentType) {
		// TODO Auto-generated method stub
		return null;
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
	public JParameterizedType getParameterizedType(JGenericType extGenericType,
			JClassType extEnclosingType, JClassType[] extTypeArgs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JParameterizedType getParameterizedType(JGenericType genericType,
			JClassType[] typeArgs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getSingleJsoImpl(JClassType intf) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized JClassType getType(String pkgName,
			String topLevelTypeSimpleName) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized JClassType[] getTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JWildcardType getWildcardType(JWildcardType.BoundType boundType,
			JClassType extTypeBound) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized JType parse(String type) throws TypeOracleException {
		// TODO Auto-generated method stub
		return null;
	}
}

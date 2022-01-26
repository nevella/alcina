package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider.ClassAnnotationResolver;

/*
 * TODO - caching annotation facade? Or cache on the resolver (possibly latter)
 */
public class ClassReflector<T> {
	private Class<T> clazz;

	private Map<String, Property> byName;

	private List<Property> properties;

	public List<Property> properties() {
		return this.properties;
	}

	private AnnotationResolver annotationResolver;

	private Supplier<T> constructor;

	private Predicate<Class> assignableTo;

	private final boolean primitive;

	private final boolean reflective;

	private boolean isAbstract;

	@Override
	public String toString() {
		return clazz.toString();
	}

	public boolean isAssignableTo(Class to) {
		return assignableTo.test(to);
	}

	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationResolver.getAnnotation(annotationClass);
	}

	public <A extends Annotation> boolean has(Class<A> annotationClass) {
		return annotationResolver.hasAnnotation(annotationClass);
	}

	public Property property(String name) {
		return byName.get(name);
	}

	public T newInstance() {
		return constructor.get();
	}

	public ClassReflector(Class<T> clazz, List<Property> properties,
			Map<String, Property> byName,
			ClassAnnotationResolver annotationResolver, Supplier<T> constructor,
			Predicate<Class> assignableTo, List<Class> interfaces,
			boolean reflective, boolean isAbstract) {
		this.clazz = clazz;
		this.properties = properties;
		this.byName = byName;
		this.annotationResolver = annotationResolver;
		this.constructor = constructor;
		this.assignableTo = assignableTo;
		this.reflective = reflective;
		this.isAbstract = isAbstract;
		this.interfaces = interfaces;
		this.primitive = ClassReflector.primitives.contains(clazz);
	}

	private T templateInstance;

	private final List<Class> interfaces;

	// FIXME - reflection 1.1 - use optimised collections, probably remove the
	// string/class maps (use 'isPrimitiveWrapper; isJdkValueClass'
	public static final Map<String, Class> stdClassMap = new HashMap<String, Class>();

	public static final Map<String, Class> primitiveClassMap = new HashMap<String, Class>();

	public static final Map<String, Class> stdAndPrimitivesMap = new HashMap<String, Class>();
	static {
		Class[] stds = { Long.class, Double.class, Float.class, Short.class,
				Byte.class, Integer.class, Boolean.class, Character.class,
				Date.class, String.class, Timestamp.class };
		for (Class std : stds) {
			ClassReflector.stdClassMap.put(std.getName(), std);
		}
	}
	static {
		Class[] prims = { long.class, int.class, short.class, char.class,
				byte.class, boolean.class, double.class, float.class };
		for (Class prim : prims) {
			ClassReflector.primitiveClassMap.put(prim.getName(), prim);
		}
	}
	static {
		ClassReflector.stdAndPrimitivesMap.putAll(ClassReflector.stdClassMap);
		ClassReflector.stdAndPrimitivesMap
				.putAll(ClassReflector.primitiveClassMap);
	}

	public static final Set<Class> stdAndPrimitives = new HashSet<Class>(
			stdAndPrimitivesMap.values());

	public static final Set<Class> primitives = new HashSet<Class>(
			primitiveClassMap.values());

	public T templateInstance() {
		if (templateInstance == null) {
			templateInstance = newInstance();
		}
		return templateInstance;
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public boolean isReflective() {
		return reflective;
	}

	public boolean hasProperty(String propertyName) {
		return byName.containsKey(propertyName);
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public List<Class> getInterfaces() {
		return interfaces;
	}
}

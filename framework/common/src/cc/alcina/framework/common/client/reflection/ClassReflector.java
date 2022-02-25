package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.reflection.Bean;

/*
 * TODO - caching annotation facade? Or cache on the resolver (possibly latter)
 */
public class ClassReflector<T> {
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

	public static ClassReflector<?> emptyReflector(Class clazz) {
		return new ClassReflector<>(clazz, Collections.emptyList(),
				Collections.emptyMap(), new AnnotationProvider.LookupProvider(),
				null, t -> false, Collections.emptyList(),
				// may in fact be true, but unused
				false);
	}

	private Class<T> reflectedClass;

	private Map<String, Property> byName;

	private List<Property> properties;

	private AnnotationProvider annotationProvider;

	private Supplier<T> constructor;

	private Predicate<Class> assignableTo;

	private boolean primitive;

	private boolean isAbstract;

	private T templateInstance;

	private List<Class> interfaces;

	public ClassReflector(Class<T> reflectedClass, List<Property> properties,
			Map<String, Property> byName, AnnotationProvider annotationResolver,
			Supplier<T> constructor, Predicate<Class> assignableTo,
			List<Class> interfaces, boolean isAbstract) {
		this();
		init(reflectedClass, properties, byName, annotationResolver,
				constructor, assignableTo, interfaces, isAbstract);
	}

	protected ClassReflector() {
		init0();
	}

	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationProvider.getAnnotation(annotationClass);
	}

	public List<Class> getInterfaces() {
		return interfaces;
	}

	public Class<T> getReflectedClass() {
		return this.reflectedClass;
	}

	public <A extends Annotation> boolean has(Class<A> annotationClass) {
		return annotationProvider.hasAnnotation(annotationClass);
	}

	public boolean hasProperty(String propertyName) {
		return byName.containsKey(propertyName);
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isAssignableTo(Class to) {
		return assignableTo.test(to);
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public T newInstance() {
		return constructor.get();
	}

	public List<Property> properties() {
		return this.properties;
	}

	public Property property(String name) {
		return byName.get(name);
	}

	// FIXME - reflection - this should probably go away - code which checks
	// generally is only reachable with a reflectable object (but check
	// associationpropagationlistener)
	public boolean provideIsReflective() {
		return has(Bean.class);
	}

	public T templateInstance() {
		if (templateInstance == null) {
			templateInstance = newInstance();
		}
		return templateInstance;
	}

	@Override
	public String toString() {
		return reflectedClass.toString();
	}

	protected void init(Class<T> reflectedClass, List<Property> properties,
			Map<String, Property> byName, AnnotationProvider annotationProvider,
			Supplier<T> constructor, Predicate<Class> assignableTo,
			List<Class> interfaces, boolean isAbstract) {
		this.reflectedClass = reflectedClass;
		this.properties = properties;
		this.byName = byName;
		this.annotationProvider = annotationProvider;
		this.constructor = constructor;
		this.assignableTo = assignableTo;
		this.isAbstract = isAbstract;
		this.interfaces = interfaces;
		this.primitive = ClassReflector.primitives.contains(reflectedClass);
	}

	protected void init0() {
		// Overriden by generated subclasses
	}
}

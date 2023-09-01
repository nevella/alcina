package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JClassType;

/*
 * TODO - caching annotation facade? Or cache on the resolver (possibly latter)
 */
public class ClassReflector<T> implements HasAnnotations {
	// FIXME - reflection 1.1 - use optimised collections, probably remove the
	// string/class maps (use 'isPrimitiveWrapper; isJdkValueClass'
	public static final Map<String, Class> stdClassMap = new HashMap<String, Class>();

	public static final Map<String, Class> primitiveClassMap = new HashMap<String, Class>();

	public static final Map<String, Class> stdAndPrimitivesMap = new HashMap<String, Class>();
	static {
		Class[] stds = { Long.class, Double.class, Float.class, Short.class,
				Byte.class, Integer.class, Boolean.class, Character.class,
				Date.class, String.class, Timestamp.class, UUID.class };
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

	public static <T> void copyProperties(T from, T to,
			Predicate<String> namePredicate) {
		ClassReflector<?> reflector = Reflections.at(from);
		Stream<Property> properties = reflector.properties().stream()
				.filter(p -> namePredicate.test(p.getName()));
		properties.forEach(p -> p.copy(from, to));
	}

	public static <T> void copyProperties(T from, T to,
			String... propertyNames) {
		List<String> list = Arrays.asList(propertyNames);
		copyProperties(from, to, list::contains);
	}

	public static ClassReflector<?> emptyReflector(Class clazz) {
		return new ClassReflector<>(clazz, Collections.emptyList(),
				Collections.emptyMap(), new AnnotationProvider.LookupProvider(),
				null, t -> false, Collections.emptyList(), null,
				// may in fact be true, but unused
				false, false);
	}

	private Class<T> reflectedClass;

	private Map<String, Property> byName;

	private List<Property> properties;

	private AnnotationProvider annotationProvider;

	private Supplier<T> noArgsConstructor;

	private Predicate<Class> assignableTo;

	private boolean primitive;

	private boolean isAbstract;

	private boolean isFinal;

	private T templateInstance;

	private List<Class> interfaces;

	private TypeBounds genericBounds;

	private transient List<Class> allInterfaces;

	public ClassReflector(Class<T> reflectedClass, List<Property> properties,
			Map<String, Property> byName, AnnotationProvider annotationResolver,
			Supplier<T> constructor, Predicate<Class> assignableTo,
			List<Class> interfaces, TypeBounds genericBounds,
			boolean isAbstract, boolean isFinal) {
		this();
		init(reflectedClass, properties, byName, annotationResolver,
				constructor, assignableTo, interfaces, genericBounds,
				isAbstract, isFinal);
	}

	protected ClassReflector() {
		init0();
	}

	@Override
	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationProvider.getAnnotation(annotationClass);
	}

	@Override
	public <A extends Annotation> List<A>
			annotations(Class<A> annotationClass) {
		return annotationProvider.getAnnotations(annotationClass);
	}

	/**
	 * This is the resolved bounds of this class - i.e
	 * {@code A extends B<String> would have a genericBounds of [String.class] }.
	 * Note that this takes into account both direct superclass and interface
	 * bounds - with more detail in {@link JClassType.Members#computeBounds}
	 */
	public TypeBounds getGenericBounds() {
		return this.genericBounds;
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

	public boolean hasNoArgsConstructor() {
		return noArgsConstructor != null;
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

	public boolean isFinal() {
		return isFinal;
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public T newInstance() {
		return noArgsConstructor.get();
	}

	public List<Property> properties() {
		return this.properties;
	}

	public Property property(PropertyEnum name) {
		return byName.get(name.name());
	}

	public Property property(String name) {
		return byName.get(name);
	}

	public Stream<Class> provideAllImplementedInterfaces() {
		if (allInterfaces == null) {
			Set<Class> allInterfaces = new LinkedHashSet<>();
			Set<Class> checked = new LinkedHashSet<>();
			Set<Class> pending = new LinkedHashSet<>();
			pending.add(reflectedClass);
			while (!pending.isEmpty()) {
				Iterator<Class> itr = pending.iterator();
				Class<?> next = itr.next();
				itr.remove();
				if (next == null || checked.contains(next)) {
					continue;
				}
				checked.add(next);
				if (next.isInterface()) {
					allInterfaces.add(next);
				}
				Reflections.at(next).getInterfaces().stream()
						.forEach(pending::add);
				pending.add(next.getSuperclass());
			}
			this.allInterfaces = allInterfaces.stream()
					.collect(Collectors.toList());
		}
		return allInterfaces.stream();
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
			List<Class> interfaces, TypeBounds genericBounds,
			boolean isAbstract, boolean isFinal) {
		this.reflectedClass = reflectedClass;
		this.properties = properties;
		this.byName = byName;
		this.annotationProvider = annotationProvider;
		this.noArgsConstructor = constructor;
		this.assignableTo = assignableTo;
		this.genericBounds = genericBounds;
		this.isAbstract = isAbstract;
		this.interfaces = interfaces;
		this.isFinal = isFinal;
		this.primitive = ClassReflector.primitives.contains(reflectedClass);
	}

	protected void init0() {
		// Overriden by generated subclasses
	}
}

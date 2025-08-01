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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JClassType;

/**
 * <p>
 * Models reflective information for a type.
 * <p>
 * Note re reflection - annotation access does *not* resolve - it essentially
 * tracks JDK annotation behaviour. Use {@link AnnotationLocation} to resolve
 * annotations at a property
 * 
 * <p>
 * TODO - caching annotation facade? Or cache on the resolver (possibly latter)
 */
public class ClassReflector<T> implements HasAnnotations {
	@Registration(TypeInvoker.class)
	public static class TypeInvoker<T> {
		public Object invoke(T bean, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<?> flags) {
			return invokeReflective(bean, methodName, argumentTypes, arguments,
					flags);
		}

		protected Object invokeReflective(Object bean, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<?> flags) {
			String beanRegex = "(get|set|is)(.)(.+)";
			RegExp regExp = RegExp.compile(beanRegex);
			MatchResult matchResult = regExp.exec(methodName);
			boolean reflective = matchResult != null;
			if (!reflective) {
				boolean get = !matchResult.getGroup(1).equals("set");
				reflective &= get && argumentTypes.size() == 0;
				reflective &= !get && argumentTypes.size() == 1;
			}
			if (!reflective) {
				throw new IllegalArgumentException(
						Ax.format("Not bean method format: %s", methodName));
			} else {
				boolean get = !matchResult.getGroup(1).equals("set");
				String propertyName = Ax.format("%s%s",
						matchResult.getGroup(2).toLowerCase(),
						matchResult.getGroup(3));
				Property property = Reflections.at(bean).property(propertyName);
				if (property == null && Al.isBrowser()) {
					/*
					 * Due to download size economics, Element subtypes aren't
					 * modelled
					 */
					property = Reflections.at(bean.getClass().getSuperclass())
							.property(propertyName);
				}
				if (get) {
					return property.get(bean);
				} else {
					property.set(bean, arguments.get(0));
					return null;
				}
			}
		}
	}

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

	public static <T> boolean equalProperties(T a, T b) {
		ClassReflector<?> reflector = Reflections.at(a);
		Predicate<Property> predicate = p -> Objects.equals(p.get(a), p.get(b));
		return reflector.properties().stream().allMatch(predicate);
	}

	public static <T> void copyProperties(T from, T to,
			String... propertyNames) {
		List<String> list = Arrays.asList(propertyNames);
		copyProperties(from, to, list::contains);
	}

	public static ClassReflector<?> emptyReflector(Class clazz,
			List<Class> interfaces) {
		return new ClassReflector<>(clazz, Collections.emptyList(),
				Collections.emptyMap(), new AnnotationProvider.EmptyProvider(),
				null, t -> false, interfaces, null, Collections.emptyList(),
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

	private List<Class> classes;

	private transient Boolean isReflective;

	public ClassReflector(Class<T> reflectedClass, List<Property> properties,
			Map<String, Property> byName, AnnotationProvider annotationProvider,
			Supplier<T> constructor, Predicate<Class> assignableTo,
			List<Class> interfaces, TypeBounds genericBounds,
			List<Class> classes, boolean isAbstract, boolean isFinal) {
		this();
		init(reflectedClass, properties, byName, annotationProvider,
				constructor, assignableTo, interfaces, genericBounds, classes,
				isAbstract, isFinal);
	}

	protected ClassReflector() {
		init0();
	}

	public List<Class> getClasses() {
		return classes;
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

	public Object invoke(Object bean, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<?> flags) {
		TypeInvoker invoker = Registry.impl(TypeInvoker.class, bean.getClass());
		return invoker.invoke(bean, methodName, argumentTypes, arguments,
				flags);
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
		if (noArgsConstructor == null) {
			throw new NullPointerException(Ax.format(
					"Reflector %s has no no-args constructor", reflectedClass));
		}
		return noArgsConstructor.get();
	}

	public List<Property> properties() {
		return this.properties;
	}

	public Property property(Object stringOrPropertyEnum) {
		Property property = property0(stringOrPropertyEnum);
		Objects.requireNonNull(property,
				() -> Ax.format("Property '%s' not found on type '%s'",
						stringOrPropertyEnum, NestedName.get(reflectedClass)));
		return property;
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
		if (isReflective == null) {
			isReflective = new AnnotationLocation(reflectedClass, null)
					.hasAnnotation(Bean.class);
		}
		return isReflective;
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

	@Override
	public boolean isProperty(Class<?> owningType, String name) {
		return false;
	}

	/**
	 * 
	 * @param <B>
	 *            the (an) expected type of the bound
	 * @return the first generic bound of the type, or null if the type has no
	 *         generic bounds
	 */
	public <B> Class<B> firstGenericBound() {
		return genericBounds.bounds.isEmpty() ? null
				: genericBounds.bounds.get(0);
	}

	protected void init(Class<T> reflectedClass, List<Property> properties,
			Map<String, Property> byName, AnnotationProvider annotationProvider,
			Supplier<T> constructor, Predicate<Class> assignableTo,
			List<Class> interfaces, TypeBounds genericBounds,
			List<Class> classes, boolean isAbstract, boolean isFinal) {
		this.reflectedClass = reflectedClass;
		this.properties = properties;
		this.byName = byName;
		this.annotationProvider = annotationProvider;
		this.noArgsConstructor = constructor;
		this.assignableTo = assignableTo;
		this.genericBounds = genericBounds;
		this.isAbstract = isAbstract;
		this.interfaces = interfaces;
		this.classes = classes;
		this.isFinal = isFinal;
		this.primitive = ClassReflector.primitives.contains(reflectedClass);
	}

	protected void init0() {
		// Overriden by generated subclasses
	}

	Property property0(Object stringOrPropertyEnum) {
		if (stringOrPropertyEnum instanceof String) {
			return property((String) stringOrPropertyEnum);
		} else if (stringOrPropertyEnum instanceof PropertyEnum) {
			return byName.get(((PropertyEnum) stringOrPropertyEnum).name());
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean isClass(Class locationClass, String propertyName) {
		if (propertyName != null) {
			return false;
		}
		return locationClass == reflectedClass;
	}

	public PropertyComparator<T> propertyComparator() {
		return new PropertyComparator<>(this);
	}

	public static class PropertyComparator<T> {
		ClassReflector<T> reflector;

		PropertyComparator(ClassReflector<T> reflector) {
			this.reflector = reflector;
		}

		public class Comparison {
			public ClassReflector<T> reflector;

			public T left;

			public T right;

			public List<Difference> differences;

			public Comparison(ClassReflector<T> reflector, T left, T right) {
				this.reflector = reflector;
				this.left = left;
				this.right = right;
			}

			public boolean hasDiffs() {
				return differences.size() > 0;
			}

			@Override
			public String toString() {
				return differences.toString();
			}
		}

		public class Difference {
			public Property property;

			public Object left;

			public Object right;

			Difference(Property property, Object left, Object right) {
				this.property = property;
				this.left = left;
				this.right = right;
			}

			@Override
			public String toString() {
				return Ax.format("%s: '%s', '%s'", property.getName(),
						Ax.trimForLogging(left, 50),
						Ax.trimForLogging(right, 50));
			}
		}

		public PropertyComparator<T>.Comparison compare(T left, T right) {
			Comparison comparison = new Comparison(reflector, left, right);
			comparison.differences = reflector.properties().stream()
					.map(property -> {
						Object leftValue = property.get(left);
						Object rightValue = property.get(right);
						if (Objects.equals(leftValue, rightValue)) {
							return null;
						} else {
							return new Difference(property, leftValue,
									rightValue);
						}
					}).filter(Objects::nonNull).toList();
			return comparison;
		}
	}

	public String toPropertiesString(T t) {
		FormatBuilder builder = new FormatBuilder().separator(", ");
		properties().forEach(prop -> {
			Object object = prop.get(t);
			if (object != null) {
				builder.format("%s:%s", prop.getName(),
						Ax.trimForLogging(object, 50));
			}
		});
		return Ax.format("[%s]", builder.toString());
	}
}

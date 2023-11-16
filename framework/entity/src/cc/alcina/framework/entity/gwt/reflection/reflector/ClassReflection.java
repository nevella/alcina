package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ClassUtil;
import cc.alcina.framework.entity.gwt.reflection.AnnotationLocationTypeInfo;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection.PropertyAccessor;

/**
 * Builds a class reflector (or data required for ClassReflector source
 * gneration)
 *
 *
 *
 */
public class ClassReflection extends ReflectionElement {
	static final Predicate<Registration> CLIENT_VISIBLE_ANNOTATION_FILTER = new Predicate<Registration>() {
		@Override
		public boolean test(Registration o) {
			return o.value()[0]
					.getAnnotation(NonClientRegistryPointType.class) == null;
		}
	};

	@Override
	public String toString() {
		return Ax.format("Reflection: %s", type);
	}

	public static JClassType erase(JClassType t) {
		if (t.isParameterized() != null) {
			return t.isParameterized().getBaseType().getErasedType();
		} else {
			return t.getErasedType();
		}
	}

	public static JType erase(JType t) {
		if (t.isClass() != null) {
			return erase((JClassType) t);
		} else {
			return t;
		}
	}

	public static <A extends Annotation> boolean has(JClassType t,
			Class<A> annotationClass) {
		return t.getAnnotation(annotationClass) != null;
	}

	static boolean isObjectType(JClassType type) {
		return type.getQualifiedSourceName()
				.equals(Object.class.getCanonicalName());
	}

	AnnotationLocationTypeInfo.Resolver annotationResolver = new AnnotationLocationTypeInfo.Resolver();

	public final JClassType type;

	public final boolean sourcesPropertyChanges;

	ReflectionVisibility reflectionVisibility;

	JConstructor noArgsConstructor;

	boolean hasAbstractModifier;

	boolean hasFinalModifier;

	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	Map<String, PropertyReflection> propertyReflections = new LinkedHashMap<>();

	List<PropertyReflection> sortedPropertyReflections = new ArrayList<>();

	Pattern getterPattern = Pattern.compile("(?:is|get)([A-Z].*)");

	Pattern setterPattern = Pattern.compile("(?:set)([A-Z].*)");

	List<Registration> registrations = new ArrayList<>();

	ProvidesTypeBounds providesTypeBounds;

	AnnotationLocationTypeInfo typeAnnotationLocation;

	public ClassReflection(JType type, boolean sourcesPropertyChanges,
			ReflectionVisibility reflectionVisibility,
			ProvidesTypeBounds providesTypeBounds) {
		this.sourcesPropertyChanges = sourcesPropertyChanges;
		this.providesTypeBounds = providesTypeBounds;
		this.type = type instanceof JClassType ? (JClassType) type : null;
		this.reflectionVisibility = reflectionVisibility;
	}

	public ClassReflector asReflector() {
		List<Property> properties = sortedPropertyReflections.stream()
				.map(PropertyReflection::asProperty)
				.collect(Collectors.toList());
		Supplier supplier = noArgsConstructor == null ? null
				: (Supplier) noArgsConstructor;
		Predicate<Class> assignableTo = ((ProvidesAssignableTo) type)
				.provideAssignableTo();
		List<Class> interfaces = ((ProvidesInterfaces) type)
				.provideInterfaces();
		Class javaType = ((ProvidesJavaType) type).provideJavaType();
		List<? extends JClassType> jTypeBounds = providesTypeBounds
				.provideTypeBounds(type);
		List<Class> bounds = jTypeBounds.stream().map(this::asJavaType)
				.collect(Collectors.toList());
		TypeBounds typeBounds = new TypeBounds(bounds);
		return new ClassReflector(javaType, properties,
				properties.stream()
						.collect(AlcinaCollectors.toKeyMap(Property::getName)),
				new AnnotationProviderImpl(), supplier, assignableTo,
				interfaces, typeBounds, type.isAbstract(), type.isFinal());
	}

	/*
	 * only called by
	 * ClientReflectionGenerator/com.google.gwt.dev.javac.typemodels
	 */
	public List<JClassType>
			computeTypeBounds(ProvidesTypeBounds providesJavacTypeBounds) {
		return (List<JClassType>) (List<?>) providesJavacTypeBounds
				.provideTypeBounds(type);
	}

	public Stream<Class> getAnnotationAttributeTypes() {
		return Stream.concat(
				annotationReflections.stream().flatMap(
						AnnotationReflection::getAnnotationAttributeTypes),
				propertyReflections.values().stream().flatMap(
						PropertyReflection::getAnnotationAttributeTypes));
	}

	public List<AnnotationReflection> getAnnotationReflections() {
		return this.annotationReflections;
	}

	public List<Registration> getRegistrations() {
		return registrations;
	}

	public List<PropertyReflection> getSortedPropertyReflections() {
		return sortedPropertyReflections;
	}

	public boolean isHasAbstractModifier() {
		return this.hasAbstractModifier;
	}

	public boolean isHasCallableNoArgsConstructor() {
		return this.noArgsConstructor != null;
	}

	public boolean isHasFinalModifier() {
		return this.hasFinalModifier;
	}

	@Override
	public void prepare() {
		if (type == null) {
			// primitive
			return;
		}
		this.typeAnnotationLocation = new AnnotationLocationTypeInfo(type,
				annotationResolver);
		this.hasAbstractModifier = type.isAbstract();
		this.hasFinalModifier = type.isFinal();
		boolean isExternalConstructible = !hasAbstractModifier
				&& !type.getQualifiedSourceName().equals("java.lang.Class")
				&& (type.isStatic() || !type.isMemberType())
				&& !(!type.isPublic()
						&& type.getQualifiedSourceName().startsWith("java"));
		if (isExternalConstructible) {
			noArgsConstructor = Arrays.stream(type.getConstructors())
					.filter(c -> c.getParameters().length == 0).findFirst()
					.orElse(null);
			if (noArgsConstructor != null && noArgsConstructor.isPrivate()) {
				noArgsConstructor = null;
			}
			if (noArgsConstructor != null && !noArgsConstructor.isPublic()
					&& type.getQualifiedSourceName().startsWith("java")) {
				noArgsConstructor = null;
			}
			if (noArgsConstructor instanceof AccessibleConstructor) {
				((AccessibleConstructor) noArgsConstructor).makeAccessible();
			}
		}
		Arrays.stream(type.getAnnotations())
				.filter(a -> reflectionVisibility
						.isVisibleAnnotation(a.annotationType()))
				.map(AnnotationReflection::new).sorted()
				.forEach(annotationReflections::add);
		prepareProperties();
		if (!hasAbstractModifier) {
			prepareRegistrations();
		}
	}

	private String methodNamePartToPropertyName(String s) {
		if (s.length() == 0) {
			return s;
		}
		String first = s.substring(0, 1);
		if (first.equals(first.toLowerCase())) {
			return s;
		}
		if (s.length() > 1) {
			String second = s.substring(1, 2);
			if (second.equals(second.toUpperCase())
					&& second.matches("[A-Z]")) {
				// acronym
				return s;
			}
		}
		return first.toLowerCase() + s.substring(1);
	}

	void addFieldMethods(JField field) {
		boolean hasMutableFields = typeAnnotationLocation
				.hasAnnotation(Bean.class)
				&& typeAnnotationLocation.getAnnotation(Bean.class)
						.value() == PropertySource.FIELDS;
		PropertyReflection propertyReflection = propertyReflections
				.computeIfAbsent(field.getName(),
						name -> new PropertyReflection(this, name,
								reflectionVisibility, providesTypeBounds));
		propertyReflection.addMethod(new PropertyAccessor.Field(field, true,
				sourcesPropertyChanges));
		if (hasMutableFields && !field.isFinal()) {
			propertyReflection.addMethod(new PropertyAccessor.Field(field,
					false, sourcesPropertyChanges));
		}
	}

	void addPropertyMethod(PropertyAccessor m) {
		PropertyReflection propertyReflection = propertyReflections
				.computeIfAbsent(m.propertyName,
						name -> new PropertyReflection(this, name,
								reflectionVisibility, providesTypeBounds));
		propertyReflection.addMethod(m);
	}

	List<JField> getAllVisibleFields() {
		List<JField> propertyFields = new ArrayList<>();
		JClassType cursor = type;
		/*
		 * The result of the loop is that fields are ordered (superclass before
		 * subclass)(then field order in class)
		 *
		 */
		while (cursor.getSuperclass() != null) {
			JField[] fields = cursor.getFields();
			int perClassIndex = 0;
			for (int idx = 0; idx < fields.length; idx++) {
				JField field = fields[idx];
				/*
				 * FIDME - reflection - this restriction shd be removed, by
				 * reworking the GWT accessors (to call the superclass)
				 */
				if (!Objects.equals(cursor.getPackage(), type.getPackage())) {
					if (!field.isPublic()) {
						continue;
					}
				}
				propertyFields.add(perClassIndex++, field);
			}
			cursor = cursor.getSuperclass();
		}
		return propertyFields;
	}

	void prepareProperties() {
		boolean hasReflectableProperties = reflectionVisibility
				.isVisibleType(type);
		if (!hasReflectableProperties) {
			return;
		}
		/*
		 * Add method (getter/setter)-derived properties
		 */
		Arrays.stream(type.getInheritableMethods())
				.filter(reflectionVisibility::isVisibleMethod)
				.filter(m -> !m.getName().equals("getClass"))
				.map(this::toPropertyMethod).filter(Objects::nonNull)
				.forEach(this::addPropertyMethod);
		/*
		 * Add field-derived properties. Note that method-first means any method
		 * will override, if it exists
		 */
		Bean.PropertySource propertySource = typeAnnotationLocation
				.hasAnnotation(Bean.class)
						? typeAnnotationLocation.getAnnotation(Bean.class)
								.value()
						: null;
		boolean hasFields = propertySource == PropertySource.FIELDS
				|| propertySource == PropertySource.IMMUTABLE_FIELDS;
		boolean hasMutableFields = propertySource == PropertySource.FIELDS;
		if (hasFields) {
			List<JField> fields = getAllVisibleFields();
			fields.stream().
			// non-transient, non-private, non-static and immutable unless
			// Bean.Fields. Note that this includes superclass fields (including
			// package- and protected-)
					filter(f -> !f.isTransient() && !f.isPrivate()
							&& !f.isStatic())
					.filter(f -> f.isFinal() || hasMutableFields)
					.forEach(this::addFieldMethods);
		}
		/*
		 * Cleanup, sort, prepare
		 */
		propertyReflections.values().removeIf(refl -> refl.has(Omit.class));
		propertyReflections.values().stream().sorted(new PropertyOrdering())
				.forEach(sortedPropertyReflections::add);
		sortedPropertyReflections.forEach(PropertyReflection::prepare);
	}

	void prepareRegistrations() {
		List<Registration> annotations = new AnnotationLocationTypeInfo(type,
				annotationResolver).getAnnotations(Registration.class);
		annotations.stream().filter(CLIENT_VISIBLE_ANNOTATION_FILTER)
				.forEach(getRegistrations()::add);
	}

	PropertyReflection.PropertyAccessor toPropertyMethod(JMethod method) {
		if (method.isPrivate()) {
			return null;
		}
		if (!method.isPublic()) {
			if (method.getEnclosingType() != type) {
				return null;
			}
			if (method.getEnclosingType().getPackage().getName()
					.startsWith("java")) {
				return null;
			}
		}
		if (method.isStatic()) {
			return null;
		}
		// getter
		if (method.getParameters().length == 0) {
			Matcher m = getterPattern.matcher(method.getName());
			if (m.matches()) {
				boolean ignoreableIs = method.getName().startsWith("is")
						&& !Objects.equals(
								method.getReturnType().getQualifiedSourceName(),
								"boolean");
				if (!ignoreableIs) {
					return new PropertyReflection.PropertyAccessor.Method(
							methodNamePartToPropertyName(m.group(1)), true,
							method);
				}
			}
		}
		// setter
		if (method.getParameters().length == 1
				// jvm version has no primitive types
				&& (method.getReturnType() == JPrimitiveType.VOID
						|| method.getReturnType().getQualifiedSourceName()
								.equals("void"))) {
			Matcher m = setterPattern.matcher(method.getName());
			if (m.matches()) {
				return new PropertyReflection.PropertyAccessor.Method(
						methodNamePartToPropertyName(m.group(1)), false,
						method);
			}
		}
		// not a property method
		return null;
	}

	public interface AccessibleConstructor {
		void makeAccessible();
	}

	public interface JdkTypeModelMapper {
		JClassType getType(Class jdkType);
	}

	public interface ProvidesAssignableTo {
		Predicate<Class> provideAssignableTo();
	}

	public interface ProvidesInterfaces {
		List<Class> provideInterfaces();
	}

	public interface ProvidesJavaType {
		Class provideJavaType();
	}

	public interface ProvidesTypeBounds {
		List<? extends JClassType> provideTypeBounds(JClassType type);
	}

	class AnnotationProviderImpl implements AnnotationProvider {
		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return (A) type.getAnnotation(annotationClass);
		}

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			return type.getAnnotations(annotationClass);
		}
	}

	/*
	 * If non custom, ordering is (superclass)(fields)(subclass)(fields)
	 */
	class PropertyOrdering implements Comparator<PropertyReflection> {
		private Map<String, Integer> fieldOrdinals;

		private PropertyOrder propertyOrder;

		private PropertyOrder.Custom customOrder;

		public PropertyOrdering() {
			Multimap<JClassType, List<JField>> declaredFieldsByClass = new Multimap<>();
			JClassType cursor = type;
			while (cursor != null && !ClassReflection.isObjectType(cursor)) {
				declaredFieldsByClass.put(cursor,
						Arrays.stream(cursor.getFields())
								.collect(Collectors.toList()));
				cursor = cursor.getSuperclass();
			}
			List<JClassType> classOrder = declaredFieldsByClass.keySet()
					.stream().collect(Collectors.toList());
			Comparator<JClassType> classOrderComparator = new Comparator<JClassType>() {
				@Override
				public int compare(JClassType o1, JClassType o2) {
					JClassType ancestor = o1.isAssignableFrom(o2) ? o1 : o2;
					JClassType descendant = o1.isAssignableFrom(o2) ? o2 : o1;
					if (descendant.getSuperclass() == ancestor) {
						return o1 == ancestor ? -1 : 1;
					} else {
						return o1 == ancestor ? -1 : 1;
					}
				}
			};
			Collections.sort(classOrder, classOrderComparator);
			List<JField> fieldOrder = new ArrayList<>();
			for (JClassType classOrdered : classOrder) {
				declaredFieldsByClass.get(classOrdered)
						.forEach(fieldOrder::add);
			}
			fieldOrdinals = new LinkedHashMap<>();
			fieldOrder.stream().map(JField::getName).distinct().forEach(
					name -> fieldOrdinals.put(name, fieldOrdinals.size()));
			propertyOrder = type.getAnnotation(PropertyOrder.class);
			customOrder = PropertyOrder.Support.customOrder(propertyOrder,
					ClassUtil.NO_ARGS_INSTANTIATOR);
		}

		@Override
		public int compare(PropertyReflection o1, PropertyReflection o2) {
			if (customOrder != null) {
				int custom = customOrder.compare(o1.getName(), o2.getName());
				if (custom != 0) {
					return custom;
				}
			}
			if (propertyOrder != null && propertyOrder.value().length > 0) {
				int idx1 = Arrays.asList(propertyOrder.value())
						.indexOf(o1.getName());
				int idx2 = Arrays.asList(propertyOrder.value())
						.indexOf(o2.getName());
				if (idx1 == -1) {
					if (idx2 == -1) {
						// fall through
					} else {
						return 1;
					}
				} else {
					if (idx2 == -1) {
						return -1;
					} else {
						return idx1 - idx2;
					}
				}
			}
			int ordinal1 = fieldOrdinals.computeIfAbsent(o1.getName(),
					key -> -1);
			int ordinal2 = fieldOrdinals.computeIfAbsent(o2.getName(),
					key -> -1);
			int i = ordinal1 - ordinal2;
			if (i != 0) {
				return i;
			}
			return o1.getName().compareTo(o2.getName());
		}
	}
}

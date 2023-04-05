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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ClassUtil;
import cc.alcina.framework.entity.gwt.reflection.AnnotationLocationTypeInfo;

/**
 * Builds a class reflector (or data required for ClassReflector source
 * gneration)
 *
 * @author nick@alcina.cc
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

	static boolean isObjectType(JClassType type) {
		return type.getQualifiedSourceName()
				.equals(Object.class.getCanonicalName());
	}

	AnnotationLocationTypeInfo.Resolver annotationResolver = new AnnotationLocationTypeInfo.Resolver();

	public final JClassType type;

	private VisibleAnnotationFilter visibleAnnotationFilter;

	boolean hasCallableNoArgsConstructor;

	boolean hasAbstractModifier;

	boolean hasFinalModifier;

	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	Map<String, PropertyReflection> propertyReflections = new LinkedHashMap<>();

	List<PropertyReflection> sortedPropertyReflections;

	Pattern getterPattern = Pattern.compile("(?:is|get)([A-Z].*)");

	Pattern setterPattern = Pattern.compile("(?:set)([A-Z].*)");

	List<Registration> registrations = new ArrayList<>();

	public ClassReflection(JClassType type,
			VisibleAnnotationFilter visibleAnnotationFilter) {
		this.type = type;
		this.visibleAnnotationFilter = visibleAnnotationFilter;
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
		return this.hasCallableNoArgsConstructor;
	}

	public boolean isHasFinalModifier() {
		return this.hasFinalModifier;
	}

	@Override
	public void prepare() {
		hasAbstractModifier = type.isAbstract();
		hasFinalModifier = type.isFinal();
		hasCallableNoArgsConstructor = !hasAbstractModifier
				&& !type.getQualifiedSourceName().equals("java.lang.Class")
				&& (type.isStatic() || !type.isMemberType())
				&& Arrays.stream(type.getConstructors())
						.filter(c -> c.getParameters().length == 0).findFirst()
						.filter(c -> c.isPublic()).isPresent();
		Arrays.stream(type.getAnnotations())
				.filter(a -> visibleAnnotationFilter.test(a.annotationType()))
				.map(AnnotationReflection::new).sorted()
				.forEach(annotationReflections::add);
		// properties are needed even for abstract classes (for annotation
		// access)
		prepareProperties();
		if (!hasAbstractModifier) {
			prepareRegistrations();
		}
	}

	private void prepareProperties() {
		boolean hasReflectableProperties = has(type, Bean.class);
		if (hasReflectableProperties) {
			Arrays.stream(type.getInheritableMethods())
					.map(this::toPropertyMethod).filter(Objects::nonNull)
					.forEach(m -> {
						PropertyReflection propertyReflection = propertyReflections
								.computeIfAbsent(m.propertyName,
										name -> new PropertyReflection(this,
												name, visibleAnnotationFilter));
						propertyReflection.addMethod(m);
					});
		}
		propertyReflections.entrySet().removeIf(
				e -> e.getValue().getter != null && e.getValue().getter.method
						.getAnnotation(Omit.class) != null);
		sortedPropertyReflections = propertyReflections.values().stream()
				.sorted(new PropertyOrdering()).collect(Collectors.toList());
		sortedPropertyReflections.forEach(PropertyReflection::prepare);
		// TODO Auto-generated method stub
	}

	<A extends Annotation> boolean has(JClassType t, Class<A> annotationClass) {
		return t.getAnnotation(annotationClass) != null;
	}

	void prepareRegistrations() {
		List<Registration> annotations = new AnnotationLocationTypeInfo(type,
				annotationResolver).getAnnotations(Registration.class);
		annotations.stream().filter(CLIENT_VISIBLE_ANNOTATION_FILTER)
				.forEach(getRegistrations()::add);
	}

	PropertyReflection.PropertyMethod toPropertyMethod(JMethod method) {
		if (method.getName().equals("getClass")) {
			return null;
		}
		if (!method.isPublic()) {
			return null;
		}
		// getter
		if (method.getParameters().length == 0) {
			Matcher m = getterPattern.matcher(method.getName());
			if (m.matches()) {
				return new PropertyReflection.PropertyMethod(
						CommonUtils.lcFirst(m.group(1)), true, method);
			}
		}
		if (method.getParameters().length == 1
				&& method.getReturnType() == JPrimitiveType.VOID) {
			Matcher m = setterPattern.matcher(method.getName());
			if (m.matches()) {
				return new PropertyReflection.PropertyMethod(
						CommonUtils.lcFirst(m.group(1)), false, method);
			}
		}
		return null;
	}

	/*
	 * Parallels cc.alcina.framework.entity.SEUtilities.
	 * getPropertyDescriptorsSortedByField(Class<?>)
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

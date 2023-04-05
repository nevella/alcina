package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

/**
 */
public class PropertyReflection extends ReflectionElement
		implements Comparable<PropertyReflection> {
	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	public final String name;

	public PropertyMethod getter;

	public PropertyMethod setter;

	public JType propertyType;

	private VisibleAnnotationFilter visibleAnnotationFilter;

	public final ClassReflection classReflection;

	public PropertyReflection(ClassReflection classReflection, String name,
			VisibleAnnotationFilter visibleAnnotationFilter) {
		this.classReflection = classReflection;
		this.name = name;
		this.visibleAnnotationFilter = visibleAnnotationFilter;
	}

	public void addMethod(PropertyMethod method) {
		if (method.getter) {
			getter = method;
			propertyType = method.method.getReturnType();
		} else {
			setter = method;
			propertyType = method.method.getParameters()[0].getType();
		}
		propertyType = ClassReflection.erase(propertyType);
	}

	@Override
	public int compareTo(PropertyReflection o) {
		return name.compareTo(o.name);
	}

	public List<AnnotationReflection> getAnnotationReflections() {
		return this.annotationReflections;
	}

	public String getName() {
		return this.name;
	}

	public boolean isSerializable() {
		return getter != null && setter != null
				&& !getter.method.isAnnotationPresent(AlcinaTransient.class);
	}

	@Override
	public void prepare() {
		annotationReflections = getter == null ? new ArrayList<>()
				: Arrays.stream(getter.method.getAnnotations())
						.filter(ann -> visibleAnnotationFilter
								.test(ann.annotationType()))
						.map(AnnotationReflection::new).sorted()
						.collect(Collectors.toList());
	}

	public static class PropertyMethod {
		String propertyName;

		public boolean getter;

		public JMethod method;

		PropertyMethod(String propertyName, boolean getter, JMethod method) {
			this.propertyName = propertyName;
			this.getter = getter;
			this.method = method;
		}
	}
}

package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PropertyReflection extends ReflectionElement
		implements Comparable<PropertyReflection> {
	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	private final String name;

	public PropertyReflection(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(PropertyReflection o) {
		return name.compareTo(o.name);
	}

	public String getName() {
		return this.name;
	}

	@Override
	public void prepare() {
		throw new UnsupportedOperationException();
	}
}

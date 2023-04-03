package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Builds a class reflector (or data required for ClassReflector source
 * gneration)
 *
 * @author nick@alcina.cc
 *
 */
public class ClassReflection extends ReflectionElement {
	private JClassType type;

	private VisibleAnnotationFilter visibleAnnotationFilter;

	boolean hasCallableNoArgsConstructor;

	boolean hasAbstractModifier;

	boolean hasFinalModifier;

	List<AnnotationReflection> annotationReflections = new ArrayList<>();

	public ClassReflection(JClassType type,
			VisibleAnnotationFilter visibleAnnotationFilter) {
		this.type = type;
		this.visibleAnnotationFilter = visibleAnnotationFilter;
	}

	public List<AnnotationReflection> getAnnotationReflections() {
		return this.annotationReflections;
	}

	public JClassType getType() {
		return this.type;
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
		// TODO Auto-generated method stub
	}

	private void prepareRegistrations() {
		// TODO Auto-generated method stub
	}
}

package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.reflection.ClassReflectorProvider.ClassAnnotationResolver;

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

	@Override
	public String toString() {
		return clazz.toString();
	}
	
	public boolean isAssignableTo(Class to){
		return assignableTo.test(to);
	}

	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationResolver.getAnnotation(annotationClass);
	}

	public Property property(String name) {
		return byName.get(name);
	}

	public T newInstance() {
		return constructor.get();
	}

	public ClassReflector(Class<T> clazz, List<Property> properties,
			Map<String, Property> byName,
			ClassAnnotationResolver annotationResolver,Supplier<T> constructor,Predicate<Class> assignableTo) {
		this.clazz = clazz;
		this.properties = properties;
		this.byName = byName;
		this.annotationResolver = annotationResolver;
		this.constructor = constructor;
		this.assignableTo = assignableTo;
	}

	private T templateInstance;
	public T templateInstance() {
		if(templateInstance==null){
			templateInstance=newInstance();
		}
		return templateInstance;
	}
}

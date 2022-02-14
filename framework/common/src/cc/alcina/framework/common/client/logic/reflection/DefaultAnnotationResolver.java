package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.Reflections;


@ClientInstantiable
@Registration.Singleton(Resolver.class)
public class DefaultAnnotationResolver extends Resolver {
	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		Resolution resolution = Reflections.at(annotationClass)
				.annotation(Resolution.class);
		if (resolution == null) {
			return super.resolveAnnotations0(annotationClass, location);
		}
		Preconditions.checkState(
				!Reflections.at(annotationClass).has(Inherited.class));
		MergeStrategy mergeStrategy = Reflections
				.newInstance(resolution.mergeStrategy());
		List<Inheritance> inheritance = Arrays.asList(resolution.inheritance());
		List<A> propertyAnnotations = mergeStrategy.resolveProperty(
				annotationClass, location.property, inheritance);
		List<A> classAnnotations = mergeStrategy.resolveClass(annotationClass,
				location.classLocation, inheritance);
		List<A> merged = mergeStrategy.merge(propertyAnnotations,
				classAnnotations);
		mergeStrategy.finish(merged);
		return merged;
	}
}
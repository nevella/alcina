package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * The default declarative information resolver. It respects
 * {@link MergeStrategy} type annotations
 */
@Reflected
@Registration.Singleton(Resolver.class)
public class DefaultAnnotationResolver extends Resolver {
	@Override
	public <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		return resolveAnnotations0(annotationClass, location, null, this);
	}

	public <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location,
			ClassResolver classResolver, Resolver originatingResolver) {
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
		List<A> propertyAnnotations = location.resolutionState.resolvedPropertyAnnotations != null
				? (List<A>) location.resolutionState.resolvedPropertyAnnotations
						.stream()
						.filter(a -> a.annotationType() == annotationClass)
						.collect(Collectors.toList())
				: mergeStrategy.resolveProperty(annotationClass,
						location.property, inheritance, originatingResolver);
		Class resolvedLocationClass = classResolver != null
				? resolvedLocationClass = classResolver.apply(location)
				: location.classLocation;
		List<A> classAnnotations = mergeStrategy.resolveClass(annotationClass,
				resolvedLocationClass, inheritance, originatingResolver);
		List<A> merged = mergeStrategy.merge(classAnnotations,
				propertyAnnotations);
		mergeStrategy.finish(merged);
		return merged;
	}

	public interface ClassResolver extends Function<AnnotationLocation, Class> {
	}
}
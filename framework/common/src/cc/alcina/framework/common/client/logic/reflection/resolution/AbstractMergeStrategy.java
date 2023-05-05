package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

@Reflected
public abstract class AbstractMergeStrategy<A extends Annotation>
		implements MergeStrategy<A> {
	@Override
	public List<A> resolveClass(Class<A> annotationClass, Class<?> clazz,
			List<Inheritance> inheritance) {
		if (!inheritance.contains(Inheritance.CLASS) || clazz == null
				|| clazz == void.class) {
			return Collections.emptyList();
		}
		List<A> result = new ArrayList<>();
		Set<Class> stack = new LinkedHashSet<>();
		Set<Class> visited = new LinkedHashSet<>();
		ClassReflector<?> resolvingReflector = Reflections.at(clazz);
		stack.add(clazz);
		Class<?> cursor = clazz;
		while ((cursor = cursor.getSuperclass()) != null) {
			stack.add(cursor);
		}
		while (stack.size() > 0) {
			Iterator<Class> itr = stack.iterator();
			cursor = itr.next();
			itr.remove();
			visited.add(cursor);
			ClassReflector<?> cursorReflector = Reflections.at(cursor);
			List<A> atClass = atClass(annotationClass, cursorReflector,
					resolvingReflector);
			result = merge(atClass, result);
			if (inheritance.contains(Inheritance.INTERFACE)) {
				cursorReflector.getInterfaces().stream()
						.filter(this::permitPackages).filter(visited::add)
						.forEach(stack::add);
			}
		}
		return result;
	}

	@Override
	/*
	 * This resolves annotations on inherited interface methods, by design.
	 */
	public List<A> resolveProperty(Class<A> annotationClass, Property property,
			List<Inheritance> inheritance) {
		if (!inheritance.contains(Inheritance.PROPERTY) || property == null) {
			return Collections.emptyList();
		}
		List<A> result = new ArrayList<>();
		Class cursor = property.getOwningType();
		boolean includeErased = inheritance
				.contains(Inheritance.ERASED_PROPERTY);
		while (cursor != null) {
			Property cursorProperty = Reflections.at(cursor)
					.property(property.getName());
			// For A.P, B.P - B is a subclass of A, P (property) is unchanged by
			// B, P should be ignored for B (and only applied for A) to avoid
			// duplication
			if (cursorProperty != null && cursorProperty
					.getOwningType() == cursorProperty.getDeclaringType()) {
				if (!includeErased) {
					if (cursorProperty.getType() != property.getType()) {
						cursorProperty = null;
					}
				}
				if (cursorProperty != null) {
					List<A> atProperty = atProperty(annotationClass,
							cursorProperty);
					result = merge(atProperty, result);
				}
			}
			cursor = cursor.getSuperclass();
		}
		return result;
	}

	protected abstract List<A> atClass(Class<A> annotationClass,
			ClassReflector<?> reflector, ClassReflector<?> resolvingReflector);

	protected abstract List<A> atProperty(Class<A> annotationClass,
			Property property);

	boolean permitPackages(Class clazz) {
		switch (Reflections.getPackageName(clazz)) {
		// FIXME - reflection - typemodel - remove? generalise?
		case "javax.swing":
			return false;
		default:
			return true;
		}
	}

	public static abstract class AdditiveMergeStrategy<A extends Annotation>
			extends AbstractMergeStrategy<A> {
		@Override
		public List<A> merge(List<A> lessSpecific, List<A> moreSpecific) {
			if (lessSpecific.isEmpty()) {
				return moreSpecific;
			}
			if (moreSpecific.isEmpty()) {
				return lessSpecific;
			}
			return Stream.concat(lessSpecific.stream(), moreSpecific.stream())
					.collect(Collectors.toList());
		}
	}

	public static abstract class SingleResultMergeStrategy<A extends Annotation>
			extends AbstractMergeStrategy<A> {
		@Override
		public List<A> merge(List<A> lessSpecific, List<A> moreSpecific) {
			if (lessSpecific.isEmpty()) {
				return moreSpecific;
			}
			if (moreSpecific.isEmpty()) {
				return lessSpecific;
			}
			Preconditions.checkState(moreSpecific.size() == 1);
			return moreSpecific;
		}

		public static abstract class ClassOnly<A extends Annotation>
				extends AbstractMergeStrategy.SingleResultMergeStrategy<A> {
			@Override
			protected List<A> atClass(Class<A> annotationClass,
					ClassReflector<?> reflector,
					ClassReflector<?> resolvingReflector) {
				A annotation = reflector.annotation(annotationClass);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}

			@Override
			protected List<A> atProperty(Class<A> annotationClass,
					Property property) {
				throw new UnsupportedOperationException();
			}
		}

		public static abstract class PropertyOnly<A extends Annotation>
				extends AbstractMergeStrategy.SingleResultMergeStrategy<A> {
			@Override
			protected List<A> atClass(Class<A> annotationClass,
					ClassReflector<?> reflector,
					ClassReflector<?> resolvingReflector) {
				throw new UnsupportedOperationException();
			}

			@Override
			protected List<A> atProperty(Class<A> annotationClass,
					Property property) {
				A annotation = property.annotation(annotationClass);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}
		}

		public static abstract class PropertyOrClass<A extends Annotation>
				extends AbstractMergeStrategy.SingleResultMergeStrategy<A> {
			@Override
			protected List<A> atClass(Class<A> annotationClass,
					ClassReflector<?> reflector,
					ClassReflector<?> resolvingReflector) {
				A annotation = reflector.annotation(annotationClass);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}

			@Override
			protected List<A> atProperty(Class<A> annotationClass,
					Property property) {
				A annotation = property.annotation(annotationClass);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}
		}
	}
}
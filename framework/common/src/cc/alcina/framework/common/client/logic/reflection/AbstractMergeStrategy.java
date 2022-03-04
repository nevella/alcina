package cc.alcina.framework.common.client.logic.reflection;

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

import cc.alcina.framework.common.client.logic.reflection.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

public abstract class AbstractMergeStrategy<A extends Annotation>
		implements MergeStrategy<A> {
	@Override
	public List<A> resolveClass(Class<A> annotationClass, Class<?> clazz,
			List<Inheritance> inheritance) {
		if (!inheritance.contains(Inheritance.CLASS)) {
			return Collections.emptyList();
		}
		List<A> result = new ArrayList<>();
		Set<Class> stack = new LinkedHashSet<>();
		Set<Class> visited = new LinkedHashSet<>();
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
			ClassReflector<?> reflector = Reflections.at(cursor);
			List<A> atClass = atClass(annotationClass, reflector);
			result = merge(result, atClass);
			if (inheritance.contains(Inheritance.INTERFACE)) {
				reflector.getInterfaces().stream().filter(this::permitPackages)
						.filter(visited::add).forEach(stack::add);
			}
		}
		return result;
	}

	@Override
	public List<A> resolveProperty(Class<A> annotationClass, Property property,
			List<Inheritance> inheritance) {
		if (!inheritance.contains(Inheritance.PROPERTY)) {
			return Collections.emptyList();
		}
		List<A> result = new ArrayList<>();
		Class cursor = property.getDefiningType();
		boolean includeErased = inheritance
				.contains(Inheritance.ERASED_PROPERTY);
		while (cursor != null) {
			Property cursorProperty = Reflections.at(cursor)
					.property(property.getName());
			if (cursorProperty != null) {
				if (!includeErased) {
					if (cursorProperty.getType() != property.getType()) {
						cursorProperty = null;
					}
				}
			}
			if (cursorProperty != null) {
				List<A> atProperty = atProperty(annotationClass,
						cursorProperty);
				result = merge(result, atProperty);
			}
			cursor = cursor.getSuperclass();
		}
		return result;
	}

	protected abstract List<A> atClass(Class<A> annotationClass,
			ClassReflector<?> reflector);

	protected abstract List<A> atProperty(Class<A> annotationClass,
			Property property);

	boolean permitPackages(Class clazz) {
		switch (clazz.getPackageName()) {
		case "javax.swing":
			return false;
		default:
			return true;
		}
	}

	public static abstract class AdditiveMergeStrategy<A extends Annotation>
			extends AbstractMergeStrategy<A> {
		@Override
		public List<A> merge(List<A> higher, List<A> lower) {
			if (higher.isEmpty()) {
				return lower;
			}
			if (lower.isEmpty()) {
				return higher;
			}
			return Stream.concat(higher.stream(), lower.stream())
					.collect(Collectors.toList());
		}
	}

	public static abstract class SingleResultMergeStrategy<A extends Annotation>
			extends AbstractMergeStrategy<A> {
		@Override
		public List<A> merge(List<A> higher, List<A> lower) {
			if (higher.isEmpty()) {
				return lower;
			}
			if (lower.isEmpty()) {
				return higher;
			}
			Preconditions.checkState(higher.size() == 1);
			return higher;
		}
	}
}
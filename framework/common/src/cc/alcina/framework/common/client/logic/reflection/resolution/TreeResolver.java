package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * A key part of dirndl - how annotations (declarative domain knowledge) are
 * modified by their context.
 *
 * See com.fasterxml.jackson.databind.ObjectMapper.addMixIn(Class<?>, Class<?>)
 * for a good alternative solution for straight-forward cases.
 *
 * Bit of an a-Ha moment - does this remind anyone of DNA transcription?
 * (particularly in the generative context of dirndl)?
 *
 * Strategy is:
 *
 * - most-specific class can simply return an imperative value
 *
 * - if not, pass to parent
 *
 * - if no overrides (resolution is null), resolve via annotation merge
 *
 * FIXME - dirndl1x1a - remove, but keep for historical reasons (replaced by
 * MergeStrategy, really)
 */
public class TreeResolver<A extends Annotation> {
	private TreeResolver<A> parent;

	private Class<A> annotationClass;

	private Predicate<A> mergeWithParent;

	private MultikeyMap cache = new UnsortedMultikeyMap<>(2);

	private MultikeyMap<TreeResolver<?>> finalChildren = new UnsortedMultikeyMap<>(
			2);

	public TreeResolver(Class<A> annotationClass,
			Predicate<A> mergeWithParent) {
		this.annotationClass = annotationClass;
		this.mergeWithParent = mergeWithParent;
	}

	public TreeResolver(TreeResolver<A> parent) {
		this.parent = parent;
		this.annotationClass = parent.annotationClass;
		this.mergeWithParent = parent.mergeWithParent;
	}

	public <TR extends TreeResolver<A>> TR finalChildResolver(Property property,
			Class discriminator, Supplier<TR> supplier) {
		return (TR) finalChildren.ensure((Supplier) supplier, property,
				discriminator);
	}

	public <T> T resolve(AnnotationLocation annotationLocation,
			Function<A, T> getter, String methodName, T defaultValue) {
		return (T) cache.ensure(() -> {
			Optional<T> imperative = resolveImperative(methodName);
			if (imperative != null) {
				return imperative.get();
			}
			return resolveAnnotationValue(annotationLocation, getter,
					methodName, defaultValue);
		}, annotationLocation, methodName);
	}

	// will be annotation values so guaranteed non-null
	private boolean areEqual(Object o1, Object o2) {
		if (o1.getClass().isArray()) {
			return Arrays.equals((Object[]) o1, (Object[]) o2);
		} else {
			return o1.equals(o2);
		}
	}

	private <T> T resolveAnnotationValue(AnnotationLocation annotationLocation,
			Function<A, T> getter, String methodName, T defaultValue) {
		T value = null;
		AnnotationLocation cursor = annotationLocation;
		while (cursor != null) {
			A resolved = resolveAnnotation(cursor, annotationLocation);
			if (resolved == null) {
				resolved = cursor.getAnnotation(annotationClass);
			}
			if (resolved != null) {
				T mergeValue = getter.apply(resolved);
				if (value == null) {
					value = mergeValue;
				} else {
					/*
					 * replace resolved value with parent value iff resolved
					 * value is default and non-array
					 *
					 * merge arrays if merge non-default
					 */
					boolean valueEqualsDefault = areEqual(value, defaultValue);
					boolean mergeEqualsDefault = areEqual(mergeValue,
							defaultValue);
					if (valueEqualsDefault) {
						value = mergeValue;
					} else {
						if (mergeEqualsDefault
								|| !mergeValue.getClass().isArray()) {
							// value does not change
						} else {
							Object[] valueArray = (Object[]) value;
							Object[] mergeArray = (Object[]) mergeValue;
							Object[] result = Arrays.copyOf(valueArray,
									valueArray.length + mergeArray.length);
							System.arraycopy(mergeArray, 0, result,
									valueArray.length, mergeArray.length);
							value = (T) result;
						}
					}
				}
				if (!mergeWithParent.test(resolved)) {
					break;
				}
			}
			cursor = cursor.parent();
		}
		return value;
	}

	protected boolean overrideChildren(Object resolvedValue) {
		return false;
	}

	protected A resolveAnnotation(AnnotationLocation location,
			AnnotationLocation startLocation) {
		if (parent == null) {
			return null;
		} else {
			return parent.resolveAnnotation(location, startLocation);
		}
	}

	protected <T> Optional<T> resolveImperative(String methodName) {
		if (parent == null) {
			return null;
		} else {
			return parent.resolveImperative(methodName);
		}
	}
}
package cc.alcina.framework.common.client.logic.reflection;

import java.util.Optional;
import java.util.function.Function;

public class TreeResolver<A> {
	private A leafValue;

	private TreeResolver<A> childResolver;

	protected AnnotationLocation annotationLocation;

	// a root resolver
	public TreeResolver() {
	}

	public TreeResolver(TreeResolver<A> childResolver) {
		this.childResolver = childResolver;
	}

	/*
	 * In the case where A is an annotationClass, the propertyReflector is
	 * the class/property tuple where that annotation occurs (i.e. it's the
	 * context of the evaluation)
	 */
	public TreeResolver(AnnotationLocation annotationLocation, A leafValue) {
		this.annotationLocation = annotationLocation;
		this.leafValue = leafValue;
	}

	public boolean hasValue() {
		return leafValue != null
				|| (childResolver != null && childResolver.hasValue());
	}
	public <T> T resolve(Function getter, String methodName) {
		return resolve(getter,methodName,null);
	}
	/*
	 * The spirit of this is:
	 * 
	 * resolvers override (per-property) if they return a non-empty optional from getValue
	 *  - they can also force override children. Will play with this for a bit then maybe elaborate the override strategies 
	 * 
	 * Implementation is (in a way) similar to that of a proxy - but this is GWT-safe.
	 * 
	 * @formatter:off
	 * dsd : resolve x
	 * dscd : resolve x
	 * ann: resolve x
	 * @formatter:on
	 * 
	 * the impl instance chain is constructed from lowest to highest, then evaluation starts at the top.
	 * 
	 * the root resolver then delegates down as low as it can to get a value
	 * 
	 */
	public <T> T resolve(Function getter, String methodName,T defaultValue) {
		return resolveDescendant(getter, methodName, Optional.empty(),defaultValue);
	}

	protected <T> Optional<T> getValue(Function getter, String methodName) {
		if (leafValue != null) {
			return (Optional<T>) Optional.of(getter.apply(leafValue));
		}
		return Optional.empty();
	}

	protected boolean overrideChildren(Object resolvedValue) {
		return false;
	}

	protected AnnotationLocation annotationLocation() {
		if (annotationLocation != null) {
			return annotationLocation;
		} else {
			return childResolver.annotationLocation();
		}
	}

	protected <T> T resolveDescendant(Function getter, String methodName,
			Optional<T> resolvedValue, T defaultValue) {
		Optional<T> value = getValue(getter, methodName);
		if (value.isPresent()) {
			resolvedValue = value;
		}
		if (overrideChildren(resolvedValue)) {
			return resolvedValue.get();
		}
		if (childResolver == null) {
			return resolvedValue.orElse(defaultValue);
		}
		return childResolver.resolveDescendant(getter, methodName,
				resolvedValue,defaultValue);
	}
}
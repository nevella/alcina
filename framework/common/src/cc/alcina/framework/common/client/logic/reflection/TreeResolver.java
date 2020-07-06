package cc.alcina.framework.common.client.logic.reflection;

import java.util.Optional;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.PropertyReflector.Location;

public class TreeResolver<A> {
	private A leafValue;

	private TreeResolver<A> childResolver;

	protected Location propertyLocation;

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
	public TreeResolver(Location propertyLocation, A leafValue) {
		this.propertyLocation = propertyLocation;
		this.leafValue = leafValue;
	}

	public boolean hasValue() {
		return leafValue != null
				|| (childResolver != null && childResolver.hasValue());
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
	public <T> T resolve(Function getter, String methodName) {
		return resolveDescendant(getter, methodName, Optional.empty());
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

	protected Location propertyLocation() {
		if (propertyLocation != null) {
			return propertyLocation;
		} else {
			return childResolver.propertyLocation();
		}
	}

	protected <T> T resolveDescendant(Function getter, String methodName,
			Optional<T> resolvedValue) {
		Optional<T> value = getValue(getter, methodName);
		if (value.isPresent()) {
			resolvedValue = value;
		}
		if (overrideChildren(resolvedValue)) {
			return resolvedValue.get();
		}
		if (childResolver == null) {
			return resolvedValue.orElse(null);
		}
		return childResolver.resolveDescendant(getter, methodName,
				resolvedValue);
	}
}
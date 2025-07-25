package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/*
 * FIXME - dirndl 1x1g - probably move resolution caching from the resolver to
 * here, since more performant (aka ThreadLocal )
 *
 * Note - the above may be wrong (use a profiler!) - see instead (possibly)
 * ReflectiveSerializer use of TypeNode/PropertyNode
 *
 * See particularly the ResolutionState inner class. For a given
 * property/actualtype/ResolutionState.parent tuple, it currently manages
 * 'consumed earlier in the chain' annotations and could be used as a caching
 * mechanism
 * 
 * Thread-saftey: not threadsafe
 *
 *
 */
public class AnnotationLocation {
	public Property property;

	/*
	 *
	 * The type to resolve annotations from if they don't exist on the property
	 * (or to merge with the property annotation if using a complex merge
	 * strategy).
	 *
	 * This can differ depending on the algorithm - here are some current use
	 * cases:
	 *
	 * - falling back on the class containing the property as the 'default
	 * provider' - DomainStore DomainProperty resolution
	 *
	 * - use the property type - XML document parsing (GWT-compatible JAXB,
	 * effectively)
	 *
	 * - use the property value type - dirndl
	 */
	public Class<?> classLocation;

	protected Resolver resolver;

	private ResolutionState resolutionState;

	public ResolutionState ensureResolutionState() {
		if (resolutionState == null) {
			resolutionState = new ResolutionState();
		}
		return resolutionState;
	}

	public ResolutionState getResolutionState() {
		return resolutionState;
	}

	public void setResolutionState(ResolutionState resolutionState) {
		this.resolutionState = resolutionState;
	}

	protected AnnotationLocation() {
	}

	public AnnotationLocation(Class clazz, Property property) {
		this(clazz, property, null);
	}

	public AnnotationLocation(Class clazz, Property property,
			Resolver resolver) {
		Preconditions.checkArgument(clazz != null || property != null);
		this.classLocation = clazz;
		this.property = property;
		if (resolver == null) {
			resolver = Resolver.get();
		}
		this.resolver = resolver;
	}

	public AnnotationLocation copyWithClassLocation(Class<?> clazz) {
		AnnotationLocation location = new AnnotationLocation(clazz, property,
				resolver);
		location.ensureResolutionState().setTransformationParent(this);
		return location;
	}

	public AnnotationLocation copyWithClassLocationOf(Object object) {
		return copyWithClassLocation(object == null ? null : object.getClass());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationLocation) {
			AnnotationLocation o = (AnnotationLocation) obj;
			return property == o.property && classLocation == o.classLocation
					&& Objects.equals(resolver, o.resolver)
					&& Objects.equals(resolutionState, o.resolutionState);
		} else {
			return false;
		}
	}

	/*
	 * FIXME - dirndl 1x3 resolution is inexact. What we really want to denote
	 * is 'ignore annotations on the property' - and just possibly 'ignore
	 * annotations on the class if the resolution parent is the same class' -
	 * rather than just 'ignore if seen'. It is, however, good enough for all
	 * current use cases
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		A resolvedAnnotation = resolver.resolveAnnotation(annotationClass,
				this);
		if (resolvedAnnotation == null || resolutionState == null
				|| resolutionState.consumed == null) {
			return resolvedAnnotation;
		}
		if (!resolutionState.consumed.contains(resolvedAnnotation)) {
			return resolvedAnnotation;
		} else {
			return null;
		}
	}

	public <A extends Annotation> Optional<A>
			getAnnotationOptional(Class<A> annotationClass) {
		return Optional.ofNullable(getAnnotation(annotationClass));
	}

	public <A extends Annotation> Optional<A>
			optional(Class<A> annotationClass) {
		return Optional.ofNullable(getAnnotation(annotationClass));
	}

	private <A extends Annotation> A getAnnotation0(Class<A> annotationClass) {
		if (property != null) {
			A annotation = property.annotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		if (classLocation == null) {
			return null;
		} else {
			ClassReflector<?> classReflector = Reflections.at(classLocation);
			if (classReflector == null) {
				return null;
			}
			return (A) classReflector.annotation(annotationClass);
		}
	}

	public <A extends Annotation> List<A>
			getAnnotations(Class<A> annotationClass) {
		return resolver.resolveAnnotations(annotationClass, this);
	}

	public Resolver getResolver() {
		return this.resolver;
	}

	public <A extends Annotation> boolean
			hasAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	public boolean hasAny(Class<? extends Annotation>... annotationClasses) {
		return Arrays.stream(annotationClasses).anyMatch(
				annotationClass -> getAnnotation(annotationClass) != null);
	}

	int hash = 0;

	@Override
	public int hashCode() {
		long n1 = System.nanoTime();
		boolean miss = false;
		if (hash == 0) {
			miss = true;
			hash ^= property == null ? 0 : property.hashCode();
			hash ^= classLocation == null ? 0 : classLocation.hashCode();
			hash ^= resolver == null ? 0 : resolver.hashCode();
			hash ^= resolutionState == null ? 0 : resolutionState.hashCode();
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	public boolean isDefiningType(Class<?> clazz) {
		return property != null && property.getOwningType() == clazz;
	}

	public boolean isPropertyName(String propertyName) {
		return property != null && propertyName.equals(property.getName());
	}

	public boolean isPropertyType(Class<?> clazz) {
		return property != null && clazz == property.getType();
	}

	public AnnotationLocation parent() {
		if (property != null) {
			if (classLocation != null) {
				return new AnnotationLocation(classLocation, null, resolver);
			} else {
				return null;
			}
		}
		if (classLocation.getSuperclass() != null) {
			return new AnnotationLocation(classLocation.getSuperclass(), null,
					resolver);
		}
		return null;
	}

	public void setResolver(Resolver resolver) {
		this.resolver = resolver;
	}

	public String toPropertyString() {
		if (property != null) {
			return Ax.format("%s.%s", classLocation.getSimpleName(),
					property.getName());
		} else {
			return classLocation.getSimpleName();
		}
	}

	@Override
	public String toString() {
		if (property != null) {
			String declaringSuffix = property.getOwningType() == classLocation
					|| classLocation == null ? ""
							: Ax.format(" :: [%s]",
									classLocation.getSimpleName());
			return Ax.format("%s%s", property.toString(), declaringSuffix);
		} else {
			return classLocation.getSimpleName();
		}
	}

	/*
	 * Handles annotation resolution at the same Property but with a different
	 * ClassLocation (due to transformation of the Property value - currently
	 * the only client is dirndl/transform). AnnotationLocations form a sequence
	 * (modelled by Resolution.transformationParent)
	 *
	 * Annotations on the property are 'consumed' by a given AnnotationLocation
	 * and are not available to s descendant locations
	 *
	 * FIXME - dirndl 1x3 - define the rules (current dirndl behaviour works but
	 * is not really formalised, and is implemented to a degree in the
	 * transforming DirectedRenderer subclasses):
	 *
	 * - property annotations (as merge inputs) should be consumed by first
	 * descendant level use
	 *
	 * - rule defining behaviour in resolvedPropertyAnnotations
	 *
	 * - examples
	 *
	 * - note that hashcode/equals do *not* depend on the parent - there's
	 * probably some formal logic out there explaining why this is always true
	 */
	public static class ResolutionState {
		public AnnotationLocation transformationParent;

		// if an annotation type has been resolved in the transformation chain,
		// the merge strategy may ignore later annotations
		public List<Annotation> resolvedPropertyAnnotations = null;

		// if an annotation type has been consumed in the transformation chain,
		// the merge strategy may ignore later annotations
		private List<Annotation> consumed = null;

		// will only be applied to a thread-specific instance (constructed via
		// copyWithClassLocationOf)
		//
		// FIXME - dirndl 1x3 - this should be applied during resolution, not
		// getAnnotation() - probably works, but disallows Transform ->
		// Transform. That said, Transform -> Transform may be getting a bit
		// outré anyway
		public void addConsumed(Annotation annotation) {
			if (annotation == null) {
				return;
			}
			if (consumed == null) {
				consumed = new ArrayList<>();
			}
			consumed.add(annotation);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ResolutionState) {
				ResolutionState o = (ResolutionState) obj;
				return // transformationParent == o.transformationParent &&
				Objects.equals(resolvedPropertyAnnotations,
						o.resolvedPropertyAnnotations)
						&& Objects.equals(consumed, o.consumed);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// don't use parent in a hash
			return Objects.hash(resolvedPropertyAnnotations, consumed);
			// return Objects.hash(transformationParent,
			// resolvedPropertyAnnotations, consumed);
		}

		void setTransformationParent(AnnotationLocation annotationLocation) {
			// FIXME - dirndl 1x1g - optimise. probably copy-on-write, and share
			// the location across say all transformation children
			transformationParent = annotationLocation;
			transformationParent.ensureResolutionState();
			if (annotationLocation.resolutionState.resolvedPropertyAnnotations != null) {
				resolvedPropertyAnnotations = new ArrayList<>();
				resolvedPropertyAnnotations.addAll(
						annotationLocation.resolutionState.resolvedPropertyAnnotations);
			}
			if (annotationLocation.resolutionState.consumed != null) {
				consumed = new ArrayList<>();
				consumed.addAll(annotationLocation.resolutionState.consumed);
			}
		}
	}

	/**
	 * <p>
	 * Resolves annotations (really, returns declarative information packaged in
	 * annotation instances) at a class/property location
	 *
	 * <p>
	 * Concurrent via access, since resolveAnnotations() is synchronized
	 *
	 */
	public static abstract class Resolver {
		private static AnnotationLocation.Resolver resolver;

		public static AnnotationLocation.Resolver get() {
			// no need to sync/double-check, since the block body is idempotent
			if (resolver == null) {
				/*
				 * Must be bootstrapped by imperative code
				 */
				resolver = Registry.impl(AnnotationLocation.Resolver.class);
			}
			return resolver;
		}

		private MultikeyMap<List<? extends Annotation>> resolvedCache = new UnsortedMultikeyMap<>(
				2);

		protected MultikeyMap<List<? extends Annotation>> resolvedCache() {
			return resolvedCache;
		}

		/*
		 * For debugging
		 */
		public void clearResolutionCache() {
			resolvedCache.clear();
		}

		/**
		 * When a given annotationlocation can have multiple values (depending
		 * on, say, document i/o type), override to return the appropriate
		 * annotation
		 */
		public <A extends Annotation> A contextAnnotation(
				HasAnnotations reflector, Class<A> clazz,
				ResolutionContext resolutionContext) {
			return reflector.annotation(clazz);
		}

		/**
		 * Don't (can't) override this - override resolveAnnotations
		 */
		public final <A extends Annotation> A resolveAnnotation(
				Class<A> annotationClass, AnnotationLocation location) {
			return (A) Ax.first(resolveAnnotations(annotationClass, location));
		}

		public synchronized <A extends Annotation> List<A> resolveAnnotations(
				Class<A> annotationClass, AnnotationLocation location) {
			return (List<A>) resolvedCache().ensure(() -> {
				return resolveAnnotations0(annotationClass, location);
			}, location, annotationClass);
		}

		/*
		 * Simplistic implementation, doesn't respect @Resolution
		 */
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			A ensured = location.getAnnotation0(annotationClass);
			return ensured == null ? Collections.emptyList()
					: Collections.singletonList(ensured);
		}

		public enum ResolutionContext {
			Strategy
		}
	}

	public AnnotationLocation copyWithResolver(Resolver replaceResolver) {
		AnnotationLocation location = new AnnotationLocation(classLocation,
				property, replaceResolver);
		return location;
	}

	public boolean hasProperty() {
		return property != null;
	}
}

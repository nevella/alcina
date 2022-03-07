package cc.alcina.framework.common.client.domain;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.TreeResolver;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
public @interface DomainStoreProperty {
	Class<? extends DomainStorePropertyLoadOracle> customLoadOracle() default DomainStorePropertyLoadOracle.class;

	boolean ignoreMismatchedCollectionModifications() default false;

	DomainStorePropertyLoadType loadType();

	/*
	 * false requires loadType EAGER
	 */
	boolean optimiseOneToManyCollectionModifications() default true;

	public static class DomainStorePropertyLoadOracle<E extends Entity> {
		public boolean shouldLoad(E entity, boolean duringWarmup) {
			return false;
		}
	}

	public enum DomainStorePropertyLoadType {
		// First two values should be accompanied by
		// @DomainTransformPropagation(PropagationType.NONE)
		TRANSIENT, LAZY, CUSTOM,
		//
		EAGER;
	}

	public static class DomainStorePropertyResolver
			implements DomainStoreProperty {
		protected TreeResolver<DomainStoreProperty> resolver;

		private AnnotationLocation location;

		public DomainStorePropertyResolver(
				TreeResolver<DomainStoreProperty> resolver,
				AnnotationLocation location) {
			this.resolver = resolver;
			this.location = location;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return DomainStoreProperty.class;
		}

		@Override
		public Class<? extends DomainStorePropertyLoadOracle>
				customLoadOracle() {
			Function<DomainStoreProperty, Class<? extends DomainStorePropertyLoadOracle>> function = DomainStoreProperty::customLoadOracle;
			return resolver.resolve(location, function, "customLoadOracle",
					DomainStorePropertyLoadOracle.class);
		}

		public boolean hasValue() {
			return loadType() != null;
		}

		@Override
		public boolean ignoreMismatchedCollectionModifications() {
			Function<DomainStoreProperty, Boolean> function = DomainStoreProperty::ignoreMismatchedCollectionModifications;
			return resolver.resolve(location, function,
					"ignoreMismatchedCollectionModifications", false);
		}

		@Override
		public DomainStorePropertyLoadType loadType() {
			Function<DomainStoreProperty, DomainStorePropertyLoadType> function = DomainStoreProperty::loadType;
			return resolver.resolve(location, function, "loadType",
					DomainStorePropertyLoadType.TRANSIENT);
		}

		@Override
		public boolean optimiseOneToManyCollectionModifications() {
			Function<DomainStoreProperty, Boolean> function = DomainStoreProperty::optimiseOneToManyCollectionModifications;
			return resolver.resolve(location, function,
					"optimiseOneToManyCollectionModifications", true);
		}

		protected TreeResolver<DomainStoreProperty>
				createResolver(TreeResolver<DomainStoreProperty> resolver) {
			return new TreeResolver<DomainStoreProperty>(resolver);
		}
	}
}

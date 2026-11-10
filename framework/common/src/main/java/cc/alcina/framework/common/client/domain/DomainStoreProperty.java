package cc.alcina.framework.common.client.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.domain.Entity;

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
}

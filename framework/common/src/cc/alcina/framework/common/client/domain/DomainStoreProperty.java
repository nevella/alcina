package cc.alcina.framework.common.client.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
public @interface DomainStoreProperty {
	boolean ignoreMismatchedCollectionModifications() default false;

	DomainStorePropertyLoadType loadType() default DomainStorePropertyLoadType.TRANSIENT;

	public enum DomainStorePropertyLoadType {
		TRANSIENT, LAZY, EAGER;
	}
}

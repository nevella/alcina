package cc.alcina.framework.entity.entityaccess.cache;

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
	DomainStorePropertyLoadType loadType() default DomainStorePropertyLoadType.TRANSIENT;

	String toIdProperty() default "";

	boolean translateObjectWritesToIdWrites() default false;

	public enum DomainStorePropertyLoadType {
		TRANSIENT, LAZY, EAGER
	}
}

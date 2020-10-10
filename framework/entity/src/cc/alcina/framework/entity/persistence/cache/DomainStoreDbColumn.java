package cc.alcina.framework.entity.persistence.cache;

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
/**
 * Retrieve from db - custom handling of db column
 * 
 * @author nick@alcina.cc
 *
 */
public @interface DomainStoreDbColumn {
	boolean customHandler() default false;

	String mappedBy() default "";

	Class targetEntity() default void.class;
}

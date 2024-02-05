package cc.alcina.framework.common.client.relational;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a package or type as relational code
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE, ElementType.PACKAGE })
public @interface RelationalCode {
}

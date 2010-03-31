package cc.alcina.framework.common.client.logic.permissions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value = { ElementType.TYPE })
public @interface PermissibleChildClasses {
	Class[] value() default {};
}

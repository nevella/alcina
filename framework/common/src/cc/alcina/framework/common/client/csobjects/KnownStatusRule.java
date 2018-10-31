package cc.alcina.framework.common.client.csobjects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface KnownStatusRule {
	public KnownTagAlcina area() default KnownTagAlcina.Area_Devops;

	public double errorValue() default 0;

	public KnownStatusRuleName name();

	public double warnValue() default 0;
}

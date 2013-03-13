package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Documented

public @interface Documentation {
	public String value();
	public boolean html() default false;
}

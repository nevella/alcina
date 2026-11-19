package cc.alcina.framework.gwt.client.dirndl.resolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

/**
 * Declarative UI permissions
 */
public class UiPermission {
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Inherited
	@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
	public @interface Access {
		Permission value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Inherited
	@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
	public @interface Visible {
		Permission value();
	}
}

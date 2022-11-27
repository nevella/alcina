package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@ClientVisible
@Inherited
public @interface DirectedContextResolver {
	Class<? extends ContextResolver> value();
}

package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.util.ToStringFunction;

@Retention(RetentionPolicy.RUNTIME)
@Documented
// in fact, should only be an inner annotation for @Directed
@Target(ElementType.TYPE_USE)
@ClientVisible
public @interface Binding {
	String from() default "";

	String literal() default "";

	String to() default "";

	Type type();

	Class<? extends ToStringFunction> transform() default ToStringFunction.Identity.class;

	@ClientInstantiable
	public enum Type {
		PROPERTY, INNER_HTML, INNER_TEXT, CSS_CLASS, TOGGLE_CSS_CLASS,
		STYLE_ATTRIBUTE, SWITCH_CSS_CLASS;
	}
}

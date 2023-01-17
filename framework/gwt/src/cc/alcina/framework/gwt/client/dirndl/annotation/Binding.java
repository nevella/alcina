package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;
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

	Class<? extends ToStringFunction> transform() default ToStringFunction.Identity.class;

	Type type();

	@Reflected
	public static class DisplayFalseTrue implements ToStringFunction<Boolean> {
		@Override
		public String apply(Boolean t) {
			return CommonUtils.bv(t) ? "block" : "none";
		}
	}

	@Reflected
	public enum Type {
		PROPERTY, INNER_HTML, INNER_TEXT, CSS_CLASS, STYLE_ATTRIBUTE,
		SWITCH_CSS_CLASS, PANEL_CHANGED;
	}

	@Reflected
	public static class UnitPx implements ToStringFunction<Integer> {
		@Override
		public String apply(Integer px) {
			return px + "px";
		}
	}

	@Reflected
	public static class UnitRem implements ToStringFunction<Integer> {
		@Override
		public String apply(Integer rem) {
			return rem + "rem";
		}
	}

	@Reflected
	public static class VisibilityVisibleHidden
			implements ToStringFunction<Boolean> {
		@Override
		public String apply(Boolean t) {
			return CommonUtils.bv(t) ? "visible" : "hidden";
		}
	}
}

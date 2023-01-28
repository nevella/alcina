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
		PROPERTY, INNER_HTML, INNER_TEXT,
		/*
		 * applies to boolean properties, adds a css class of name css-ified
		 * propertyName to the element if property is true
		 *
		 * e.g. @Binding(type = Type.CSS_CLASS, from = "selected") -- if
		 * property 'selected' is true, will render as <x class="selected"/>
		 */
		CSS_CLASS,
		/*
		 * applies to String properties, adds a style attributed of name
		 * css-ified propertyName to the element if property is non-null
		 *
		 * e.g. @Binding(type = Type.STYLE_ATTRIBUTE, from = "backgroundColor")
		 * -- property backgroundColor="#99cccc" will render as <x
		 * style="background-color: #99cccc;"/>
		 */
		STYLE_ATTRIBUTE, SWITCH_CSS_CLASS,
		// sugar for type=PROPERTY,to="class" -- because we can't have a source
		// property named 'class', natch
		CLASS_PROPERTY;
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

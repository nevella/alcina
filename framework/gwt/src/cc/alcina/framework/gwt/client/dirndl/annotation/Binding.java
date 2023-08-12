package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FromStringFunction;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@Retention(RetentionPolicy.RUNTIME)
@Documented
// Use as a field of @Directed, or on a property
@Target({ ElementType.METHOD, ElementType.FIELD })
@ClientVisible
public @interface Binding {
	String from() default "";

	String literal() default "";

	String to() default "";

	Class<? extends ToStringFunction> transform() default ToStringFunction.Identity.class;

	Type type();

	public abstract static class AbstractContextSensitiveReverseTransform<T>
			implements ContextSensitiveReverseTransform<T> {
		protected Node node;

		@Override
		public ContextSensitiveReverseTransform<T> withContextNode(Node node) {
			this.node = node;
			return this;
		}
	}

	@Reflected
	public abstract static class AbstractContextSensitiveTransform<T>
			implements ContextSensitiveTransform<T> {
		protected Node node;

		@Override
		public ContextSensitiveTransform<T> withContextNode(Node node) {
			this.node = node;
			return this;
		}
	}

	@Reflected
	public interface Bidi<T> extends ToStringFunction.Bidi<T> {
	}

	public interface ContextSensitiveReverseTransform<T>
			extends FromStringFunction<T> {
		public ContextSensitiveReverseTransform<T>
				withContextNode(DirectedLayout.Node node);
	}

	public interface ContextSensitiveTransform<T> extends ToStringFunction<T> {
		public ContextSensitiveTransform<T>
				withContextNode(DirectedLayout.Node node);
	}

	@Reflected
	public static class DisplayFalseTrue implements ToStringFunction<Boolean> {
		@Override
		public String apply(Boolean t) {
			return CommonUtils.bv(t) ? "block" : "none";
		}
	}

	public static class Impl implements Binding {
		public static final Impl DEFAULT_INSTANCE = new Binding.Impl();

		public static Impl propertyBinding(Property property, Binding binding) {
			Impl impl = new Impl(binding);
			impl.from = property.getName();
			return impl;
		}

		String from = "";

		String literal = "";

		String to = "";

		Class<? extends ToStringFunction> transform = ToStringFunction.Identity.class;

		Type type;

		public Impl() {
		}

		public Impl(Binding binding) {
			this.from = binding.from();
			this.literal = binding.literal();
			this.to = binding.to();
			this.transform = binding.transform();
			this.type = binding.type();
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Binding.class;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Impl)) {
				return false;
			}
			Impl other = (Impl) obj;
			return Objects.equals(this.from, other.from)
					&& Objects.equals(this.literal, other.literal)
					&& Objects.equals(this.to, other.to)
					&& Objects.equals(this.transform, other.transform)
					&& this.type == other.type;
		}

		@Override
		public String from() {
			return from;
		}

		@Override
		public int hashCode() {
			return Objects.hash(from, literal, to, transform, type);
		}

		@Override
		public String literal() {
			return literal;
		}

		@Override
		public String to() {
			return to;
		}

		@Override
		public String toString() {
			return toString(false);
		}

		public String toStringElideDefaults() {
			return toString(true);
		}

		@Override
		public Class<? extends ToStringFunction> transform() {
			return transform;
		}

		@Override
		public Type type() {
			return type;
		}

		private String __stringValue(Object o) {
			if (o instanceof Class) {
				return ((Class) o).getSimpleName() + ".class";
			}
			if (o.getClass().isArray()) {
				return "[" + java.util.Arrays.stream((Object[]) o)
						.map(this::__stringValue).collect(
								java.util.stream.Collectors.joining(","))
						+ "]";
			}
			return o.toString();
		}

		private void append(StringBuilder stringBuilder, String fieldName,
				Function<Impl, ?> function, boolean elideDefaults) {
			Object value = function.apply(this);
			if (elideDefaults) {
				Object defaultValue = function.apply(DEFAULT_INSTANCE);
				if (Objects.deepEquals(value, defaultValue)) {
					return;
				}
			}
			if (stringBuilder.length() > 0) {
				stringBuilder.append(',');
			}
			stringBuilder.append(fieldName);
			stringBuilder.append('=');
			stringBuilder.append(__stringValue(value));
		}

		String toString(boolean elideDefaults) {
			StringBuilder stringBuilder = new StringBuilder();
			append(stringBuilder, "from", Impl::from, elideDefaults);
			append(stringBuilder, "literal", Impl::literal, elideDefaults);
			append(stringBuilder, "to", Impl::to, elideDefaults);
			append(stringBuilder, "transform", Impl::transform, elideDefaults);
			append(stringBuilder, "type", Impl::type, elideDefaults);
			return stringBuilder.toString();
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
	public static class UnitPercent implements ToStringFunction<Integer> {
		@Override
		public String apply(Integer percent) {
			return percent + "%";
		}
	}

	@Reflected
	public static class UnitPx implements ToStringFunction<Integer> {
		@Override
		public String apply(Integer px) {
			return px + "px";
		}
	}

	@Reflected
	public static class UnitRem implements ToStringFunction<Number> {
		@Override
		public String apply(Number rem) {
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

package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface ValueTransformer {
	/**
	 * The value transformer
	 */
	Class<? extends Function> value();

	public static class Impl implements ValueTransformer {
		Class<? extends Function<?, ?>> value;

		public Impl(Class<? extends Function<?, ?>> value) {
			this.value = value;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return ValueTransformer.class;
		}

		@Override
		public Class<? extends Function<?, ?>> value() {
			return value;
		}
	}
}
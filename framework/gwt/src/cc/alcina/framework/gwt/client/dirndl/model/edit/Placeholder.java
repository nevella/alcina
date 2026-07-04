package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Placeholder {
	/**
	 * The placeholder
	 */
	String value();

	public static class Impl implements Placeholder {
		private String value;

		@Override
		public Class<? extends Annotation> annotationType() {
			return Placeholder.class;
		}

		@Override
		public String value() {
			return value;
		}

		public Impl withValue(String value) {
			this.value = value;
			return this;
		}
	}
}
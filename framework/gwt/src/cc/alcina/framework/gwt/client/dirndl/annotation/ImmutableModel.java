package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@ClientVisible
public @interface ImmutableModel {
	public static class Impl implements ImmutableModel {
		public static final Impl INSTANCE = new Impl();

		@Override
		public Class<? extends Annotation> annotationType() {
			return ImmutableModel.class;
		}
	}
}

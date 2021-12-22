package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public @interface ReflectiveAccess {
	Access[] value() default Access.ALL;

	public static class Support {
		public static boolean has(ReflectiveAccess access, Access test) {
			return Arrays.stream(access.value()).anyMatch(v -> v == test);
		}
	}

	public static final class DefaultValue implements ReflectiveAccess {
		@Override
		public Class<? extends Annotation> annotationType() {
			return ReflectiveAccess.class;
		}

		@Override
		public Access[] value() {
			return Access.ALL;
		}
	}

	public enum Access {
		 CLASS, PROPERTIES, ANNOTATIONS;

		public static final Access[] ALL = { CLASS, PROPERTIES, ANNOTATIONS };
	}
}

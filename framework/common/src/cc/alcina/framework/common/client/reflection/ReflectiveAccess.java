package cc.alcina.framework.common.client.reflection;

public @interface ReflectiveAccess {
	Access[] value() default Access.ALL;

	public enum Access {
		CLASS, PROPERTIES, ANNOTATIONS;

		public static final Access[] ALL = { CLASS, PROPERTIES, ANNOTATIONS };
	}
}

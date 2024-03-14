package cc.alcina.framework.entity;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class ClassUtil {
	public static final Function<Class, Object> NO_ARGS_INSTANTIATOR = clazz -> {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	};

	public static final String getRootClasspathElement(Class clazz) {
		return clazz.getProtectionDomain().getCodeSource().getLocation()
				.toString()//
				.replaceFirst("(file:)(.+)", "$2");
	}
}

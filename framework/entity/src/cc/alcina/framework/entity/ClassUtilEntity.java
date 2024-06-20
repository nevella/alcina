package cc.alcina.framework.entity;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class ClassUtilEntity {
	public static final Function<Class, Object> NO_ARGS_INSTANTIATOR = clazz -> {
		try {
			Constructor ctr = clazz.getDeclaredConstructor();
			ctr.setAccessible(true);
			return ctr.newInstance();
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

package cc.alcina.framework.common.client.reflection;

import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Reflections {
	private static Reflections theInstance;

	private Map<Class, ClassReflector> reflectors = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	private Map<String, Class> forName = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	public static <T> ClassReflector<T> at(Class<T> clazz) {
		return get().reflectors.computeIfAbsent(clazz,
				c -> ClassReflectorProvider.getClassReflector(clazz));
	}

	public static <T> Class<T> forName(String fqn) {
		if (fqn == null) {
			return null;
		}
		// FIXME - reflection - populate the forName map on init
		switch (fqn) {
		case "boolean":
			return (Class<T>) boolean.class;
		case "byte":
			return (Class<T>) byte.class;
		case "short":
			return (Class<T>) short.class;
		case "int":
			return (Class<T>) int.class;
		case "long":
			return (Class<T>) long.class;
		case "float":
			return (Class<T>) float.class;
		case "double":
			return (Class<T>) double.class;
		case "char":
			return (Class<T>) char.class;
		case "void":
			return (Class<T>) void.class;
		}
		return get().forName.computeIfAbsent(fqn, ForName::forName);
	}

	public static String getApplicationName() {
		return get().applicationName;
	}

	public static boolean isAssignableFrom(Class from, Class to) {
		return at(to).isAssignableTo(from);
	}

	public static boolean isEffectivelyFinal(Class clazz) {
		return ClassReflector.stdAndPrimitivesMap.containsKey(clazz.getName())
				|| CommonUtils.isEnumOrEnumSubclass(clazz);
	}

	public static <T> T newInstance(Class<T> clazz) {
		return at(clazz).newInstance();
	}

	public static <T> T newInstance(String className) {
		return (T) at(forName(className)).newInstance();
	}

	public static void setApplicationName(String applicationName) {
		get().applicationName = applicationName;
	}

	private static Reflections get() {
		return theInstance;
	}

	public static void init() {
		ForName.init();
		theInstance = new Reflections();
	}

	private String applicationName = "app";

	public void appShutdown() {
		theInstance = null;
	}
}

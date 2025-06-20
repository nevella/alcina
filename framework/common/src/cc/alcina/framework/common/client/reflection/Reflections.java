package cc.alcina.framework.common.client.reflection;

import java.util.Map;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableValueModel;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Reflections {
	private static Reflections theInstance;

	public static <T> ClassReflector<T> at(Class<T> clazz) {
		return get().reflectors.computeIfAbsent(clazz,
				c -> ClassReflectorProvider.getClassReflector(clazz));
	}

	public static <T> ClassReflector<T> at(T instance) {
		return (ClassReflector<T>) at(instance.getClass());
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
		case "[B":
			return (Class<T>) byte[].class;
		}
		return get().forName.computeIfAbsent(fqn, ForName::forName);
	}

	private static Reflections get() {
		return theInstance;
	}

	public static String getApplicationName() {
		return get().applicationName;
	}

	// here for android compatibility
	public static String getPackageName(Class clazz) {
		Package _package = clazz.getPackage();
		return _package == null ? "" : _package.getName();
	}

	public static void init() {
		ForName.init();
		theInstance = new Reflections();
	}

	public static boolean isAssignableFrom(Class from, Class to) {
		return get().assignableFrom.isAssignableFrom(from, to);
	}

	public static boolean isEffectivelyFinal(Class clazz) {
		return ClassReflector.stdAndPrimitivesMap.containsKey(clazz.getName())
				|| ClassUtil.isEnumOrEnumSubclass(clazz) || (clazz.isArray()
						&& isEffectivelyFinal(clazz.getComponentType()));
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

	private AssignableFrom assignableFrom = new AssignableFrom();

	class AssignableFrom {
		public boolean isAssignableFrom(Class from, Class to) {
			if (from == to) {
				return true;
			}
			if (to.isPrimitive()) {
				return false;
			}
			Map<Class, Boolean> forClass = resolvedAssignable
					.computeIfAbsent(from, clazz -> CollectionCreators.Bootstrap
							.createConcurrentClassMap());
			return forClass.computeIfAbsent(to,
					clazz -> hasReflectionMetadata(to)
							&& at(to).isAssignableTo(from));
		}

		Map<Class, Map<Class, Boolean>> resolvedAssignable = CollectionCreators.Bootstrap
				.createConcurrentClassMap();
	}

	private Map<Class, ClassReflector> reflectors = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	private Map<String, Class> forName = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	private Map<Class, Boolean> hasReflectionMetadata = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	private String applicationName = "app";

	public void appShutdown() {
		theInstance = null;
	}

	/*
	 * private because not generally encouraged (code shouldn't have to check) -
	 * used by isAssignableFrom which is already, in a way, a metadata check
	 */
	private boolean hasReflectionMetadata(Class clazz) {
		return hasReflectionMetadata.computeIfAbsent(clazz,
				c -> forName(c.getName()) != null);
	}

	public static <T> T propertyClone(T source) {
		T target = at(source).newInstance();
		ClassReflector.copyProperties(source, target, p -> true);
		return target;
	}
}

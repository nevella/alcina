package cc.alcina.framework.common.client.reflection;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class ClientReflections {
	public static final String DEV_MODE_REFLECTOR = ClientReflections.class
			.getName() + ".DEV_MODE_REFLECTOR";

	static Map<Class, Supplier<ClassReflector>> perClassReflectorSuppliers = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	static Map<String, Class> forNames = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	static Map<Class, Map<Class, Boolean>> assignableTo = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public static Class<?> forName(String fqn) {
		return forNames.get(fqn);
	}

	public static ClassReflector<?> getClassReflector(Class clazz) {
		Supplier<ClassReflector> supplier = perClassReflectorSuppliers
				.get(clazz);
		if (supplier == null) {
			if (clazz.getName().startsWith("java.") || clazz.isPrimitive()) {
				// non-public internal class, either GWT or JDK, e.g.
				// Arrays$ArrayList - or primitive
				return ClassReflector.emptyReflector(clazz);
			}
			// if (!GWT.isScript()) {
			// // FIXME - reflection - although this is fancy, better to add
			// // all unknown reflectables to UNKNOWN on build of Initial and
			// // include in initial if dev mode
			// //
			// // effectively call back into the generator
			// System.setProperty(DEV_MODE_REFLECTOR, clazz.getName());
			// DevModeReflector devModeReflector = GWT
			// .create(DevModeReflector.class);
			// register(devModeReflector);
			// supplier = perClassReflectorSuppliers.get(clazz);
			// if (supplier != null) {
			// return supplier.get();
			// }
			// }
			throw new NoSuchElementException(
					"No reflector for " + clazz.getName());
		}
		return supplier.get();
	}

	public static boolean isAssignableFrom(Class from, Class to) {
		return computeToMap(to).containsKey(from);
	}

	public static void register(ModuleReflector reflector) {
		reflector.registerReflectorSuppliers(perClassReflectorSuppliers);
		reflector.registerForNames(forNames);
		reflector.registerRegistrations();
	}

	private static Map<Class, Boolean> computeToMap(Class to) {
		Map<Class, Boolean> map = assignableTo.get(to);
		if (map != null) {
			return map;
		}
		// monitor ensures only one thread populates - but concurrent reads are
		// fine
		synchronized (assignableTo) {
			map = CollectionCreators.Bootstrap.createConcurrentClassMap();
			map.put(to, Boolean.TRUE);
			Class superclass = to.getSuperclass();
			if (superclass != null) {
				map.putAll(computeToMap(superclass));
			}
			for (Class implemented : getClassReflector(to).getInterfaces()) {
				map.putAll(computeToMap(implemented));
			}
			assignableTo.put(to, map);
			return map;
		}
	}
}

package cc.alcina.framework.common.client.reflection;

import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class ClientReflections {
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
		return perClassReflectorSuppliers.get(clazz).get();
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
			if (to.getSuperclass() != null) {
				map.putAll(computeToMap(to.getSuperclass()));
			}
			for (Class implemented : getClassReflector(to).getInterfaces()) {
				map.putAll(computeToMap(implemented));
			}
			assignableTo.put(to, map);
			return map;
		}
	}
}

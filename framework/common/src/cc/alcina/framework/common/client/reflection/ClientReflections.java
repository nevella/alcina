package cc.alcina.framework.common.client.reflection;

import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class ClientReflections {
	static Map<String, Supplier<ClassReflector>> perClassReflectorSuppliers = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	static Map<String, Supplier<Class>> forNames = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	static Map<Class, Map<Class, Boolean>> assignableTo = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public static Class<?> forName(String fqn) {
		return forNames.get(fqn).get();
	}

	public static ClassReflector<?> getClassReflector(Class clazz) {
		return perClassReflectorSuppliers.get(clazz.getName()).get();
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
		return assignableTo.computeIfAbsent(to, toClazz -> {
			Map<Class, Boolean> map = CollectionCreators.Bootstrap
					.createConcurrentClassMap();
			map.put(toClazz, Boolean.TRUE);
			if (to.getSuperclass() != null) {
				map.putAll(computeToMap(to.getSuperclass()));
			}
			for (Class implemented : getClassReflector(toClazz)
					.getInterfaces()) {
				map.putAll(computeToMap(implemented));
			}
			return map;
		});
	}
}
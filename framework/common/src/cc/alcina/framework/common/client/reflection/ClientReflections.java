package cc.alcina.framework.common.client.reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators;

public class ClientReflections {
	public static final String DEV_MODE_REFLECTOR = ClientReflections.class
			.getName() + ".DEV_MODE_REFLECTOR";

	static List<ModuleReflector> moduleReflectors = new ArrayList<>();

	static Map<Class, Map<Class, Boolean>> assignableTo = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public static Map<Class, Boolean> emptyReflectorClasses = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

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

	public static Class<?> forName(String fqn) {
		Optional<Class> optional = moduleReflectors.stream()
				.map(mr -> mr.forName(fqn)).filter(Objects::nonNull)
				.findFirst();
		if (optional.isEmpty()) {
			throw new NoSuchElementException("No forName for " + fqn);
		}
		return optional.get();
	}

	public static ClassReflector<?> getClassReflector(Class clazz) {
		Optional<ClassReflector> optional = moduleReflectors.stream()
				.map(mr -> mr.getClassReflector(clazz)).filter(Objects::nonNull)
				.findFirst();
		if (optional.isEmpty()) {
			if (ClassUtil.isEnumSubclass(clazz)) {
				return getClassReflector(clazz.getSuperclass());
			}
			if (clazz.getName().startsWith("java.") || clazz.isPrimitive()
					|| (clazz.isArray()
							&& clazz.getComponentType().isPrimitive())
					|| emptyReflectorClasses.containsKey(clazz)) {
				// non-public internal class, either GWT or JDK, e.g.
				// Arrays$ArrayList - or primitive
				// add a few hardcoded internal jdk classes to help
				// serialization
				List<Class> interfaces = List.of();
				switch (clazz.getName()) {
				case "java.util.ImmutableCollections$AbstractImmutableList":
				case "java.util.Collections$UnmodifiableList":
				case "java.util.Collections$SingletonList":
				case "java.util.Collections$EmptyList":
					interfaces = List.of(List.class);
					break;
				case "java.util.ImmutableCollections$AbstractImmutableMap":
					interfaces = List.of(Map.class);
					break;
				case "java.util.ImmutableCollections$AbstractImmutableSet":
					interfaces = List.of(Set.class);
					break;
				}
				return ClassReflector.emptyReflector(clazz, interfaces);
			}
			throw new NoSuchElementException(Ax.format(
					"No reflector for %s - check it or a superclass has the @Bean or @Reflected annotation",
					clazz.getName()));
		}
		return optional.get();
	}

	public static boolean isAssignableFrom(Class from, Class to) {
		return computeToMap(to).containsKey(from);
	}

	public static void register(ModuleReflector reflector) {
		moduleReflectors.add(reflector);
		reflector.registerRegistrations();
	}
}

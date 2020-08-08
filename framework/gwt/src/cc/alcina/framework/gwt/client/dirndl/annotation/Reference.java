package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

/*
 * Implementations (essentially extensible enums) require an @Ref annotation if serializable
 */
@Bean
public abstract class Reference {
	public static <A extends Reference> Class<? extends A> forId(Class<A> clazz,
			String id) {
		return Resolver.get().forId(clazz, id);
	}

	@RegistryLocation(registryPoint = Resolver.class, implementationType = ImplementationType.SINGLETON)
	public static class Resolver {
		public static Reference.Resolver get() {
			Reference.Resolver singleton = Registry
					.checkSingleton(Reference.Resolver.class);
			if (singleton == null) {
				singleton = new Reference.Resolver();
				Registry.registerSingleton(Reference.Resolver.class, singleton);
			}
			return singleton;
		}

		Map<Class, Map<String, Class<? extends Reference>>> cache = new LinkedHashMap<>();

		public <A extends Reference> Class<? extends A> forId(Class<A> clazz,
				String id) {
			return (Class<? extends A>) cache.computeIfAbsent(clazz, c -> {
				Map<String, Class<? extends Reference>> byId = new LinkedHashMap<>();
				List<Class> classes = Registry.get().lookup(clazz);
				for (Class refClass : classes) {
					String refId = Reflections.classLookup()
							.getAnnotationForClass(refClass, Ref.class).value();
					if (byId.put(refId, refClass) != null) {
						throw Ax.runtimeException(
								"Key collision:: class %s :: key %s",
								clazz.getSimpleName(), refId);
					}
				}
				return byId;
			}).get(id);
		}
	}

	public static String id(Class<? extends Reference> refClass) {
		return Reflections.classLookup()
				.getAnnotationForClass(refClass, Ref.class).value();
	}
}

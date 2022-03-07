package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;

/*
 * Implementations (essentially extensible enums) require an @Ref annotation if serializable
 *
 * FIXME - dirndl 1.3 - revisit
 */
@Bean
public abstract class Reference {
	public static <A extends Reference> Class<? extends A> forId(Class<A> clazz,
			String id) {
		return Resolver.get().forId(clazz, id);
	}

	public static String id(Class<? extends Reference> refClass) {
		return Reflections.at(refClass).annotation(Ref.class).value();
	}

	@Registration.Singleton
	public static class Resolver {
		public static Reference.Resolver get() {
			return Registry.impl(Reference.Resolver.class);
		}

		Map<Class, Map<String, Class<? extends Reference>>> cache = new LinkedHashMap<>();

		public <A extends Reference> Class<? extends A> forId(Class<A> clazz,
				String id) {
			return (Class<? extends A>) cache.computeIfAbsent(clazz, c -> {
				Map<String, Class<? extends Reference>> byId = new LinkedHashMap<>();
				Registry.query(clazz).registrations().forEach(refClass -> {
					String refId = Reflections.at(refClass)
							.annotation(Ref.class).value();
					Class existing = byId.put(refId, refClass);
					if (existing != null) {
						throw Ax.runtimeException(
								"Key collision:: key %s\n\texisting: %s\n\t incoming: %s ",
								refId, existing.getCanonicalName(),
								refClass.getCanonicalName());
					}
				});
				return byId;
			}).get(id);
		}
	}
}

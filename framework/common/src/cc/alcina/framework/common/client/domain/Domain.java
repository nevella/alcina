package cc.alcina.framework.common.client.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class Domain {
	private static DomainHandler handler = new DomainHandlerNonTransactional();

	public static final List<String> DOMAIN_BASE_VERSIONABLE_PROPERTY_NAMES = Arrays
			.asList(new String[] { "id", "localId", "lastModificationDate",
					"creationDate", "versionNumber" });

	static Logger logger = LoggerFactory.getLogger(Domain.class);

	// FIXME - mvcc.sky - deprecate (ditto asSet)
	public static <V extends Entity> List<V> asList(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toList());
	}

	public static <V extends Entity> Set<V> asSet(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toSet());
	}

	public static <T extends Entity> void async(Class<T> clazz, long objectId,
			boolean create, Consumer<T> resultConsumer) {
		handler.async(clazz, objectId, create, resultConsumer);
	}

	public static <V extends Entity> V by(Class<V> clazz, String propertyName,
			Object value) {
		return handler.byProperty(clazz, propertyName, value);
	}

	public static <V extends Entity> V create(Class<V> clazz) {
		return handler.create(clazz);
	}

	public static <V extends Entity> void delete(V v) {
		TransformManager.get().delete(v);
	}

	public static <V extends Entity> V detachedToDomain(V entity) {
		return detachedToDomain(entity, null);
	}

	public static <V extends Entity> V detachedToDomain(V entity,
			List<String> ignoreProperties) {
		if (Domain.isDomainVersion(entity)) {
			return entity;
		}
		Class<V> clazz = entity.entityClass();
		V writeable = entity.domain().wasPersisted()
				? (V) detachedVersion(entity.domain().domainVersion())
				: Domain.create(clazz);
		Reflections.at(clazz).properties().stream().filter(p -> !p.isReadOnly())
				.forEach(property -> {
					if (TransformManager.get()
							.isIgnoreProperty(property.getName())) {
						return;
					}
					if (ignoreProperties != null
							&& ignoreProperties.contains(property.getName())) {
						return;
					}
					if (property.has(AlcinaTransient.class)) {
						return;
					}
					logger.info(
							"detachedToDomain:: {}.{} - ignoreProperties: {} :: [{}]->[{}]",
							entity.getClass().getSimpleName(),
							property.getName(), ignoreProperties,
							property.get(entity), property.get(writeable));
					property.copy(entity, writeable);
				});
		return writeable;
	}

	public static <V extends Entity> V detachedVersion(Class<V> clazz,
			long id) {
		V v = find(clazz, id);
		return detachedVersion(v);
	}

	public static <V extends Entity> V detachedVersion(V v) {
		return v == null ? null : handler.detachedVersion(v);
	}

	public static <V extends Entity> V ensure(Class<V> clazz,
			String propertyName, Object value) {
		V first = by(clazz, propertyName, value);
		if (first == null) {
			first = create(clazz);
			Reflections.at(clazz).property(propertyName).set(first, value);
		}
		return first;
	}

	public static <V extends Entity> V find(Class<V> clazz, long id) {
		return handler.find(clazz, id);
	}

	public static <V extends Entity> V find(EntityLocator locator) {
		return handler.find(locator);
	}

	/*
	 * Only access via entity.domain().domainVersion/ensurePopulated();
	 */
	public static <V extends Entity> V find(V v) {
		return handler.find(v);
	}

	public static <V extends Entity> boolean isDomainVersion(V v) {
		return v == null ? false : handler.isDomainVersion(v);
	}

	public static <V extends Entity> List<V> listByProperty(Class<V> clazz,
			String propertyName, Object value) {
		return handler.listByProperty(clazz, propertyName, value);
	}

	public static <V extends Entity> Optional<V> optionalByProperty(
			Class<V> clazz, String propertyName, Object value) {
		return Optional.ofNullable(by(clazz, propertyName, value));
	}

	public static <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
		return handler.query(clazz);
	}

	public static <V extends Entity> V register(V v) {
		return TransformManager.get().registerDomainObject(v);
	}

	public static void registerHandler(DomainHandler singleton) {
		Domain.handler = singleton;
	}

	public static <V extends Entity> V resolve(V v) {
		return handler.resolve(v);
	}

	public static Class<? extends Object>
			resolveEntityClass(Class<? extends Object> clazz) {
		if (clazz == null) {
			return null;
		}
		return handler.resolveEntityClass(clazz);
	}

	public static <V extends Entity> long size(Class<V> clazz) {
		return handler.size(clazz);
	}

	public static <V extends Entity> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public static boolean wasRemoved(Entity entity) {
		return handler.wasRemoved(entity);
	}

	public interface DomainHandler {
		public <V extends Entity> void async(Class<V> clazz, long objectId,
				boolean create, Consumer<V> resultConsumer);

		public <V extends Entity> V find(Class clazz, long id);

		public <V extends Entity> V find(V v);

		public <V extends Entity> Stream<V> stream(Class<V> clazz);

		default <V extends Entity> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			Property property = Reflections.at(clazz).property(propertyName);
			return stream(clazz)
					.filter(e -> Objects.equals(property.get(e), value))
					.findFirst().orElse(null);
		}

		default <V extends Entity> V create(Class<V> clazz) {
			return TransformManager.get().createDomainObject(clazz);
		}

		<V extends Entity> V detachedVersion(V v);

		default <V extends Entity> V find(EntityLocator locator) {
			return find(locator.clazz, locator.id);
		}

		<V extends Entity> boolean isDomainVersion(V v);

		default <V extends Entity> List<V> listByProperty(Class<V> clazz,
				String propertyName, Object value) {
			Property property = Reflections.at(clazz).property(propertyName);
			return stream(clazz)
					.filter(e -> Objects.equals(property.get(e), value))
					.collect(Collectors.toList());
		}

		<V extends Entity> DomainQuery<V> query(Class<V> clazz);

		default <V extends Entity> V resolve(V v) {
			return v;
		}

		default Class<? extends Object>
				resolveEntityClass(Class<? extends Object> clazz) {
			return clazz;
		}

		default <V extends Entity> long size(Class<V> clazz) {
			return stream(clazz).count();
		}

		default boolean wasRemoved(Entity entity) {
			return false;
		}
	}

	public static class DomainHandlerNonTransactional implements DomainHandler {
		@Override
		public <V extends Entity> void async(Class<V> clazz, long objectId,
				boolean create, Consumer<V> resultConsumer) {
		}

		@Override
		public <V extends Entity> V detachedVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V find(Class clazz, long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V find(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> boolean isDomainVersion(V v) {
			return v.getId() != 0 || v.getLocalId() != 0;
		}

		@Override
		public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}
	}
}

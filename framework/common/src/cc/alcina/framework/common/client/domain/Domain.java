package cc.alcina.framework.common.client.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfo;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class Domain {
	private static DomainHandler handler = new DomainHandlerNonTransactional();

	public static final List<String> DOMAIN_BASE_VERSIONABLE_PROPERTY_NAMES = Arrays
			.asList(new String[] { "id", "localId", "lastModificationDate",
					"lastModificationUser", "creationDate", "creationUser",
					"versionNumber" });

	static Logger logger = LoggerFactory.getLogger(Domain.class);

	// FIXME - mvcc.3 - deprecate (ditto asSet)
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

	public static <V extends Entity> V byProperty(Class<V> clazz,
			String propertyName, Object value) {
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
				? detachedVersion(Domain.find(entity))
				: Domain.create(clazz);
		List<PropertyInfo> writableProperties = Reflections.classLookup()
				.getWritableProperties(clazz);
		for (PropertyInfo propertyInfo : writableProperties) {
			String propertyName = propertyInfo.getPropertyName();
			if (TransformManager.get().isIgnoreProperty(propertyName)) {
				continue;
			}
			if (ignoreProperties != null
					&& ignoreProperties.contains(propertyName)) {
				continue;
			}
			AlcinaTransient alcinaTransient = Reflections.propertyAccessor()
					.getAnnotationForProperty(entity.getClass(),
							AlcinaTransient.class, propertyName);
			if (alcinaTransient != null) {
				continue;
			}
			logger.info(
					"detachedToDomain:: %s.%s - ignoreProperties: %s :: [%s]->[%s]",
					entity.getClass().getSimpleName(),
					propertyInfo.getPropertyName(), ignoreProperties,
					propertyInfo.get(entity), propertyInfo.get(writeable));
			propertyInfo.copy(entity, writeable);
		}
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

	public static <V extends Entity> V find(Class clazz, long id) {
		return handler.find(clazz, id);
	}

	public static <V extends Entity> V find(EntityLocator locator) {
		return handler.find(locator.clazz, locator.id);
	}

	public static <V extends Entity> V find(V v) {
		return handler.find(v);
	}

	public static <V extends Entity> V findOrCreate(Class<V> clazz,
			String propertyName, Object value, boolean createIfNonexistent) {
		V first = byProperty(clazz, propertyName, value);
		if (first == null && createIfNonexistent) {
			first = create(clazz);
			Reflections.propertyAccessor().setPropertyValue(first, propertyName,
					value);
		} else {
		}
		return first;
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
		return Optional.ofNullable(byProperty(clazz, propertyName, value));
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
		return handler.resolveEntityClass(clazz);
	}

	public static <V extends Entity> long size(Class<V> clazz) {
		return handler.size(clazz);
	}

	public static <V extends Entity> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public interface DomainHandler {
		public <V extends Entity> void async(Class<V> clazz, long objectId,
				boolean create, Consumer<V> resultConsumer);

		public <V extends Entity> V find(Class clazz, long id);

		public <V extends Entity> V find(V v);

		public <V extends Entity> Stream<V> stream(Class<V> clazz);

		default <V extends Entity> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			return CommonUtils
					.first(listByProperty(clazz, propertyName, value));
		}

		default <V extends Entity> V create(Class<V> clazz) {
			return TransformManager.get().createDomainObject(clazz);
		}

		<V extends Entity> V detachedVersion(V v);

		<V extends Entity> boolean isDomainVersion(V v);

		default <V extends Entity> List<V> listByProperty(Class<V> clazz,
				String propertyName, Object value) {
			return query(clazz).filter(propertyName, value).list();
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
			throw new UnsupportedOperationException();
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

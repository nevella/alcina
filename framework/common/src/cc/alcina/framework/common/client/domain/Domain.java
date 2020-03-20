package cc.alcina.framework.common.client.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
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

	public static <V extends Entity> List<V> asList(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toList());
	}

	public static <V extends Entity> Set<V> asSet(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toSet());
	}

	public static <T extends Entity> void async(Class<T> clazz,
			long objectId, boolean create, Consumer<T> resultConsumer) {
		handler.async(clazz, objectId, create, resultConsumer);
	}

	public static <V extends Entity> V byProperty(Class<V> clazz,
			String propertyName, Object value) {
		return handler.byProperty(clazz, propertyName, value);
	}

	public static <V extends Entity> Supplier<Collection>
			castSupplier(Class<V> clazz) {
		return () -> values(clazz);
	}

	public static void commitPoint() {
		handler.commitPoint();
	}

	public static <V extends Entity> V create(Class<V> clazz) {
		return handler.create(clazz);
	}

	public static <V extends Entity> void delete(Class<V> clazz,
			long id) {
		Entity entity = find(clazz, id);
		if (entity != null) {
			writeable(entity).delete();
		}
	}

	public static <V extends Entity> void delete(V v) {
		TransformManager.get().deleteObject(v, true);
	}

	public static <V extends Entity> V detachedToDomain(V entity) {
		return detachedToDomain(entity, null);
	}

	public static <V extends Entity> V detachedToDomain(V entity,
			List<String> ignoreProperties) {
		Class<V> clazz = (Class<V>) entity.getClass();
		V writeable = entity.provideWasPersisted()
				? Domain.writeable(Domain.find(entity))
				: Domain.create(clazz);
		List<PropertyInfoLite> writableProperties = Reflections.classLookup()
				.getWritableProperties(clazz);
		for (PropertyInfoLite propertyInfo : writableProperties) {
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
			first = writeable(first);
		}
		return first;
	}

	public static <V extends Entity> List<Long> ids(Class<V> clazz) {
		return handler.ids(clazz);
	}

	public static <V extends Entity> boolean isDomainVersion(V v) {
		return v == null ? false : handler.isDomainVersion(v);
	}

	public static <V extends Entity> List<V>
			listByProperty(Class<V> clazz, String propertyName, Object value) {
		return handler.listByProperty(clazz, propertyName, value);
	}

	public static <V extends Entity> Optional<V> optionalByProperty(
			Class<V> clazz, String propertyName, Object value) {
		return Optional.ofNullable(byProperty(clazz, propertyName, value));
	}

	public static <V extends Entity> DomainQuery<V>
			query(Class<V> clazz) {
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

	public static <V extends Entity> V resolveTransactional(
			DomainListener listener, V value, Object[] path) {
		return handler.resolveTransactional(listener, value, path);
	}

	public static <V extends Entity> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public static <V extends Entity> V transactionalFind(Class clazz,
			long id) {
		return handler.transactionalFind(clazz, id);
	}

	public static <V extends Entity> V transactionVersion(V v) {
		return handler.transactionalVersion(v);
	}

	public static <V extends Entity> Collection<V>
			values(Class<V> clazz) {
		return handler.values(clazz);
	}

	/**
	 * if DomainStore, project - if detached (no tm listeners), find and project
	 */
	public static <V extends Entity> V writeable(V v) {
		return handler.writeable(v);
	}

	public interface DomainHandler {
		public <V extends Entity> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer);

		public void commitPoint();

		public <V extends Entity> V find(Class clazz, long id);

		public <V extends Entity> V find(V v);

		public <V extends Entity> V resolveTransactional(
				DomainListener listener, V value, Object[] path);

		public <V extends Entity> Stream<V> stream(Class<V> clazz);

		public <V extends Entity> V transactionalFind(Class clazz,
				long id);

		public <V extends Entity> Collection<V> values(Class<V> clazz);

		public <V extends Entity> V writeable(V v);

		default <V extends Entity> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			return CommonUtils
					.first(listByProperty(clazz, propertyName, value));
		}

		default <V extends Entity> V create(Class<V> clazz) {
			return TransformManager.get().createDomainObject(clazz);
		}

		<V extends Entity> V detachedVersion(V v);

		<V extends Entity> List<Long> ids(Class<V> clazz);

		<V extends Entity> boolean isDomainVersion(V v);

		default <V extends Entity> List<V> listByProperty(
				Class<V> clazz, String propertyName, Object value) {
			return query(clazz).filter(propertyName, value).list();
		}

		<V extends Entity> DomainQuery<V> query(Class<V> clazz);

		default <V extends Entity> V resolve(V v) {
			return v;
		}

		<V extends Entity> V transactionalVersion(V v);
	}

	public static class DomainHandlerNonTransactional implements DomainHandler {
		@Override
		public <V extends Entity> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer) {
		}

		@Override
		public void commitPoint() {
			throw new UnsupportedOperationException();
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends Entity> List<Long> ids(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> boolean isDomainVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> DomainQuery<V>
				query(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V resolveTransactional(
				DomainListener listener, V value, Object[] path) {
			return value;
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends Entity> V transactionalFind(Class clazz,
				long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V transactionalVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> Collection<V>
				values(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends Entity> V writeable(V v) {
			throw new UnsupportedOperationException();
		}
	}
}

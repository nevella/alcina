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
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
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

	public static <V extends HasIdAndLocalId> List<V> asList(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toList());
	}

	public static <V extends HasIdAndLocalId> Set<V> asSet(Class<V> clazz) {
		return handler.stream(clazz).collect(Collectors.toSet());
	}

	public static <T extends HasIdAndLocalId> void async(Class<T> clazz,
			long objectId, boolean create, Consumer<T> resultConsumer) {
		handler.async(clazz, objectId, create, resultConsumer);
	}

	public static <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
			String propertyName, Object value) {
		return handler.byProperty(clazz, propertyName, value);
	}

	public static <V extends HasIdAndLocalId> Supplier<Collection>
			castSupplier(Class<V> clazz) {
		return () -> values(clazz);
	}

	public static void commitPoint() {
		handler.commitPoint();
	}

	public static <V extends HasIdAndLocalId> V create(Class<V> clazz) {
		return handler.create(clazz);
	}

	public static <V extends HasIdAndLocalId> void delete(Class<V> clazz,
			long id) {
		HasIdAndLocalId hili = find(clazz, id);
		if (hili != null) {
			writeable(hili).delete();
		}
	}

	public static <V extends HasIdAndLocalId> void delete(V v) {
		TransformManager.get().deleteObject(v, true);
	}

	public static <V extends HasIdAndLocalId> V detachedToDomain(V hili) {
		return detachedToDomain(hili, null);
	}

	public static <V extends HasIdAndLocalId> V detachedToDomain(V hili,
			List<String> ignoreProperties) {
		Class<V> clazz = (Class<V>) hili.getClass();
		V writeable = hili.provideWasPersisted()
				? Domain.writeable(Domain.find(hili))
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
					.getAnnotationForProperty(hili.getClass(),
							AlcinaTransient.class, propertyName);
			if (alcinaTransient != null) {
				continue;
			}
			propertyInfo.copy(hili, writeable);
		}
		return writeable;
	}

	public static <V extends HasIdAndLocalId> V detachedVersion(Class<V> clazz,
			long id) {
		V v = find(clazz, id);
		return detachedVersion(v);
	}

	public static <V extends HasIdAndLocalId> V detachedVersion(V v) {
		return v == null ? null : handler.detachedVersion(v);
	}

	public static <V extends HasIdAndLocalId> V find(Class clazz, long id) {
		return handler.find(clazz, id);
	}

	public static <V extends HasIdAndLocalId> V find(HiliLocator locator) {
		return handler.find(locator.clazz, locator.id);
	}

	public static <V extends HasIdAndLocalId> V find(V v) {
		return handler.find(v);
	}

	public static <V extends HasIdAndLocalId> V findOrCreate(Class<V> clazz,
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

	public static <V extends HasIdAndLocalId> List<Long> ids(Class<V> clazz) {
		return handler.ids(clazz);
	}

	public static <V extends HasIdAndLocalId> boolean isDomainVersion(V v) {
		return v == null ? false : handler.isDomainVersion(v);
	}

	public static <V extends HasIdAndLocalId> List<V>
			listByProperty(Class<V> clazz, String propertyName, Object value) {
		return handler.listByProperty(clazz, propertyName, value);
	}

	public static <V extends HasIdAndLocalId> Optional<V> optionalByProperty(
			Class<V> clazz, String propertyName, Object value) {
		return Optional.ofNullable(byProperty(clazz, propertyName, value));
	}

	public static <V extends HasIdAndLocalId> DomainQuery<V>
			query(Class<V> clazz) {
		return handler.query(clazz);
	}

	public static <V extends HasIdAndLocalId> V register(V v) {
		return TransformManager.get().registerDomainObject(v);
	}

	public static void registerHandler(DomainHandler singleton) {
		Domain.handler = singleton;
	}

	public static <V extends HasIdAndLocalId> V resolve(V v) {
		return handler.resolve(v);
	}

	public static <V extends HasIdAndLocalId> V resolveTransactional(
			DomainListener listener, V value, Object[] path) {
		return handler.resolveTransactional(listener, value, path);
	}

	public static <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public static <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
			long id) {
		return handler.transactionalFind(clazz, id);
	}

	public static <V extends HasIdAndLocalId> V transactionVersion(V v) {
		return handler.transactionalVersion(v);
	}

	public static <V extends HasIdAndLocalId> Collection<V>
			values(Class<V> clazz) {
		return handler.values(clazz);
	}

	/**
	 * if DomainStore, project - if detached (no tm listeners), find and project
	 */
	public static <V extends HasIdAndLocalId> V writeable(V v) {
		return handler.writeable(v);
	}

	public interface DomainHandler {
		public <V extends HasIdAndLocalId> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer);

		public void commitPoint();

		public <V extends HasIdAndLocalId> V find(Class clazz, long id);

		public <V extends HasIdAndLocalId> V find(V v);

		public <V extends HasIdAndLocalId> V resolveTransactional(
				DomainListener listener, V value, Object[] path);

		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz);

		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id);

		public <V extends HasIdAndLocalId> Collection<V> values(Class<V> clazz);

		public <V extends HasIdAndLocalId> V writeable(V v);

		default <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			return CommonUtils
					.first(listByProperty(clazz, propertyName, value));
		}

		default <V extends HasIdAndLocalId> V create(Class<V> clazz) {
			return TransformManager.get().createDomainObject(clazz);
		}

		<V extends HasIdAndLocalId> V detachedVersion(V v);

		<V extends HasIdAndLocalId> List<Long> ids(Class<V> clazz);

		<V extends HasIdAndLocalId> boolean isDomainVersion(V v);

		default <V extends HasIdAndLocalId> List<V> listByProperty(
				Class<V> clazz, String propertyName, Object value) {
			return query(clazz).filter(propertyName, value).list();
		}

		<V extends HasIdAndLocalId> DomainQuery<V> query(Class<V> clazz);

		default <V extends HasIdAndLocalId> V resolve(V v) {
			return v;
		}

		<V extends HasIdAndLocalId> V transactionalVersion(V v);
	}

	public static class DomainHandlerNonTransactional implements DomainHandler {
		@Override
		public <V extends HasIdAndLocalId> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer) {
		}

		@Override
		public void commitPoint() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V detachedVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V find(Class clazz, long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V find(V v) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> List<Long> ids(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> boolean isDomainVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> DomainQuery<V>
				query(Class<V> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V resolveTransactional(
				DomainListener listener, V value, Object[] path) {
			return value;
		}

		@Override
		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> Collection<V>
				values(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> V writeable(V v) {
			throw new UnsupportedOperationException();
		}
	}
}

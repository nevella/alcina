package cc.alcina.framework.common.client.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliComparator;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class Domain {
	private static DomainHandler handler = new DomainHandlerNonTransactional();

	public static <T extends HasIdAndLocalId> void async(Class<T> clazz,
			long objectId, boolean create, Consumer<T> resultConsumer) {
		handler.async(clazz, objectId, create, resultConsumer);
	}

	public static <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
			String propertyName, Object value) {
		return handler.byProperty(clazz, propertyName, value);
	}

	public static <V extends HasIdAndLocalId> List<V>
			listByProperty(Class<V> clazz, String propertyName, Object value) {
		return handler.listByProperty(clazz, propertyName, value);
	}

	public static <V extends HasIdAndLocalId> Supplier<Collection>
			castSupplier(Class<V> clazz) {
		return () -> list(clazz);
	}

	public static <V extends HasIdAndLocalId> V create(Class<V> clazz) {
		return TransformManager.get().createDomainObject(clazz);
	}

	public static <V extends HasIdAndLocalId> void delete(V v) {
		TransformManager.get().deleteObject(v, true);
	}

	public static <V extends HasIdAndLocalId> V find(Class clazz, long id) {
		return handler.find(clazz, id);
	}

	public static <V extends HasIdAndLocalId> V find(V v) {
		return handler.find(v);
	}

	public static <V extends HasIdAndLocalId> V find(HiliLocator locator) {
		return handler.find(locator.clazz, locator.id);
	}

	public static <V extends HasIdAndLocalId> Collection<V>
			list(Class<V> clazz) {
		return handler.list(clazz);
	}

	public static <V extends HasIdAndLocalId> Optional<V> optionalByProperty(
			Class<V> clazz, String propertyName, Object value) {
		return Optional.ofNullable(byProperty(clazz, propertyName, value));
	}

	public static void registerHandler(DomainHandler singleton) {
		Domain.handler = singleton;
	}

	public static <V extends HasIdAndLocalId> V resolveTransactional(
			CacheListener listener, V value, Object[] path) {
		return handler.resolveTransactional(listener, value, path);
	}

	public static <V extends HasIdAndLocalId> Set<V> set(Class<V> clazz) {
		return new LinkedHashSet<>(handler.list(clazz));
	}

	public static <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public static <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
			long id) {
		return handler.transactionalFind(clazz, id);
	}

	/**
	 * if memcache, project - if detached (no tm listeners), find and project
	 */
	public static <V extends HasIdAndLocalId> V writeable(V v) {
		return handler.writeable(v);
	}

	public static final List<String> domainBaseVersionablePropertyNames = Arrays
			.asList(new String[] { "id", "localId", "lastModificationDate",
					"lastModificationUser", "creationDate", "creationUser",
					"versionNumber" });

	public interface DomainHandler {
		public <V extends HasIdAndLocalId> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer);

		default <V extends HasIdAndLocalId> List<V> listByProperty(
				Class<V> clazz, String propertyName, Object value) {
			IndividualPropertyAccessor accessor = Reflections.propertyAccessor()
					.cachedAccessor(clazz, propertyName);
			return stream(clazz).filter(
					o -> Objects.equals(accessor.getPropertyValue(o), value))
					.collect(Collectors.toList());
		}

		default <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			IndividualPropertyAccessor accessor = Reflections.propertyAccessor()
					.cachedAccessor(clazz, propertyName);
			return stream(clazz).filter(
					o -> Objects.equals(accessor.getPropertyValue(o), value))
					.findFirst().orElse(null);
		}

		public <V extends HasIdAndLocalId> V find(Class clazz, long id);

		public <V extends HasIdAndLocalId> V find(V v);

		public <V extends HasIdAndLocalId> Collection<V> list(Class<V> clazz);

		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path);

		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz);

		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id);

		public <V extends HasIdAndLocalId> V writeable(V v);

		public void commitPoint();
	}

	public static class DomainHandlerNonTransactional implements DomainHandler {
		@Override
		public <V extends HasIdAndLocalId> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer) {
			// TODO Auto-generated method stub
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
		public <V extends HasIdAndLocalId> Collection<V> list(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path) {
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
		public <V extends HasIdAndLocalId> V writeable(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void commitPoint() {
			throw new UnsupportedOperationException();
		}
	}

	public static <V extends HasIdAndLocalId> V detachedToDomain(V hili) {
		return detachedToDomain(hili, null);
	}

	public static <V extends HasIdAndLocalId> V detachedToDomain(V hili,
			List<String> ignoreProperties) {
		if (hili.getId() == 0 && hili.getLocalId() != 0) {
			//in this case, we're using temporary objects (generally in cascade) - just listen
			if (hili instanceof SourcesPropertyChangeEvents) {
				TransformManager.get()
						.listenTo((SourcesPropertyChangeEvents) hili);
			}
			return hili;
		}
		Class<V> clazz = (Class<V>) hili.getClass();
		V writeable = hili.provideWasPersisted()
				? Domain.writeable(Domain.find(hili)) : Domain.create(clazz);
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

	public static void commitPoint() {
		handler.commitPoint();
	}

	public static <V extends HasIdAndLocalId> V register(V v) {
		return TransformManager.get().registerDomainObject(v);
	}

	public static <V extends HasIdAndLocalId> Collection<V>
			reverseIdList(Class<V> clazz) {
		return handler.stream(clazz).sorted(HiliComparator.REVERSED_INSTANCE)
				.collect(Collectors.toList());
	}

	public static <V extends HasIdAndLocalId> void delete(Class<V> clazz,
			long id) {
		HasIdAndLocalId hili = find(clazz, id);
		if (hili != null) {
			writeable(hili).delete();
		}
	}
}

package cc.alcina.framework.common.client.cache;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class Domain {
	public interface DomainHandler {
		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path);

		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id);

		public <V extends HasIdAndLocalId> V find(Class clazz, long id);

		public <V extends HasIdAndLocalId> Collection<V> list(Class<V> clazz);

		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz);

		public <V extends HasIdAndLocalId> V writeable(V v);
	}

	private static DomainHandler handler = new DomainHandlerNonTransactional();

	public static void registerHandler(DomainHandler singleton) {
		Domain.handler = singleton;
	}

	public static <V extends HasIdAndLocalId> V resolveTransactional(
			CacheListener listener, V value, Object[] path) {
		return handler.resolveTransactional(listener, value, path);
	}

	public static <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
			long id) {
		return handler.transactionalFind(clazz, id);
	}

	public static <V extends HasIdAndLocalId> V find(Class clazz, long id) {
		return handler.find(clazz, id);
	}

	public static class DomainHandlerNonTransactional implements DomainHandler {
		@Override
		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path) {
			return value;
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V find(Class clazz, long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> Collection<V> list(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <V extends HasIdAndLocalId> V writeable(V v) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
		return handler.stream(clazz);
	}

	public static <V extends HasIdAndLocalId> Collection<V>
			list(Class<V> clazz) {
		return handler.list(clazz);
	}

	public static <V extends HasIdAndLocalId> V writeable(V v) {
		return handler.writeable(v);
	}

	public static <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
			String propertyName, Object value) {
		throw new UnsupportedOperationException();
	}

	public static <V extends HasIdAndLocalId> Optional<V> optionalByProperty(
			Class<V> clazz, String propertyName, Object value) {
		return Optional.ofNullable(byProperty(clazz, propertyName, value));
	}
}

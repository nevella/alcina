package cc.alcina.framework.common.client.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class Domain {
	public interface DomainHandler {
		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path);

		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id);
	}

	private static DomainHandler handler;

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
}

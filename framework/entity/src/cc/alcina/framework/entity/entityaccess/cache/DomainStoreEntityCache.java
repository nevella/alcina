package cc.alcina.framework.entity.entityaccess.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;

class DomainStoreEntityCache extends DetachedEntityCache
		implements DomainStoreCache {
	@Override
	public void invalidate(Class clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	// linkedhashmap is fine here
	protected Map<Class, Map<Long, Entity>> createClientInstanceClassMap() {
		return super.createClientInstanceClassMap();
	}

	@Override
	protected Map<Long, Entity> createIdEntityMap(Class clazz) {
		TransactionalMap transactionalMap = new TransactionalMap(Long.class,
				clazz);
		transactionalMap.setImmutableValues(true);
		return transactionalMap;
	}

	@Override
	protected void createTopMaps() {
		domain = new LinkedHashMap<>();
		local = new LinkedHashMap<>();
		createdLocals = new ConcurrentHashMap<>();
	}

	@Override
	protected <T> T getLocal(Class<T> clazz, long localId) {
		return super.getLocal(clazz, localId);
	}
}
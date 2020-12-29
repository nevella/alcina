package cc.alcina.framework.entity.persistence.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalMap;

class DomainStoreEntityCache extends DetachedEntityCache {
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
		return transactionalMap;
	}

	@Override
	protected void createTopMaps() {
		domain = new LinkedHashMap<>();
		local = new LinkedHashMap<>();
		createdLocals = new ConcurrentHashMap<>();
	}

	@Override
	protected void ensureMap(Class clazz) {
		// noop - prevent possibly concurrent writes by forcing map creation at
		// domainstore init time
	}

	@Override
	protected <T> T getLocal(Class<T> clazz, long localId) {
		return super.getLocal(clazz, localId);
	}

	void initialiseMap(Class clazz) {
		super.ensureMap(clazz);
	}

	void putExternalLocal(Entity instance) {
		super.put0(instance, true);
	}
}
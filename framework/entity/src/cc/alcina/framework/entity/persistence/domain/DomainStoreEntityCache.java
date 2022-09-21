package cc.alcina.framework.entity.persistence.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalMap;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalMap.EntityIdMap;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

class DomainStoreEntityCache extends DetachedEntityCache {
	int negativeIdPutWarningCount = 0;

	@Override
	public void debugNotFound(EntityLocator objectLocator) {
		TransactionalMap txMap = (TransactionalMap) domain
				.get(objectLocator.getClazz());
		txMap.debugNotFound(objectLocator.getId());
	}

	public void ensureVersion(Entity entity) {
		getDomainMap(entity.entityClass()).ensureVersion(entity.getId());
	}

	@Override
	/*
	 * Don't use outside of transitional, backend bulk jobs
	 */
	public void invalidate(Class clazz) {
		super.invalidate(clazz);
	}

	@Override
	protected void checkNegativeIdPut(Entity entity) {
		if (Transaction.current().isToDomainCommitting()) {
			if (negativeIdPutWarningCount++ < 100) {
				LoggerFactory.getLogger(getClass()).warn(
						"indexing entity with negative id :: {}",
						entity.toLocator());
			}
		}
	}

	@Override
	// linkedhashmap is fine here
	protected Map<Class, Map<Long, Entity>> createClientInstanceClassMap() {
		return super.createClientInstanceClassMap();
	}

	@Override
	protected Map<Long, Entity> createIdEntityMap(Class clazz) {
		return new TransactionalMap.EntityIdMap(Long.class, clazz);
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

	@Override
	protected boolean isExternalCreate() {
		return ThreadlocalTransformManager.cast().isExternalCreate();
	}

	Entity getAnyTransaction(Class<? extends Entity> clazz, long id) {
		return getDomainMap(clazz).getAnyTransaction(id);
	}

	EntityIdMap getDomainMap(Class<? extends Entity> clazz) {
		return (EntityIdMap) domain.get(clazz);
	}

	void initialiseMap(Class clazz) {
		super.ensureMap(clazz);
	}

	void putExternalLocal(Entity instance) {
		super.put0(instance, true);
	}
}
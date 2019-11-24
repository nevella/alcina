package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;

public class DetachedEntityCacheTransactionalMap extends DetachedEntityCache
		implements DomainStoreCache {
	/*
	 * return a set view of the real thing
	 * 
	 * FIXME
	 */
	@Override
	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		return (Set<T>) detached.get(clazz).values();
	}

	@Override
	protected void ensureMaps(Class clazz) {
		if (!detached.containsKey(clazz)) {
			synchronized (this) {
				if (!detached.containsKey(clazz)) {
					detached.put(clazz,
							new TransactionalMap(Long.class, clazz));
				}
			}
		}
	}
}
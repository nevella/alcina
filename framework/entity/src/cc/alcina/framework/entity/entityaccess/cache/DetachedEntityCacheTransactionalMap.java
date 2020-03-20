package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.TransactionalMap;

/*
 * 
 *	objects created in phase TO_DB_PREPARING (with a local id) are indexed with the negative of their local id
 *
 *
 *
 */
public class DetachedEntityCacheTransactionalMap extends DetachedEntityCache
		implements DomainStoreCache {
	/*
	 * return a set view of the real thing
	 * 
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
package cc.alcina.framework.entity.persistence.domain;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;

/*
 * FIXME - reflection
 * 
 * @Override public <T> T newInstance(Class<T> clazz, long objectId, long
 * localId) { try { Entity newInstance = Transaction.current().create((Class)
 * clazz, DomainStore.stores().storeFor(clazz), objectId, localId); return (T)
 * newInstance; } catch (Exception e) { throw new WrappedRuntimeException(e); }
 * }
 */
public class SubgraphTransformManager extends TransformManager {
	protected DetachedCacheObjectStore store;

	public SubgraphTransformManager() {
		super();
		initObjectStore();
	}

	@Override
	protected Object ensureEndpointInTransformGraph(Object object) {
		if (object instanceof Entity) {
			return getObject((Entity) object);
		}
		return object;
	}

	public DetachedEntityCache getDetachedEntityCache() {
		return store.getCache();
	}

	@Override
	protected Entity getEntityForCreate(DomainTransformEvent event) {
		return null;
	}

	public DetachedCacheObjectStore getStore() {
		return this.store;
	}

	@Override
	protected void initObjectStore() {
		store = new DetachedCacheObjectStore(new DomainStoreEntityCache());
		setObjectStore(store);
	}
}

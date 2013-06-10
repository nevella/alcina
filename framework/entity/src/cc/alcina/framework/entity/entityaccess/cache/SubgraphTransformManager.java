package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;

public class SubgraphTransformManager extends TransformManager {
	private DetachedCacheObjectStore store;

	public SubgraphTransformManager() {
		super();
		createObjectLookup();
	}

	@Override
	protected void createObjectLookup() {
		store = new DetachedCacheObjectStore();
		setDomainObjects(store);
	}

	public DetachedEntityCache getDetachedEntityCache() {
		return store.cache;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}
}

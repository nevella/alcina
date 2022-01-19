package cc.alcina.framework.entity.persistence.domain;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

public class PermissionsTestingTransformManager
		extends ThreadlocalTransformManager implements LazyObjectLoader {
	protected DetachedCacheObjectStore store;

	public PermissionsTestingTransformManager() {
		super();
		initObjectStore();
	}

	@Override
	public TransformManager getT() {
		return this;
	}

	@Override
	public <T extends Entity> void loadObject(Class<? extends T> c, long id,
			long localId) {
		T t = Domain.detachedVersion(c, id);
		store.mapObject(t);
	}

	@Override
	protected void initObjectStore() {
		store = new DetachedCacheObjectStore(new DetachedEntityCache());
		store.setLazyObjectLoader(this);
		setObjectStore(store);
	}
}
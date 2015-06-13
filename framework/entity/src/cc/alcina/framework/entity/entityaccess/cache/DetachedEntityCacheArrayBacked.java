package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;

public class DetachedEntityCacheArrayBacked extends DetachedEntityCache {
	@Override
	protected void ensureMaps(Class clazz) {
		if (!detached.containsKey(clazz)) {
			System.out.println("Ensure map - " + clazz.getSimpleName());
			detached.put(clazz, new ArrayBackedLongMap<HasIdAndLocalId>());
		}
	}
}
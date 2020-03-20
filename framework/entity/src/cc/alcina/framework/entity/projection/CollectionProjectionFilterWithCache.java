package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupJvm;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

public class CollectionProjectionFilterWithCache
		extends CollectionProjectionFilter {
	protected DetachedEntityCache cache = new DetachedEntityCache();

	// just used for reachability usages
	private MapObjectLookupJvm objectLookup = new MapObjectLookupJvm();

	@Override
	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		if (original instanceof Entity) {
			Entity entity = (Entity) original;
			if (entity.getId() != 0) {
				Object cached = cache.get(original.getClass(), entity.getId());
				if (cached != null) {
					return (T) cached;
				} else {
					Entity clonedEntity = (Entity) projected;
					clonedEntity.setId(entity.getId());
					cache.put(clonedEntity);
					objectLookup.mapObject(clonedEntity);
					return (T) clonedEntity;
				}
			} else {
				objectLookup.mapObject(entity);
			}
		}
		return super.filterData(original, projected, context, graphProjection);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public MapObjectLookupJvm getObjectLookup() {
		return this.objectLookup;
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}
}

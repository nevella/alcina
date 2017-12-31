package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
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
		if (original instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) original;
			if (hili.getId() != 0) {
				Object cached = cache.get(original.getClass(), hili.getId());
				if (cached != null) {
					return (T) cached;
				} else {
					HasIdAndLocalId clonedHili = (HasIdAndLocalId) projected;
					clonedHili.setId(hili.getId());
					cache.put(clonedHili);
					objectLookup.mapObject(clonedHili);
					return (T) clonedHili;
				}
			} else {
				objectLookup.mapObject(hili);
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

package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

public class CollectionProjectionFilterWithCache extends
		CollectionProjectionFilter {
	protected DetachedEntityCache cache=new DetachedEntityCache();

	@Override
	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		if (original instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) original;
			Object cached = cache.get(original.getClass(), hili.getId());
			if (cached != null) {
				return (T) cached;
			} else {
				HasIdAndLocalId clonedHili = (HasIdAndLocalId) projected;
				clonedHili.setId(hili.getId());
				cache.put(clonedHili);
				return (T) clonedHili;
			}
		}
		return super.filterData(original, projected, context, graphProjection);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}
}

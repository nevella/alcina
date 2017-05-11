package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.cache.CacheQuery;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

public class AlcinaMemCacheQuery extends CacheQuery<AlcinaMemCacheQuery> {
	private GraphProjectionFieldFilter fieldFilter;

	private GraphProjectionDataFilter dataFilter;

	public AlcinaMemCacheQuery() {
	}

	public <T extends HasIdAndLocalId> List<T> allRaw(Class<T> clazz) {
		raw = true;
		ids = AlcinaMemCache.get().getIds(clazz);
		return list(clazz);
	}

	public AlcinaMemCacheQuery
			dataFilter(GraphProjectionDataFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public AlcinaMemCacheQuery
			fieldFilter(GraphProjectionFieldFilter fieldFilter) {
		this.fieldFilter = fieldFilter;
		return this;
	}

	public String getCanonicalPropertyPath(Class clazz, String propertyPath) {
		return AlcinaMemCache.get().getCanonicalPropertyPath(clazz,
				propertyPath);
	}

	public GraphProjectionDataFilter getDataFilter() {
		if (dataFilter == null) {
			dataFilter = Registry.impl(CollectionProjectionFilter.class);
		}
		return this.dataFilter;
	}

	public GraphProjectionFieldFilter getFieldFilter() {
		if (fieldFilter == null) {
			fieldFilter = Registry.impl(PermissibleFieldFilter.class);
		}
		return this.fieldFilter;
	}

	@Override
	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return AlcinaMemCache.get().list(clazz, this);
	}

	public <T extends HasIdAndLocalId> Stream<T>
			streamRegistered(Class<T> clazz) {
		List<T> list = list(clazz);
		list.forEach(t -> TransformManager.get().registerDomainObject(t));
		return list.stream();
	}
}

package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.util.CommonUtils;

public class CacheFilter {
	public String propertyPath;

	public Object propertyValue;

	public CollectionFilter collectionFilter;

	public FilterOperator filterOperator;

	public CacheFilter(String propertyPath, Object propertyValue) {
		this(propertyPath, propertyValue, FilterOperator.EQ);
	}

	public CacheFilter(CollectionFilter collectionFilter) {
		this.collectionFilter = collectionFilter;
	}

	public CacheFilter(String key, Object value, FilterOperator operator) {
		this.propertyPath = key;
		this.propertyValue = value;
		this.filterOperator = operator;
	}

	public static List<CacheFilter> fromKvs(Object... objects) {
		List<CacheFilter> result = new ArrayList<CacheFilter>();
		for (int i = 0; i < objects.length; i += 2) {
			result.add(new CacheFilter((String) objects[i], objects[i + 1]));
		}
		return result;
	}

	@Override
	public String toString() {
		if (collectionFilter != null) {
			return CommonUtils.formatJ("CacheFilter: %s - %s", collectionFilter
					.getClass().getSimpleName(), collectionFilter);
		}
		return CommonUtils.formatJ("CacheFilter: %s %s %s", propertyPath,
				filterOperator.operationText(), propertyValue);
	}

	public CollectionFilter asCollectionFilter() {
		return collectionFilter != null ? collectionFilter
				: new PropertyPathFilter(propertyPath, propertyValue,
						filterOperator);
	}

	public boolean canFlatten() {
		return collectionFilter==null;
	}
}

package cc.alcina.framework.common.client.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PropertyFilter<T> implements CollectionFilter<T> {
	Map<String, Object> keyValues = new LinkedHashMap<String, Object>();

	public PropertyFilter() {
	}

	public PropertyFilter(String key, Object value) {
		add(key, value);
	}

	public PropertyFilter add(String key, Object value) {
		keyValues.put(key, value);
		return this;
	}

	@Override
	public boolean allow(T o) {
		for (Entry<String, Object> entry : keyValues.entrySet()) {
			if (!CommonUtils.equalsWithNullEquality(CommonLocator.get()
					.propertyAccessor().getPropertyValue(o, entry.getKey()),
					entry.getValue())) {
				return false;
			}
		}
		return true;
	}
}

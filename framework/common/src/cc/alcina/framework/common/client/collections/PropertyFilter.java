package cc.alcina.framework.common.client.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PropertyFilter<T> implements CollectionFilter<T> {
	public static final transient Object NOT_NULL = new Object();

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
			Object propertyValue = CommonLocator.get().propertyAccessor()
					.getPropertyValue(o, entry.getKey());
			boolean match = false;
			if (entry.getValue() == NOT_NULL && propertyValue != null) {
				match = true;
			}
			if (CommonUtils.equalsWithNullEquality(propertyValue,
					entry.getValue())) {
				match = true;
			}
			if (!match) {
				return false;
			}
		}
		return true;
	}
}

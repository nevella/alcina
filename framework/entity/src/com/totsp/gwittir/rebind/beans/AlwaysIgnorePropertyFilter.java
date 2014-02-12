package com.totsp.gwittir.rebind.beans;

import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.collections.CollectionFilter;

class AlwaysIgnorePropertyFilter implements
		CollectionFilter<Map.Entry<String, RProperty>> {
	@Override
	public boolean allow(Entry<String, RProperty> o) {
		int readMethodParamCount = o.getValue().getReadMethod() != null ? o
				.getValue().getReadMethod().getBaseMethod().getParameters().length
				: 0;
		int writeMethodParamCount = o.getValue().getWriteMethod() != null ? o
				.getValue().getWriteMethod().getBaseMethod().getParameters().length
				: 0;
		return o.getValue().getReadMethod() != null
				&& !o.getKey().equals("class")
				&& !o.getKey().equals("propertyChangeListeners")
				&& readMethodParamCount == 0 && writeMethodParamCount < 2;
	}
}
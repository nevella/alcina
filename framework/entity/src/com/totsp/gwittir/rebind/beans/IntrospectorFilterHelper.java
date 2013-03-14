package com.totsp.gwittir.rebind.beans;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;

public class IntrospectorFilterHelper {
	public static IntrospectorFilter getFilter(GeneratorContext context) {
		try {
			ConfigurationProperty cp = context.getPropertyOracle()
					.getConfigurationProperty(
							IntrospectorFilter.ALCINA_INTROSPECTOR_FILTER_CLASSNAME);
			for (String filterClassName : cp.getValues()) {
				if (CommonUtils.isNotNullOrEmpty(filterClassName)) {
					try {
						IntrospectorFilter filter = (IntrospectorFilter) Class
								.forName(filterClassName).newInstance();
						filter.setContext(context);
						return filter;
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			}
			return new IntrospectorFilterPassthrough();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
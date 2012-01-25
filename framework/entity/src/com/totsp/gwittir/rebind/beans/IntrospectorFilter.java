package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

public interface IntrospectorFilter {
	void filterIntrospectorResults(List<BeanResolver> results);

	void filterAnnotations(List<JAnnotationType> jAnns,
			List<Class<? extends Annotation>> visibleAnnotationClasses);

	void filterReflectionInfo(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses);

	public static class IntrospectorFilterHelper {
		private static final String ALCINA_INTROSPECTOR_FILTER_CLASSNAME = "alcina.introspectorFilter.classname";

		public static IntrospectorFilter getFilter(GeneratorContext context) {
			try {
				ConfigurationProperty cp = context.getPropertyOracle()
						.getConfigurationProperty(
								ALCINA_INTROSPECTOR_FILTER_CLASSNAME);
				for (String filterClassName : cp.getValues()) {
					if (CommonUtils.isNotNullOrEmpty(filterClassName)) {
						try {
							IntrospectorFilter filter = (IntrospectorFilter) Class
									.forName(filterClassName).newInstance();
							return filter;
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				}
				return null;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

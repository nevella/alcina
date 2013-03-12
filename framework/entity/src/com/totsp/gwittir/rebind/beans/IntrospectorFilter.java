package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.NotIntrospected;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

public interface IntrospectorFilter {
	public static class IntrospectorFilterPassthrough implements
			IntrospectorFilter {
		@Override
		public void filterIntrospectorResults(List<BeanResolver> results) {
			// do nothing
		}

		@Override
		public void filterAnnotations(List<JAnnotationType> jAnns,
				List<Class<? extends Annotation>> visibleAnnotationClasses) {
			// do nothing
		}

		@Override
		public void filterReflectionInfo(List<JClassType> beanInfoTypes,
				List<JClassType> instantiableTypes,
				Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses) {
			// do nothing
		}

		@Override
		public boolean emitBeanResolver(BeanResolver resolver) {
			return true;
		}

		@Override
		public void filterProperties(BeanResolver resolver) {
		}

		@Override
		public void setModuleName(String value) {
		}

		@Override
		public boolean omitForModule(JClassType jClassType) {
			return false;
		}
	}

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
				return new IntrospectorFilterPassthrough();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	boolean emitBeanResolver(BeanResolver resolver);

	public static abstract class IntrospectorFilterBase implements
			IntrospectorFilter {
		private String moduleName="";

		private Map<JClassType, Boolean> implBoundWidgetMap = new LinkedHashMap<JClassType, Boolean>();

		protected boolean isImplBoundWidget(BeanResolver resolver) {
			return isImplBoundWidgetJct(resolver.getType());
		}

		protected boolean isImplBoundWidgetJct(JClassType t) {
			if (!implBoundWidgetMap.containsKey(t)) {
				JClassType[] interfaces = t.getImplementedInterfaces();
				boolean implBoundWidget = false;
				for (JClassType jClassType : interfaces) {
					if (jClassType.getQualifiedSourceName().contains(
							"BoundWidget")) {
						implBoundWidget = true;
					}
				}
				while (t != null) {
					if (t.getQualifiedSourceName().contains("BoundWidget")) {
						implBoundWidget = true;
					}
					t = t.getSuperclass();
				}
				implBoundWidgetMap.put(t, implBoundWidget);
			}
			return implBoundWidgetMap.get(t);
		}

		protected CollectionFilter<Entry<String, RProperty>> valueOnlyFilter = new CollectionFilter<Map.Entry<String, RProperty>>() {
			@Override
			public boolean allow(Entry<String, RProperty> o) {
				return o.getKey().equals("value");
			}
		};

		protected void filterPropertiesCollection(BeanResolver resolver,
				CollectionFilter<Entry<String, RProperty>> filter) {
			Map<String, RProperty> properties = resolver.getProperties();
			CollectionFilters.filterInPlace(properties.entrySet(), filter);
		}

		@Override
		public void filterIntrospectorResults(List<BeanResolver> results) {
			CollectionFilters.filterInPlace(results,
					new CollectionFilter<BeanResolver>() {
						@Override
						public boolean allow(BeanResolver o) {
							JClassType t = o.getType();
							return !t
									.isAnnotationPresent(NotIntrospected.class)
									&& emitBeanResolver(o);
						}
					});
		}

		CollectionFilter<Entry<String, RProperty>> alwaysIgnoreFilter = new CollectionFilter<Map.Entry<String, RProperty>>() {
			@Override
			public boolean allow(Entry<String, RProperty> o) {
				return o.getValue().getReadMethod() != null
						&& o.getValue().getWriteMethod() != null;
			}
		};

		@Override
		public void filterProperties(BeanResolver resolver) {
			CollectionFilters.filterInPlace(
					resolver.getProperties().entrySet(), alwaysIgnoreFilter);
		}

		public String getModuleName() {
			return this.moduleName;
		}

		public void setModuleName(String moduleName) {
			this.moduleName = moduleName;
		}
	}

	void filterProperties(BeanResolver resolver);

	void setModuleName(String value);

	boolean omitForModule(JClassType jClassType);
}

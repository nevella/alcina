package com.totsp.gwittir.rebind.beans;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.NotIntrospected;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;

public abstract class IntrospectorFilterBase implements IntrospectorFilter {
	private GeneratorContext context;

	private String moduleName;

	private Map<JClassType, Boolean> implBoundWidgetMap = new LinkedHashMap<JClassType, Boolean>();

	protected boolean isImplBoundWidget(BeanResolver resolver) {
		return isImplBoundWidgetJct(resolver.getType());
	}

	protected boolean isImplBoundWidgetJct(JClassType t) {
		if (!implBoundWidgetMap.containsKey(t)) {
			JClassType[] interfaces = t.getImplementedInterfaces();
			boolean implBoundWidget = false;
			for (JClassType jClassType : interfaces) {
				if (jClassType.getQualifiedSourceName().contains("BoundWidget")) {
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
						return !t.isAnnotationPresent(NotIntrospected.class)
								&& emitBeanResolver(o);
					}
				});
	}

	CollectionFilter<Entry<String, RProperty>> alwaysIgnoreFilter = new CollectionFilter<Map.Entry<String, RProperty>>() {
		@Override
		public boolean allow(Entry<String, RProperty> o) {
			return o.getValue().getReadMethod() != null
					&& !o.getKey().equals("class")
					&& !o.getKey().equals("propertyChangeListeners");
		}
	};

	@Override
	public void filterProperties(BeanResolver resolver) {
		CollectionFilters.filterInPlace(resolver.getProperties().entrySet(),
				alwaysIgnoreFilter);
	}

	public String getModuleName() {
		return this.moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public GeneratorContext getContext() {
		return this.context;
	}

	public void setContext(GeneratorContext context) {
		this.context = context;
	}

	@Override
	public void generationComplete() {
	}
}
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

	private Map<JClassType, Boolean> uiObjectMap = new LinkedHashMap<JClassType, Boolean>();

	protected boolean isUiObject(BeanResolver resolver) {
		return isUiObject(resolver.getType());
	}

	protected boolean isUiObject(JClassType t) {
		if (!uiObjectMap.containsKey(t)) {
			boolean implBoundWidget = false;
			for (JClassType jClassType : t.getFlattenedSupertypeHierarchy()) {
				if (jClassType.getQualifiedSourceName().contains("UIObject")) {
					implBoundWidget = true;
					break;
				}
			}
			uiObjectMap.put(t, implBoundWidget);
		}
		return uiObjectMap.get(t);
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
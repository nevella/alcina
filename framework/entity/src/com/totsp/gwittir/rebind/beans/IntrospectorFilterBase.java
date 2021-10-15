package com.totsp.gwittir.rebind.beans;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.reflection.NotIntrospected;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;

public abstract class IntrospectorFilterBase implements IntrospectorFilter {
	private GeneratorContext context;

	private String moduleName;

	private Map<JClassType, Boolean> uiObjectMap = new LinkedHashMap<JClassType, Boolean>();

	protected Predicate<Entry<String, RProperty>> valueOnlyFilter = new Predicate<Map.Entry<String, RProperty>>() {
		@Override
		public boolean test(Entry<String, RProperty> o) {
			return o.getKey().equals("value");
		}
	};

	@Override
	public void filterIntrospectorResults(List<BeanResolver> results) {
		results.removeIf(o -> {
			JClassType t = o.getType();
			return t.isAnnotationPresent(NotIntrospected.class)
					|| !emitBeanResolver(o);
		});
	}

	@Override
	public void filterProperties(BeanResolver resolver) {
		resolver.getProperties().entrySet()
				.removeIf(new AlwaysIgnorePropertyFilter());
	}

	@Override
	public void generationComplete() {
	}

	public GeneratorContext getContext() {
		return this.context;
	}

	@Override
	public String getModuleName() {
		return this.moduleName;
	}

	@Override
	public boolean isReflectableJavaCollectionClass(JClassType jClassType) {
		return moduleName.equals(ReflectionModule.INITIAL)
				? IntrospectorFilter.super.isReflectableJavaCollectionClass(
						jClassType)
				: false;
	}

	@Override
	public void setContext(GeneratorContext context) {
		this.context = context;
	}

	@Override
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	protected void filterPropertiesCollection(BeanResolver resolver,
			Predicate<Entry<String, RProperty>> filter) {
		Map<String, RProperty> properties = resolver.getProperties();
		properties.entrySet().removeIf(filter.negate());
	}

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
}
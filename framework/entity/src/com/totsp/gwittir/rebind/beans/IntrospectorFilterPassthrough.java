package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

public class IntrospectorFilterPassthrough implements
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
		CollectionFilters.filterInPlace(resolver.getProperties().entrySet(),
				new AlwaysIgnorePropertyFilter());
	}

	@Override
	public void setModuleName(String value) {
	}

	@Override
	public boolean omitForModule(JClassType jClassType,ReflectionAction reflectionAction) {
		return false;
	}

	@Override
	public void setContext(GeneratorContext context) {
	}

	@Override
	public void generationComplete() {
	}

	@Override
	public String getModuleName() {
		return ReflectionModule.INITIAL;
	}
}
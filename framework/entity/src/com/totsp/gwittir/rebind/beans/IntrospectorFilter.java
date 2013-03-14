package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

public interface IntrospectorFilter {
	public static final String ALCINA_INTROSPECTOR_FILTER_CLASSNAME = "alcina.introspectorFilter.classname";

	public static final String ALCINA_INTROSPECTOR_FILTER_DATA_FILE = "alcina.introspectorFilter.dataFile";

	void filterIntrospectorResults(List<BeanResolver> results);

	void filterAnnotations(List<JAnnotationType> jAnns,
			List<Class<? extends Annotation>> visibleAnnotationClasses);

	void filterReflectionInfo(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses);

	boolean emitBeanResolver(BeanResolver resolver);

	void setContext(GeneratorContext context);

	static Pattern sourceToBinary=Pattern.compile("(.+\\.[A-Z].+)(\\.)([A-Z].+)");  
	void filterProperties(BeanResolver resolver);

	void setModuleName(String value);

	boolean omitForModule(JClassType jClassType, ReflectionAction reflectionAction);

	void generationComplete();

	String getModuleName();
}

package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

public interface IntrospectorFilter {
	public static final String ALCINA_INTROSPECTOR_FILTER_CLASSNAME = "alcina.introspectorFilter.classname";

	public static final String ALCINA_INTROSPECTOR_FILTER_DATA_FILE = "alcina.introspectorFilter.dataFile";

	static Pattern sourceToBinary = Pattern
			.compile("(.+\\.[A-Z].+)(\\.)([A-Z].+)");

	boolean emitBeanResolver(BeanResolver resolver);

	void filterAnnotations(List<JAnnotationType> jAnns,
			List<Class<? extends Annotation>> visibleAnnotationClasses);

	void filterIntrospectorResults(List<BeanResolver> results);

	void filterProperties(BeanResolver resolver);

	void filterReflectionInfo(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses);

	void generationComplete();

	String getModuleName();

	boolean omitForModule(JClassType jClassType,
			ReflectionAction reflectionAction);

	void setContext(GeneratorContext context);

	void setModuleName(String value);
}

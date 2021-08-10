package com.totsp.gwittir.rebind.beans;

import java.lang.annotation.Annotation;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

public interface IntrospectorFilter {
	public static final String ALCINA_INTROSPECTOR_FILTER_CLASSNAME = "alcina.introspectorFilter.classname";

	public static final String ALCINA_INTROSPECTOR_FILTER_DATA_FILE = "alcina.introspectorFilter.dataFile";

	static Pattern sourceToBinary = Pattern
			.compile("(.+\\.[A-Z].+)(\\.)([A-Z].+)");

	public static final Set<String> COLLECTION_CLASS_NAMES = Arrays
			.asList(ArrayList.class, LinkedList.class, HashSet.class,
					LinkedHashSet.class, TreeSet.class, HashMap.class,
					LinkedHashMap.class, TreeMap.class, LightSet.class,
					LiSet.class, LightMap.class)
			.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

	public static final Set<String> CORE_CLASS_NAMES = Arrays
			.asList(Class.class, Date.class, Timestamp.class).stream()
			.map(Class::getCanonicalName).collect(Collectors.toSet());

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

	default boolean isReflectableJavaCollectionClass(JClassType jClassType) {
		return COLLECTION_CLASS_NAMES
				.contains(jClassType.getQualifiedSourceName());
	}

	default boolean isReflectableJavaCoreClass(JClassType jClassType) {
		return CORE_CLASS_NAMES.contains(jClassType.getQualifiedSourceName());
	}

	boolean omitForModule(JClassType jClassType,
			ReflectionAction reflectionAction);

	void setContext(GeneratorContext context);

	void setModuleName(String value);
}

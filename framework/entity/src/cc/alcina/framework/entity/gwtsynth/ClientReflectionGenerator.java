/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.gwtsynth;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.client.beans.annotations.Omit;
import com.totsp.gwittir.rebind.beans.IntrospectorFilter;
import com.totsp.gwittir.rebind.beans.IntrospectorFilterHelper;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ToStringComparator;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.SEUtilities;

/**
 * Currently, it's a schemozzle - this was originally a standalone generator, so
 * there's a mishmash of usages of JVM vs GWT reflection - it does, however,
 * work, it's acceptably fast, it can be beautified later
 *
 * -- update, pretty sure all usages of class changed to jclasstype where
 * appropriate...
 * 
 * @author Nick Reddel
 */
public class ClientReflectionGenerator extends Generator {
	public static final CollectionFilter<RegistryLocation> CLIENT_VISIBLE_ANNOTATION_FILTER = new CollectionFilter<RegistryLocation>() {
		@Override
		public boolean allow(RegistryLocation o) {
			return o.registryPoint()
					.getAnnotation(NonClientRegistryPointType.class) == null;
		}
	};

	// Annotation utils for gwt
	// presence class, annotation simple name
	private static UnsortedMultikeyMap<Annotation> classNameAnnotationMap = new UnsortedMultikeyMap<>(
			2);

	public static boolean hasAnnotationNamed(JClassType clazz,
			Class<? extends Annotation> ann) {
		String annClazzName = ann.getSimpleName();
		if (!classNameAnnotationMap.containsKey(clazz, annClazzName)) {
			JClassType c = clazz;
			Annotation found = null;
			while (c != null && found == null) {
				for (Annotation a : c.getAnnotations()) {
					if (a.annotationType().getSimpleName()
							.equals(annClazzName)) {
						found = a;
						break;
					}
				}
				c = c.getSuperclass();
			}
			classNameAnnotationMap.put(clazz, annClazzName, found);
		}
		return classNameAnnotationMap.get(clazz, annClazzName) != null;
	}

	private String packageName = ClientReflector.class.getCanonicalName()
			.substring(0,
					ClientReflector.class.getCanonicalName().lastIndexOf("."));

	private boolean debug = false;

	SourceWriter sw;

	private Map<Class, String> ann2impl = new HashMap<Class, String>();

	private ArrayList<Class<? extends Annotation>> visibleAnnotationClasses;

	Map<Class, JClassType> ctLookup = new HashMap<Class, JClassType>();

	private Map<PrintWriter, BiWriter> wrappedWriters = new HashMap<PrintWriter, BiWriter>();

	private HashMap<JMethod, Set<Annotation>> superMethodAnnotationMap = new HashMap<JMethod, Set<Annotation>>();

	private UnsortedMultikeyMap<Set<Annotation>> superAnnotationMap = new UnsortedMultikeyMap<Set<Annotation>>(
			2);

	private IntrospectorFilter filter;

	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		filter = IntrospectorFilterHelper.getFilter(context);
		// System.out.println("ClientReflector generation...");
		long start = System.currentTimeMillis();
		Map<Class, String> ann2impl = new HashMap<Class, String>();
		Map<String, String> simpleNameCheck = new HashMap<String, String>();
		try {
			ClassSourceFileComposerFactory crf = null;
			// scan for reflectable annotations etc
			String superClassName = null;
			JClassType intrType = context.getTypeOracle().getType(typeName);
			if (intrType.isInterface() != null) {
				intrType = context.getTypeOracle()
						.getType(ClientReflector.class.getName());
			}
			ReflectionModule module = intrType
					.getAnnotation(ReflectionModule.class);
			String moduleName = module.value();
			filter.setModuleName(moduleName);
			String implementationName = String.format("ClientReflector_%s_Impl",
					moduleName);
			superClassName = intrType.getQualifiedSourceName();
			crf = new ClassSourceFileComposerFactory(this.packageName,
					implementationName);
			PrintWriter printWriter = context.tryCreate(logger, packageName,
					implementationName);
			if (printWriter == null) {
				return packageName + "." + implementationName;
			}
			crf.addImport(LinkedHashMap.class.getName());
			crf.addImport(Map.class.getName());
			crf.addImport(GWT.class.getName());
			crf.addImport(JavaScriptObject.class.getName());
			crf.addImport(Registry.class.getName());
			crf.addImport(Annotation.class.getName());
			crf.addImport(UnsafeNativeLong.class.getName());
			crf.setSuperclass(superClassName);
			crf.addImport(ClientBeanReflector.class.getName());
			crf.addImport(ClientPropertyReflector.class.getName());
			crf.addImport(ClientReflector.class.getName());
			crf.addImport(RegistryLocation.class.getName());
			ctLookup.clear();
			visibleAnnotationClasses = new ArrayList<Class<? extends Annotation>>();
			List<JAnnotationType> jAnns = this.getClientVisibleAnnotations(
					logger, context.getTypeOracle());
			for (JAnnotationType jAnnotationType : jAnns) {
				visibleAnnotationClasses.add((Class<? extends Annotation>) Class
						.forName(jAnnotationType.getQualifiedBinaryName()));
			}
			visibleAnnotationClasses.add(Omit.class);
			filter.filterAnnotations(jAnns, visibleAnnotationClasses);
			writeAnnotations(logger, context, jAnns, crf,
					moduleName.equals(ReflectionModule.INITIAL));
			List<JClassType> beanInfoTypes = this.getBeanInfoTypes(logger,
					context.getTypeOracle(), crf);
			List<JClassType> instantiableTypes = this
					.getInstantiableTypes(logger, context.getTypeOracle(), crf);
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses = getRegistryAnnotations(
					context.getTypeOracle());
			filter.filterReflectionInfo(beanInfoTypes, instantiableTypes,
					gwtRegisteringClasses);
			SourceWriter srcW = createWriter(crf, printWriter);
			writeIt(beanInfoTypes, instantiableTypes, srcW,
					gwtRegisteringClasses, implementationName);
			commit(context, logger, printWriter);
			System.out.format(
					"Client reflection generation  [%s] - "
							+ "%s annotations, %s beans, "
							+ "%s instantiable types - %s ms\n",
					filter.getModuleName(), jAnns.size(), beanInfoTypes.size(),
					instantiableTypes.size(),
					System.currentTimeMillis() - start);
			filter.generationComplete();
			return packageName + "." + implementationName;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	public Set<Annotation> getClassAnnotations(JClassType clazz,
			List<Class<? extends Annotation>> annotationClasses,
			boolean allowMultiple) {
		if (superAnnotationMap.containsKey(clazz, annotationClasses)) {
			return superAnnotationMap.get(clazz, annotationClasses);
		}
		Map<Class, Annotation> uniqueMap = new HashMap<Class, Annotation>();
		Set<? extends JClassType> flattenedSupertypeHierarchy = clazz
				.getFlattenedSupertypeHierarchy();
		// uhoh-nono
		// List<? extends JClassType> nonGeneric = flattenedSupertypeHierarchy
		// .stream().map(cl -> {
		// if (cl instanceof JParameterizedType) {
		// return ((JParameterizedType) cl).getBaseType();
		// } else {
		// return cl;
		// }
		// }).collect(Collectors.toList());
		Set values = new LinkedHashSet();// order important - lowest ordinal,
											// highest priority
		for (JClassType jct : flattenedSupertypeHierarchy) {
			try {
				List<Annotation> visibleAnnotations = getVisibleAnnotations(jct,
						annotationClasses);
				values.addAll(visibleAnnotations);
				for (Annotation a : visibleAnnotations) {
					if (!uniqueMap.containsKey(a.annotationType())) {
						uniqueMap.put(a.annotationType(), a);
					}
				}
			} catch (Exception e) {
			}
		}
		if (!allowMultiple) {
			values = uniqueMap.values().stream().collect(Collectors.toSet());
		}
		superAnnotationMap.put(clazz, annotationClasses, values);
		return values;
	}

	public Set<Annotation> getSuperclassAnnotationsForMethod(JMethod m) {
		if (superMethodAnnotationMap.containsKey(m)) {
			return superMethodAnnotationMap.get(m);
		}
		Map<Class, Annotation> uniqueMap = new HashMap<Class, Annotation>();
		JClassType c = m.getEnclosingType();
		Set<? extends JClassType> flattenedSupertypeHierarchy = c
				.getFlattenedSupertypeHierarchy();
		for (JClassType jct : flattenedSupertypeHierarchy) {
			try {
				JMethod m2 = jct.getMethod(m.getName(), m.getParameterTypes());
				for (Annotation a : getVisibleAnnotations(m2,
						visibleAnnotationClasses)) {
					if (!uniqueMap.containsKey(a.annotationType())) {
						uniqueMap.put(a.annotationType(), a);
					}
				}
			} catch (Exception e) {
			}
		}
		HashSet values = new HashSet(uniqueMap.values());
		superMethodAnnotationMap.put(m, values);
		return values;
	}

	public SourceWriter getSw() {
		return this.sw;
	}

	public void writeIt(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes, SourceWriter sw,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses,
			String implName) throws Exception {
		String qualifiedImplName = this.packageName + "." + implName;
		Map<JClassType, String> initClassMethodNames = new LinkedHashMap<>();
		Map<JClassType, String> initNewInstanceNames = new LinkedHashMap<>();
		List<String> methodLines = new ArrayList<String>();
		sw.indent();
		sw.println("private JavaScriptObject createLookup;");
		sw.println();
		sw.println(String.format("public %s() {", implName));
		sw.indent();
		sw.println("super();");
		sw.println("init();");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println("@Override");
		sw.println("@UnsafeNativeLong");
		sw.println(
				"public native <T> T newInstance0(Class<T> clazz, long objectId, long localId) /*-{");
		sw.indent();
		sw.println(String.format(
				"var constructor = this.@%s::createLookup.get(clazz);",
				qualifiedImplName));
		sw.println("return constructor ? constructor() : null;");
		sw.outdent();
		sw.println("}-*/;");
		sw.println();
		sw.println();
		int methodCount = 0;
		for (JClassType jct : beanInfoTypes) {
			if (filter.omitForModule(jct,
					ReflectionAction.BEAN_INFO_DESCRIPTOR)) {
				continue;
			}
			String methodName = "initClass" + (methodCount++);
			initClassMethodNames.put(jct, methodName);
			sw.println(String.format("private void %s(){", methodName));
			sw.indent();
			sw.println(
					"Map<String,ClientPropertyReflector> propertyReflectors = new LinkedHashMap<String,ClientPropertyReflector>();");
			for (JMethod method : getPropertyGetters(jct)) {
				String propertyName = getPropertyNameForReadMethod(method);
				if (propertyName.equals("class")
						|| propertyName.equals("propertyChangeListeners")) {
					continue;
				}
				if (method.isStatic()) {
					continue;
				}
				Collection<Annotation> annotations = getSuperclassAnnotationsForMethod(
						method);
				int aCount = 0;
				String annArray = "";
				boolean ignore = false;
				for (Annotation a : annotations) {
					if (a.annotationType() == Omit.class) {
						ignore = true;
					}
				}
				if (ignore) {
					continue;
				}
				sw.println("{");
				sw.indent();
				for (Annotation a : annotations) {
					if (!a.annotationType()
							.isAnnotationPresent(ClientVisible.class)
							|| a.annotationType() == RegistryLocation.class) {
						continue;
					}
					if (aCount++ != 0) {
						annArray += ", ";
					}
					String annImpl = getAnnImpl(a, ann2impl, aCount);
					annArray += "a" + aCount;
					sw.println(annImpl);
				}
				sw.println(String.format(
						"ClientPropertyReflector reflector = "
								+ "new ClientPropertyReflector(\"%s\",%s.class,"
								+ " new Annotation[]{%s}) ;",
						propertyName,
						method.getReturnType().getQualifiedSourceName(),
						annArray));
				sw.println(
						"propertyReflectors.put(reflector.getPropertyName(), reflector);");
				sw.outdent();
				sw.println("}");
			}
			int aCount = 0;
			String annArray = "";
			for (Annotation a : getClassAnnotations(jct,
					visibleAnnotationClasses, false)) {
				if (aCount++ != 0) {
					annArray += ", ";
				}
				String annImpl = getAnnImpl(a, ann2impl, aCount);
				annArray += "a" + aCount;
				sw.println(annImpl);
			}
			sw.println(String.format(
					"ClientBeanReflector beanReflector = new ClientBeanReflector("
							+ "%s.class,new Annotation[]{%s},propertyReflectors);",
					jct.getQualifiedSourceName(), annArray));
			sw.println(
					"gwbiMap.put(beanReflector.getBeanClass(),beanReflector );");
			sw.outdent();
			sw.println("}");
			sw.println("");
		}
		Set<JClassType> allTypes = new LinkedHashSet<JClassType>();
		allTypes.addAll(instantiableTypes);
		allTypes.addAll(beanInfoTypes);
		List<JClassType> constructorTypes = CollectionFilters.filter(allTypes,
				new CollectionFilter<JClassType>() {
					@Override
					public boolean allow(JClassType o) {
						return o.isEnum() == null;
					}
				});
		methodCount = 0;
		for (JClassType jClassType : constructorTypes) {
			/*
			 * private native void registerNewInstanceFunction0(Class clazz)/*-{
			 * var closure=this;
			 * this.@au.com.barnet.jade.client.test.TestClientReflector
			 * ::createLookup[clazz] = function() { return
			 * closure.@au.com.barnet
			 * .jade.client.test.TestClientReflector::createInstance0()(); }; }-
			 */;
			String registerMethodName = String
					.format("registerNewInstanceFunction%s", methodCount);
			String createMethodName = String.format("createInstance%s",
					methodCount);
			initNewInstanceNames.put(jClassType, String.format("%s(%s.class);",
					registerMethodName, jClassType.getQualifiedSourceName()));
			sw.println(String.format("private Object %s(){", createMethodName));
			sw.indent();
			sw.println(String.format("return GWT.create(%s.class);",
					jClassType.getQualifiedSourceName()));
			sw.outdent();
			sw.println("};");
			sw.println();
			sw.println(String.format("private native void %s(Class clazz)/*-{",
					registerMethodName));
			sw.indent();
			sw.println("var closure = this;");
			sw.println(
					String.format("var fn = function() {", qualifiedImplName));
			sw.indent();
			sw.println(String.format("return closure.@%s::%s()();",
					qualifiedImplName, createMethodName));
			sw.outdent();
			sw.println("};");
			sw.println(String.format("this.@%s::createLookup.set(clazz,fn);",
					qualifiedImplName));
			sw.outdent();
			sw.println("}-*/;");
			sw.println();
			methodCount++;
		}
		sw.println("private native void initCreateLookup0()/*-{");
		sw.indent();
		sw.println(String.format("this.@%s::createLookup = new Map();",
				qualifiedImplName));
		sw.outdent();
		sw.println("}-*/;");
		sw.println();
		sw.println("protected void initReflector(Class clazz) {");
		sw.indent();
		sw.println("switch(clazz.getName()){");
		sw.indent();
		initClassMethodNames.entrySet().forEach(e -> {
			sw.println("case \"%s\":", e.getKey().getQualifiedBinaryName());
			sw.indent();
			sw.println("%s();", e.getValue());
			sw.println("break;");
			sw.outdent();
		});
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println("protected void initialiseNewInstance(Class clazz) {");
		sw.indent();
		sw.println("switch(clazz.getName()){");
		sw.indent();
		initNewInstanceNames.entrySet().forEach(e -> {
			sw.println("case \"%s\":", e.getKey().getQualifiedBinaryName());
			sw.indent();
			sw.println("%s", e.getValue());
			sw.println("break;");
			sw.outdent();
		});
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println("private void init() {");
		sw.indent();
		sw.println("initCreateLookup0();");
		for (JClassType t : allTypes) {
			if (!filter.omitForModule(t, ReflectionAction.NEW_INSTANCE)) {
				sw.println(String.format("forNameMap.put(\"%s\",%s.class);",
						t.getQualifiedBinaryName(),
						t.getQualifiedSourceName()));
			}
		}
		sw.println("");
		sw.println("//init registry");
		sw.println("");
		for (JClassType clazz : gwtRegisteringClasses.keySet()) {
			for (RegistryLocation l : gwtRegisteringClasses.get(clazz)) {
				StringBuffer sb = new StringBuffer();
				writeAnnImpl(l, ann2impl, 0, false, sb, false);
				sw.println(
						String.format("Registry.get().register(%s.class,%s);",
								clazz.getQualifiedSourceName(), sb));
			}
		}
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
	}

	private void addImport(ClassSourceFileComposerFactory factory,
			Class<?> type) {
		if (!type.isPrimitive()) {
			factory.addImport(type.getCanonicalName().replace("[]", ""));
		}
	}

	private void commit(GeneratorContext context, TreeLogger logger,
			PrintWriter printWriter) {
		context.commit(logger, printWriter);
		if (wrappedWriters.containsKey(printWriter)) {
			System.out.println(wrappedWriters.get(printWriter).getStringWriter()
					.toString());
		}
	}

	private SourceWriter createWriter(ClassSourceFileComposerFactory factory,
			PrintWriter contextWriter) {
		PrintWriter writer = contextWriter;
		if (debug) {
			writer = new BiWriter(writer);
			wrappedWriters.put(contextWriter, (BiWriter) writer);
		}
		return factory.createSourceWriter(writer);
	}

	private Class forName(JType type) throws ClassNotFoundException {
		String name = type.getQualifiedBinaryName();
		return Class.forName(name);
	}

	private String getAnnImpl(Annotation a, Map<Class, String> ann2impl,
			int count) throws Exception {
		StringBuffer sb = new StringBuffer();
		writeAnnImpl(a, ann2impl, count, true, sb, false);
		return sb.toString();
	}

	private List<JClassType> getBeanInfoTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JClassType> results = new ArrayList<JClassType>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jClassType : types) {
			if (jClassType.isAnnotationPresent(
					cc.alcina.framework.common.client.logic.reflection.Bean.class)
					&& !ignore(jClassType,
							ReflectionAction.BEAN_INFO_DESCRIPTOR)) {
				results.add(jClassType);
				crf.addImport(jClassType.getQualifiedSourceName());
			}
		}
		return results;
	}

	private <T> Set<T> getClassAnnotations(JClassType jct,
			Class<T> annotationType, boolean allowMultiple) {
		return (Set) getClassAnnotations(jct,
				(List) Collections.singletonList(annotationType),
				allowMultiple);
	}

	private List<JAnnotationType> getClientVisibleAnnotations(TreeLogger logger,
			TypeOracle oracle) {
		List<JAnnotationType> results = new ArrayList<JAnnotationType>();
		JClassType[] types = oracle.getTypes();
		for (JClassType jClassType : types) {
			if (jClassType.isAnnotationPresent(ClientVisible.class)) {
				JAnnotationType annotation = jClassType.isAnnotation();
				if (annotation != null) {
					results.add(annotation);
				}
			}
		}
		return results;
	}

	private List<JClassType> getInstantiableTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JClassType> results = new ArrayList<JClassType>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jClassType : types) {
			if ((hasAnnotationNamed(jClassType, ClientInstantiable.class)
					|| jClassType.isAnnotationPresent(
							cc.alcina.framework.common.client.logic.reflection.Bean.class))
					&& !ignore(jClassType, ReflectionAction.NEW_INSTANCE)) {
				results.add(jClassType);
				crf.addImport(jClassType.getQualifiedSourceName());
			}
		}
		return results;
	}

	private List<JMethod> getPropertyGetters(JClassType jct) {
		List<JMethod> methods = new ArrayList<JMethod>();
		JMethod[] jms = jct.getInheritableMethods();
		for (JMethod jm : jms) {
			String name = jm.getName();
			if ((name.startsWith("get") && name.length() > 3)
					|| (name.startsWith("is") && name.length() > 2)) {
				if (jm.getParameters().length == 0) {
					methods.add(jm);
				}
			}
		}
		// sortMethodsByFieldName
		Map<String, Integer> fieldOrdinals = new LinkedHashMap<>();
		Class clazz = Reflections.classLookup()
				.getClassForName(jct.getQualifiedBinaryName());
		SEUtilities.allFields(clazz).stream().collect(
				Collectors.toMap(f -> f.getName(), f -> fieldOrdinals.size()));
		Comparator<JMethod> comparator = new Comparator<JMethod>() {
			@Override
			public int compare(JMethod o1, JMethod o2) {
				int ordinal1 = fieldOrdinals.computeIfAbsent(o1.getName(),
						key -> -1);
				int ordinal2 = fieldOrdinals.computeIfAbsent(o1.getName(),
						key -> -1);
				int i = ordinal1 - ordinal2;
				if (i != 0) {
					return i;
				}
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(methods, comparator);
		return methods;
	}

	private String getPropertyNameForReadMethod(JMethod method) {
		String name = method.getName();
		int offset = name.startsWith("is") ? 2 : 3;
		return name.substring(offset, offset + 1).toLowerCase()
				+ name.substring(offset + 1);
	}

	/**
	 * Since overridden parent annotations are potentially useful, we don't use
	 * standard overriding behaviour
	 *
	 * @throws ClassNotFoundException
	 */
	private Map<JClassType, Set<RegistryLocation>> getRegistryAnnotations(
			TypeOracle typeOracle) throws ClassNotFoundException {
		HashMap<JClassType, Set<RegistryLocation>> results = new HashMap<JClassType, Set<RegistryLocation>>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jct : types) {
			if ((jct.isAnnotationPresent(RegistryLocation.class)
					|| jct.isAnnotationPresent(RegistryLocations.class))
					&& !jct.isAbstract()) {
				Set<RegistryLocation> rls = getClassAnnotations(jct,
						RegistryLocation.class, true);
				Set<RegistryLocations> rlsSet = getClassAnnotations(jct,
						RegistryLocations.class, true);
				for (RegistryLocations rlcs : rlsSet) {
					for (RegistryLocation rl : rlcs.value()) {
						rls.add(rl);
					}
				}
				rls = new LinkedHashSet<RegistryLocation>(rls);
				CollectionFilters.filterInPlace(rls,
						CLIENT_VISIBLE_ANNOTATION_FILTER);
				rls = Registry.filterForRegistryPointUniqueness(rls);
				if (!rls.isEmpty() && !ignore(jct)) {
					results.put(jct, rls);
				}
			}
		}
		return results;
	}

	private List<Annotation> getVisibleAnnotations(HasAnnotations ha,
			List<Class<? extends Annotation>> annotationClasses) {
		List<Annotation> result = new ArrayList<Annotation>();
		for (Class<? extends Annotation> a : annotationClasses) {
			if (ha.isAnnotationPresent(a)) {
				result.add(ha.getAnnotation(a));
			}
		}
		return result;
	}

	private boolean ignore(JClassType jClassType) {
		return ignore(jClassType, null);
	}

	private boolean ignore(JClassType jClassType,
			ReflectionAction reflectionAction) {
		return (jClassType.isAbstract() && jClassType.isEnum() == null)
				|| (jClassType.isInterface() != null) || !jClassType.isPublic()
				|| filter.omitForModule(jClassType, reflectionAction);
	}

	private void writeAnnImpl(Annotation a, Map<Class, String> ann2impl,
			int count, boolean assignment, StringBuffer sb,
			boolean qualifiedClassNames) throws Exception {
		List<Method> declaredMethods = new ArrayList<Method>(
				Arrays.asList(a.getClass().getDeclaredMethods()));
		Collections.sort(declaredMethods, ToStringComparator.INSTANCE);
		String implCN = ann2impl.get(a.annotationType());
		String implSimpleName = implCN.substring(implCN.lastIndexOf(".") + 1);
		if (assignment) {
			sb.append(String.format("%s a%s = ", implSimpleName, count));
		}
		sb.append(String.format("new %s()", implSimpleName));
		for (Method m : declaredMethods) {
			if ("hashCode|toString|equals|annotationType"
					.contains(m.getName())) {
				continue;
			}
			Object annotationValue = m.invoke(a,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			if (annotationValue instanceof String) {
				annotationValue = ((String) annotationValue).replace("\n",
						"\\n");
			}
			Object defaultValue = a.annotationType()
					.getDeclaredMethod(m.getName(), new Class[0])
					.getDefaultValue();
			if (!CommonUtils.equalsWithNullEmptyEquality(annotationValue,
					defaultValue)) {
				sb.append(String.format("._set%s(", m.getName()));
				writeLiteral(annotationValue, m.getReturnType(), sb,
						qualifiedClassNames);
				sb.append(")");
			}
		}
		if (assignment) {
			sb.append(";");
		}
	}

	private void writeAnnotations(TreeLogger logger, GeneratorContext context,
			List<JAnnotationType> jAnns, ClassSourceFileComposerFactory crf,
			boolean initial) throws Exception {
		for (JAnnotationType type : jAnns) {
			Class<? extends Annotation> annClass = forName(type);
			String implementationName = type.getName() + "_Impl";
			ann2impl.put(annClass,
					type.getPackage().getName() + "." + implementationName);
		}
		for (JAnnotationType type : jAnns) {
			Class<? extends Annotation> annClass = forName(type);
			String implementationName = type.getName() + "_Impl";
			String implFQN = type.getPackage().getName() + "."
					+ implementationName;
			crf.addImport(implFQN);
			ClassSourceFileComposerFactory annf = new ClassSourceFileComposerFactory(
					type.getPackage().getName(), implementationName);
			annf.addImport(Annotation.class.getCanonicalName());
			annf.addImport(type.getQualifiedSourceName());
			annf.addImplementedInterface(type.getName());
			List<Method> declaredMethods = new ArrayList<Method>(
					Arrays.asList(annClass.getDeclaredMethods()));
			Collections.sort(declaredMethods, ToStringComparator.INSTANCE);
			StringBuffer constrParams = new StringBuffer();
			boolean first = true;
			for (Method method : declaredMethods) {
				Class<?> returnType = method.getReturnType();
				addImport(annf, returnType);
				addImport(crf, returnType);
			}
			PrintWriter printWriter = context.tryCreate(logger,
					type.getPackage().getName(), implementationName);
			// if calling from a non-initial module, we just want to add imports
			// without rewriting (indeed, we can't...) the annotation impls
			if (printWriter != null) {
				SourceWriter sw = createWriter(annf, printWriter);
				for (Method method : declaredMethods) {
					Class returnType = method.getReturnType();
					String rn = returnType.getSimpleName();
					String mn = method.getName();
					StringBuffer sb = new StringBuffer();
					writeLiteral(method.getDefaultValue(), returnType, sb,
							true);
					sw.println(String.format("private %s %s = %s;", rn, mn,
							sb.toString()));
					sw.println(String.format("public %s %s(){return %s;}", rn,
							mn, mn));
					sw.println(String.format(
							"public %s _set%s(%s %s){this.%s = %s;return this;}",
							implementationName, mn, rn, mn, mn, mn));
					sw.println();
				}
				sw.println();
				sw.println(
						"public Class<? extends Annotation> annotationType() {");
				sw.indentln(String.format("return %s.class;",
						annClass.getSimpleName()));
				sw.println("}");
				sw.println();
				sw.println(String.format("public %s (){}", implementationName));
				sw.outdent();
				sw.println("}");
				commit(context, logger, printWriter);
			}
		}
	}

	private void writeLiteral(Object object, Class declaredType,
			StringBuffer sb, boolean qualifiedClassNames) throws Exception {
		if (object == null) {
			sb.append("null");
			return;
		}
		Class<? extends Object> c = object.getClass();
		if (c.isArray()) {
			String implClassName = ann2impl
					.containsKey(declaredType.getComponentType())
							? ann2impl.get(declaredType.getComponentType())
									+ "[]"
							: qualifiedClassNames ? c.getCanonicalName()
									: c.getSimpleName();
			sb.append(String.format("new %s{", implClassName));
			int length = Array.getLength(object);
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				writeLiteral(Array.get(object, i), c.getComponentType(), sb,
						qualifiedClassNames);
			}
			sb.append("}");
		} else if (declaredType.isAnnotation()) {
			writeAnnImpl((Annotation) object, ann2impl, 0, false, sb,
					qualifiedClassNames);
		} else if (c.equals(Class.class)) {
			sb.append(((Class) object).getCanonicalName() + ".class");
		} else if (c.equals(String.class)) {
			sb.append(String.format("\"%s\"",
					object.toString().replace("\\", "\\\\")));
		} else if (Enum.class.isAssignableFrom(c)) {
			sb.append((qualifiedClassNames ? c.getCanonicalName()
					: c.getSimpleName()) + "." + object.toString());
		} else {
			sb.append(object.toString());
		}
	}

	Class getBoxType(Class c) {
		if (c.isPrimitive()) {
			Map<String, Class> pbm = new HashMap<String, Class>();
			pbm.put("long", Long.class);
			pbm.put("int", Integer.class);
			pbm.put("boolean", Boolean.class);
			if (!pbm.containsKey(c.getName())) {
				throw new WrappedRuntimeException(
						"Unimplemented primitive:" + c.getName(),
						SuggestedAction.NOTIFY_AND_SHUTDOWN);
			}
			return pbm.get(c.getName());
		}
		return c;
	}
}

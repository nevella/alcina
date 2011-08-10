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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.ToStringComparator;

import com.google.gwt.core.client.GWT;
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

@SuppressWarnings("unchecked")
/**
 * Currently, it's a schemozzle - this was originally a standalone generator, so there's a mishmash
 * of usages of JVM vs GWT reflection - 
 * it does, however, work, it's acceptably fast, it can be beautified later
 * 
 * @author Nick Reddel
 */
public class ClientReflectionGenerator extends Generator {
	private String implementationName = ClientReflector.class.getSimpleName()
			+ "_Impl";

	private String packageName = ClientReflector.class.getCanonicalName()
			.substring(0,
					ClientReflector.class.getCanonicalName().lastIndexOf("."));

	private boolean debug = false;

	SourceWriter sw;

	private Map<Class, String> ann2impl = new HashMap<Class, String>();

	private ArrayList<Class<? extends Annotation>> visibleAnnotationClasses;

	public SourceWriter getSw() {
		return this.sw;
	}

	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
//		System.out.println("ClientReflector generation...");
		PrintWriter printWriter = context.tryCreate(logger, packageName,
				implementationName);
		if (printWriter == null) {
			// System.out.println("Reflector generate skipped.");
			return packageName + "." + implementationName;
		}
		long start = System.currentTimeMillis();
		Map<Class, String> ann2impl = new HashMap<Class, String>();
		Map<String, String> simpleNameCheck = new HashMap<String, String>();
		ClassSourceFileComposerFactory crf = null;
		crf = new ClassSourceFileComposerFactory(this.packageName,
				this.implementationName);
		crf.addImport(HashMap.class.getName());
		crf.addImport(Map.class.getName());
		crf.addImport(GWT.class.getName());
		crf.addImport(Registry.class.getName());
		crf.addImport(Annotation.class.getName());
		crf.setSuperclass(ClientReflector.class.getName());
		crf.addImport(ClientBeanReflector.class.getName());
		crf.addImport(ClientPropertyReflector.class.getName());
		crf.addImport(ClientReflector.class.getName());
		crf.addImport(RegistryLocation.class.getName());
		ctLookup.clear();
		// scan for reflectable annotations etc
		try {
			visibleAnnotationClasses = new ArrayList<Class<? extends Annotation>>();
			List<JAnnotationType> jAnns = this.getClientVisibleAnnotations(
					logger, context.getTypeOracle());
			for (JAnnotationType jAnnotationType : jAnns) {
				visibleAnnotationClasses
						.add((Class<? extends Annotation>) Class
								.forName(jAnnotationType
										.getQualifiedBinaryName()));
			}
			writeAnnotations(logger, context, jAnns, crf);
			List<JClassType> beanInfoTypes = this.getBeanInfoTypes(logger,
					context.getTypeOracle(), crf);
			List<JClassType> instantiableTypes = this.getInstantiableTypes(
					logger, context.getTypeOracle(), crf);
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses = getRegistryAnnotations(context
					.getTypeOracle());
			SourceWriter srcW = createWriter(crf, printWriter);
			procDomain(beanInfoTypes, instantiableTypes, srcW,
					gwtRegisteringClasses, implementationName);
//			srcW.println("bruce");
			commit(context, logger, printWriter);
			System.out.println(String.format("Client reflection generation - "
					+ "%s annotations, %s beans, "
					+ "%s instantiable types - %s ms", jAnns.size(),
					beanInfoTypes.size(), instantiableTypes.size(),
					System.currentTimeMillis() - start));
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
		return packageName + "." + implementationName;
	}

	Map<Class, JClassType> ctLookup = new HashMap<Class, JClassType>();

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
			if (ignore(jct)) {
				continue;
			}
			if ((jct.isAnnotationPresent(RegistryLocation.class) || jct
					.isAnnotationPresent(RegistryLocations.class))
					&& !jct.isAbstract()) {
				Set<RegistryLocation> rls = getClassAnnotations(jct,
						RegistryLocation.class);
				Set<RegistryLocations> rlsSet = getClassAnnotations(jct,
						RegistryLocations.class);
				for (RegistryLocations rlcs : rlsSet) {
					for (RegistryLocation rl : rlcs.value()) {
						rls.add(rl);
					}
				}
				results.put(jct, new LinkedHashSet<RegistryLocation>(rls));
			}
		}
		return results;
	}

	private <T> Set<T> getClassAnnotations(JClassType jct,
			Class<T> annotationType) {
		return (Set) getClassAnnotations(jct,
				(List) Collections.singletonList(annotationType));
	}

	private boolean ignore(JClassType jClassType) {
		return jClassType.isAbstract() || (jClassType.isInterface() != null)
				|| !jClassType.isPublic();
	}

	private List<JClassType> getInstantiableTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JClassType> results = new ArrayList<JClassType>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jClassType : types) {
			if (jClassType.isAnnotationPresent(ClientInstantiable.class)
					&& !ignore(jClassType)) {
				results.add(jClassType);
				crf.addImport(jClassType.getQualifiedSourceName());
			}
		}
		return results;
	}

	private List<JClassType> getBeanInfoTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JClassType> results = new ArrayList<JClassType>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jClassType : types) {
			if (jClassType
					.isAnnotationPresent(cc.alcina.framework.common.client.logic.reflection.BeanInfo.class)
					&& !ignore(jClassType)) {
				results.add(jClassType);
				crf.addImport(jClassType.getQualifiedSourceName());
			}
		}
		return results;
	}

	private void addImport(ClassSourceFileComposerFactory factory, Class<?> type) {
		if (!type.isPrimitive()) {
			factory.addImport(type.getCanonicalName().replace("[]", ""));
		}
	}

	private void writeAnnotations(TreeLogger logger, GeneratorContext context,
			List<JAnnotationType> jAnns, ClassSourceFileComposerFactory crf)
			throws Exception {
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
			List<String> constrLines = new ArrayList<String>();
			ann2impl.put(annClass, type.getPackage().getName() + "."
					+ implementationName);
			boolean first = true;
			for (Method method : declaredMethods) {
				Class<?> returnType = method.getReturnType();
				addImport(annf, returnType);
				addImport(crf, returnType);
			}
			PrintWriter printWriter = context.tryCreate(logger, packageName,
					implementationName);
			SourceWriter sw = createWriter(annf, printWriter);
			for (Method method : declaredMethods) {
				Class<?> returnType = method.getReturnType();
				String rn = returnType.getSimpleName();
				String mn = method.getName();
				sw.println(String.format("private %s %s;", rn, mn));
				sw.println(String.format("public %s %s(){return %s;}", rn, mn,
						mn));
				if (!first) {
					constrParams.append(", ");
				}
				constrParams.append(rn + " " + mn);
				constrLines.add(String.format("this.%s = %s;", mn, mn));
				first = false;
				sw.println();
			}
			sw.println();
			sw.println("public Class<? extends Annotation> annotationType() {");
			sw.indentln(String.format("return %s.class;",
					annClass.getSimpleName()));
			sw.println("}");
			sw.println();
			sw.println(String.format("public %s (%s){", implementationName,
					constrParams));
			sw.indent();
			for (String s : constrLines) {
				sw.println(s);
			}
			sw.outdent();
			sw.println("}");
			sw.outdent();
			sw.println("}");
			commit(context, logger, printWriter);
		}
	}

	private Class forName(JType type) throws ClassNotFoundException {
		String name = type.getQualifiedBinaryName();
		return Class.forName(name);
	}

	private Map<PrintWriter, BiWriter> wrappedWriters = new HashMap<PrintWriter, BiWriter>();

	private SourceWriter createWriter(ClassSourceFileComposerFactory factory,
			PrintWriter contextWriter) {
		PrintWriter writer = contextWriter;
		if (debug) {
			writer = new BiWriter(writer);
			wrappedWriters.put(contextWriter, (BiWriter) writer);
		}
		return factory.createSourceWriter(writer);
	}

	private void commit(GeneratorContext context, TreeLogger logger,
			PrintWriter printWriter) {
		context.commit(logger, printWriter);
		if (wrappedWriters.containsKey(printWriter)) {
			System.out.println(wrappedWriters.get(printWriter)
					.getStringWriter().toString());
		}
	}

	private List<JAnnotationType> getClientVisibleAnnotations(
			TreeLogger logger, TypeOracle oracle) {
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

	public void procDomain(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes, SourceWriter sw,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses,
			String implName) throws Exception {
		sw.indent();
		sw.println("Map<Class, ClientBeanReflector> gwbiMap = new HashMap<Class, ClientBeanReflector>();");
		sw.println(String.format("public %s() {", implName));
		sw.indent();
		sw.println("super();");
		sw.println("init();");
		sw.println("ClientReflector.register(this);");
		sw.outdent();
		sw.println("}");
		sw.println("public ClientBeanReflector beanInfoForClass(Class clazz) {");
		sw.indent();
		sw.println("return gwbiMap.get(clazz);");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println();
		sw.println("@Override");
		sw.println("public <T> T newInstance(Class<T> clazz, long objectId, long localId) {");
		sw.indent();
		for (JClassType c : beanInfoTypes) {
			sw.println(String
					.format("if (clazz.equals(%s.class)){return (T)GWT.create(%s.class);} ",
							c.getSimpleSourceName(), c.getSimpleSourceName()));
		}
		for (JClassType c : instantiableTypes) {
			if (c.isEnum() != null) {
				continue;
			}
			sw.println(String
					.format("if (clazz.equals(%s.class)){return (T)GWT.create(%s.class);} ",
							c.getSimpleSourceName(), c.getSimpleSourceName()));
		}
		sw.println("if (child!=null){return child.newInstance(clazz,objectId,localId);}");
		sw.println("throw new RuntimeException(\"Class \"+clazz+\" not reflect-instantiable\");");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println();
		List<String> methodNames = new ArrayList<String>();
		int methodCount = 0;
		for (JClassType jct : beanInfoTypes) {
			String methodName = "initClass" + (methodCount++);
			methodNames.add(methodName);
			sw.println(String.format("private void %s(){", methodName));
			sw.indent();
			sw.println("Map<String,ClientPropertyReflector> propertyReflectors = new HashMap<String,ClientPropertyReflector>();");
			for (JMethod method : getPropertyGetters(jct)) {
				String propertyName = getPropertyNameForReadMethod(method);
				if (propertyName.equals("class")
						|| propertyName
								.equals("propertyChangeListeners")) {
					continue;
				}
				Collection<Annotation> annotations = getSuperclassAnnotationsForMethod(method);
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
					if (a.annotationType() == Omit.class) {
						ignore = true;
					}
					if (!a.annotationType().isAnnotationPresent(
							ClientVisible.class)
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
				sw.println(String.format("ClientPropertyReflector reflector = "
						+ "new ClientPropertyReflector(\"%s\",%s.class,"
						+ " new Annotation[]{%s}) ;", propertyName, method
						.getReturnType().getQualifiedSourceName(), annArray));
				sw.println("propertyReflectors.put(reflector.getPropertyName(), reflector);");
				sw.outdent();
				sw.println("}");
			}
			int aCount = 0;
			String annArray = "";
			for (Annotation a : getClassAnnotations(jct,
					visibleAnnotationClasses)) {
				if (aCount++ != 0) {
					annArray += ", ";
				}
				String annImpl = getAnnImpl(a, ann2impl, aCount);
				annArray += "a" + aCount;
				sw.println(annImpl);
			}
			sw.println(String
					.format("ClientBeanReflector beanReflector = new ClientBeanReflector("
							+ "%s.class,new Annotation[]{%s},propertyReflectors);",
							jct.getQualifiedSourceName(), annArray));
			sw.println("gwbiMap.put(beanReflector.getBeanClass(),beanReflector );");
			sw.outdent();
			sw.println("}");
			sw.println("");
		}
		sw.println("private void init() {");
		sw.indent();
		Set<JClassType> qt = new HashSet<JClassType>();
		qt.addAll(instantiableTypes);
		qt.addAll(beanInfoTypes);
		for (JClassType t : qt) {
			sw.println(String.format("forNameMap.put(\"%s\",%s.class);",
					t.getQualifiedBinaryName(), t.getQualifiedSourceName()));
		}
		for (String methodName : methodNames) {
			sw.println(String.format("%s();", methodName));
		}
		sw.println("");
		sw.println("//init registry");
		sw.println("");
		for (JClassType clazz : gwtRegisteringClasses.keySet()) {
			for (RegistryLocation l : gwtRegisteringClasses.get(clazz)) {
				StringBuffer sb = new StringBuffer();
				writeAnnImpl(l, ann2impl, 0, false, sb);
				sw.println(String.format(
						"Registry.get().register(%s.class,%s);",
						clazz.getQualifiedSourceName(), sb));
			}
		}
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
	}

	private String getPropertyNameForReadMethod(JMethod method) {
		String name = method.getName();
		int offset = name.startsWith("is") ? 2 : 3;
		return name.substring(offset, offset + 1).toLowerCase()
				+ name.substring(offset + 1);
	}

	private List<JMethod> getPropertyGetters(JClassType jct) {
		ArrayList<JMethod> methods = new ArrayList<JMethod>();
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
		return methods;
	}

	private String getAnnImpl(Annotation a, Map<Class, String> ann2impl,
			int count) throws Exception {
		StringBuffer sb = new StringBuffer();
		writeAnnImpl(a, ann2impl, count, true, sb);
		return sb.toString();
	}

	private void writeAnnImpl(Annotation a, Map<Class, String> ann2impl,
			int count, boolean assignment, StringBuffer sb) throws Exception {
		List<Method> declaredMethods = new ArrayList<Method>(Arrays.asList(a
				.getClass().getDeclaredMethods()));
		Collections.sort(declaredMethods, ToStringComparator.INSTANCE);
		String implCN = ann2impl.get(a.annotationType());
		String implSimpleName = implCN.substring(implCN.lastIndexOf(".") + 1);
		if (assignment) {
			sb.append(String.format("%s a%s = ", implSimpleName, count));
		}
		sb.append(String.format("new %s(", implSimpleName));
		// TODO Auto-generated method stub
		int pc = 0;
		for (Method m : declaredMethods) {
			if ("hashCode|toString|equals|annotationType".contains(m.getName())) {
				continue;
			}
			if (pc++ != 0) {
				sb.append(", ");
			}
			writeLiteral(m.invoke(a, CommonUtils.EMPTY_OBJECT_ARRAY),
					m.getReturnType(), sb);
		}
		sb.append(")");
		if (assignment) {
			sb.append(";");
		}
	}

	private void writeLiteral(Object object, Class declaredType, StringBuffer sb)
			throws Exception {
		Class<? extends Object> c = object.getClass();
		if (c.isArray()) {
			String implClassName = ann2impl.containsKey(declaredType
					.getComponentType()) ? ann2impl.get(declaredType
					.getComponentType()) + "[]" : c.getSimpleName();
			sb.append(String.format("new %s{", implClassName));
			int length = Array.getLength(object);
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				writeLiteral(Array.get(object, i), c.getComponentType(), sb);
			}
			sb.append("}");
		} else if (declaredType.isAnnotation()) {
			writeAnnImpl((Annotation) object, ann2impl, 0, false, sb);
		} else if (c.equals(Class.class)) {
			sb.append(((Class) object).getCanonicalName() + ".class");
		} else if (c.equals(String.class)) {
			sb.append(String.format("\"%s\"",
					object.toString().replace("\\", "\\\\")));
		} else if (Enum.class.isAssignableFrom(c)) {
			sb.append(c.getSimpleName() + "." + object.toString());
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
				throw new WrappedRuntimeException("Unimplemented primitive:"
						+ c.getName(), SuggestedAction.NOTIFY_AND_SHUTDOWN);
			}
			return pbm.get(c.getName());
		}
		return c;
	}

	private HashMap<JMethod, Set<Annotation>> superMethodAnnotationMap = new HashMap<JMethod, Set<Annotation>>();

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

	private LookupMapToMap<Set<Annotation>> superAnnotationMap = new LookupMapToMap<Set<Annotation>>(
			2);

	public Set<Annotation> getClassAnnotations(JClassType clazz,
			List<Class<? extends Annotation>> annotationClasses) {
		if (superAnnotationMap.containsKey(clazz, annotationClasses)) {
			return superAnnotationMap.get(clazz, annotationClasses);
		}
		Map<Class, Annotation> uniqueMap = new HashMap<Class, Annotation>();
		Set<? extends JClassType> flattenedSupertypeHierarchy = clazz
				.getFlattenedSupertypeHierarchy();
		for (JClassType jct : flattenedSupertypeHierarchy) {
			try {
				for (Annotation a : getVisibleAnnotations(jct,
						annotationClasses)) {
					if (!uniqueMap.containsKey(a.annotationType())) {
						uniqueMap.put(a.annotationType(), a);
					}
				}
			} catch (Exception e) {
			}
		}
		HashSet values = new HashSet(uniqueMap.values());
		superAnnotationMap.put(clazz, annotationClasses, values);
		return values;
	}
}

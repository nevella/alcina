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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
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
import cc.alcina.framework.common.client.util.ToStringComparator;
import cc.alcina.framework.entity.util.AnnotationUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.client.beans.annotations.Omit;

@SuppressWarnings("unchecked")
/**
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

	public SourceWriter getSw() {
		return this.sw;
	}

	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		// System.out.println("ClientReflector generation...");
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
		// scan for reflectable annotations etc
		try {
			List<JAnnotationType> jAnns = this.getClientVisibleAnnotations(
					logger, context.getTypeOracle());
			writeAnnotations(logger, context, jAnns, crf);
			List<JType> beanInfoTypes = this.getBeanInfoTypes(logger, context
					.getTypeOracle(), crf);
			List<JType> instantiableTypes = this.getInstantiableTypes(logger,
					context.getTypeOracle(), crf);
			Map<Class, Set<RegistryLocation>> gwtRegisteringClasses = getRegistryAnnotations(context
					.getTypeOracle());
			SourceWriter srcW = createWriter(crf, printWriter);
			procDomain(toJavaClasses(beanInfoTypes),
					toJavaClasses(instantiableTypes), srcW,
					gwtRegisteringClasses, implementationName);
			commit(context, logger, printWriter);
			System.out.println(String.format("Client reflection generation - "
					+ "%s annotations, %s beans, "
					+ "%s instantiable types - %s ms", jAnns.size(),
					beanInfoTypes.size(), instantiableTypes.size(), System
							.currentTimeMillis()
							- start));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return packageName + "." + implementationName;
	}

	/**
	 * Since overridden parent annotations are potentially useful, we don't use
	 * standard overriding behaviour
	 * 
	 * @throws ClassNotFoundException
	 */
	private Map<Class, Set<RegistryLocation>> getRegistryAnnotations(
			TypeOracle typeOracle) throws ClassNotFoundException {
		HashMap<Class, Set<RegistryLocation>> results = new HashMap<Class, Set<RegistryLocation>>();
		JClassType[] types = typeOracle.getTypes();
		for (JClassType jClassType : types) {
			if (ignore(jClassType)) {
				continue;
			}
			if ((jClassType.isAnnotationPresent(RegistryLocation.class) || jClassType
					.isAnnotationPresent(RegistryLocations.class))
					&& !jClassType.isAbstract()) {
				Class c = forName(jClassType);
				Set<Annotation> sca = AnnotationUtils
						.getSuperclassAnnotations(c);
				Set<RegistryLocation> rls = AnnotationUtils.filterAnnotations(
						sca, RegistryLocation.class);
				Set<RegistryLocations> rlsSet = AnnotationUtils
						.filterAnnotations(sca, RegistryLocations.class);
				for (RegistryLocations rlcs : rlsSet) {
					for (RegistryLocation rl : rlcs.value()) {
						rls.add(rl);
					}
				}
				for (RegistryLocation rl : rls) {
					if (!results.containsKey(c)) {
						results.put(c, new LinkedHashSet<RegistryLocation>());
					}
					results.get(c).add(rl);
				}
			}
		}
		return results;
	}

	private boolean ignore(JClassType jClassType) {
		return jClassType.isAbstract() || (jClassType.isInterface() != null)
				|| !jClassType.isPublic();
	}

	private List<Class> toJavaClasses(List<JType> instantiableTypes)
			throws Exception {
		List<Class> results = new ArrayList<Class>();
		for (JType jType : instantiableTypes) {
			results.add(forName(jType));
		}
		return results;
	}

	private List<JType> getInstantiableTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JType> results = new ArrayList<JType>();
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

	private List<JType> getBeanInfoTypes(TreeLogger logger,
			TypeOracle typeOracle, ClassSourceFileComposerFactory crf) {
		List<JType> results = new ArrayList<JType>();
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
			List<Method> declaredMethods = new ArrayList<Method>(Arrays
					.asList(annClass.getDeclaredMethods()));
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
			sw.indentln(String.format("return %s.class;", annClass
					.getSimpleName()));
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

	private  Class forName(JType type) throws ClassNotFoundException {
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

	public void procDomain(List<Class> beanInfoClasses,
			List<Class> instantiableClasses, SourceWriter sw,
			Map<Class, Set<RegistryLocation>> gwtRegisteringClasses,
			String implName) throws Exception {
		sw.indent();
		sw
				.println("Map<Class, ClientBeanReflector> gwbiMap = new HashMap<Class, ClientBeanReflector>();");
		sw.println(String.format("public %s() {", implName));
		sw.indent();
		sw.println("super();");
		sw.println("init();");
		sw.println("ClientReflector.register(this);");
		sw.outdent();
		sw.println("}");
		sw
				.println("public ClientBeanReflector beanInfoForClass(Class clazz) {");
		sw.indent();
		sw.println("return gwbiMap.get(clazz);");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println();
		sw.println("@Override");
		sw.println("public <T> T newInstance(Class<T> clazz, long localId) {");
		sw.indent();
		for (Class c : beanInfoClasses) {
			sw
					.println(String
							.format(
									"if (clazz.equals(%s.class)){return (T)GWT.create(%s.class);} ",
									c.getSimpleName(), c.getSimpleName()));
		}
		for (Class c : instantiableClasses) {
			if (c.isEnum()) {
				continue;
			}
			sw
					.println(String
							.format(
									"if (clazz.equals(%s.class)){return (T)GWT.create(%s.class);} ",
									c.getSimpleName(), c.getSimpleName()));
		}
		sw
				.println("if (child!=null){return child.newInstance(clazz,localId);}");
		sw
				.println("throw new RuntimeException(\"Class \"+clazz+\" not reflect-instantiable\");");
		sw.outdent();
		sw.println("}");
		sw.println();
		sw.println();
		List<String> methodNames = new ArrayList<String>();
		int methodCount = 0;
		for (Class c : beanInfoClasses) {
			String methodName = "initClass" + (methodCount++);
			methodNames.add(methodName);
			sw.println(String.format("private void %s(){", methodName));
			sw.indent();
			sw
					.println("Map<String,ClientPropertyReflector> propertyReflectors = new HashMap<String,ClientPropertyReflector>();");
			BeanInfo beanInfo = Introspector.getBeanInfo(c);
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals("class")
						|| pd.getName().equals("propertyChangeListeners")) {
					continue;
				}
				// if (pd.getName().equals("startLocation")){
				// int k=3;
				// }
				Method m = pd.getReadMethod();
				Collection<Annotation> annotations = AnnotationUtils
						.getSuperclassAnnotationsForMethod(m);
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
						+ " new Annotation[]{%s}) ;", pd.getName(), pd
						.getPropertyType().getName().replace("$", "."),
						annArray));
				sw
						.println("propertyReflectors.put(reflector.getPropertyName(), reflector);");
				sw.outdent();
				sw.println("}");
			}
			int aCount = 0;
			String annArray = "";
			for (Annotation a : c.getAnnotations()) {
				if (!a.annotationType()
						.isAnnotationPresent(ClientVisible.class)) {
					continue;
				}
				if (aCount++ != 0) {
					annArray += ", ";
				}
				String annImpl = getAnnImpl(a, ann2impl, aCount);
				annArray += "a" + aCount;
				sw.println(annImpl);
			}
			sw
					.println(String
							.format(
									"ClientBeanReflector beanReflector = new ClientBeanReflector("
											+ "%s.class,new Annotation[]{%s},propertyReflectors);",
									c.getCanonicalName(), annArray));
			sw
					.println("gwbiMap.put(beanReflector.getBeanClass(),beanReflector );");
			sw.outdent();
			sw.println("}");
			sw.println("");
		}
		sw.println("private void init() {");
		sw.indent();
		Set<Class> qt = new HashSet<Class>();
		qt.addAll(instantiableClasses);
		for (Class c : instantiableClasses) {
			sw.println(String.format("forNameMap.put(\"%s\",%s.class);", c
					.getName(), c.getName().replace("$", ".")));
		}
		for (Class c : beanInfoClasses) {
			if (!qt.contains(c)) {
				sw.println(String.format("forNameMap.put(\"%s\",%s.class);", c
						.getName(), c.getName().replace("$", ".")));
			}
		}
		for (String methodName : methodNames) {
			sw.println(String.format("%s();", methodName));
		}
		sw.println("");
		sw.println("//init registry");
		sw.println("");
		for (Class clazz : gwtRegisteringClasses.keySet()) {
			for (RegistryLocation l : gwtRegisteringClasses.get(clazz)) {
				StringBuffer sb = new StringBuffer();
				writeAnnImpl(l, ann2impl, 0, false, sb);
				sw.println(String.format(
						"Registry.get().register(%s.class,%s);", clazz
								.getCanonicalName(), sb));
			}
		}
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
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
			writeLiteral(m.invoke(a, CommonUtils.EMPTY_OBJECT_ARRAY), m
					.getReturnType(), sb);
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
					.getComponentType())
					+ "[]" : c.getSimpleName();
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
			sb.append(String.format("\"%s\"", object.toString().replace("\\",
					"\\\\")));
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
}

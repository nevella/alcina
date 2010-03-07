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
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ToStringComparator;
import cc.alcina.framework.entity.gwtsynth.GwtReflectionScanner.ScanInfo;
import cc.alcina.framework.entity.util.AnnotationUtils;

import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.rebind.beans.IntrospectorIgnore;
@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class GwtReflectionGenerator {
	SourceWriter sw;

	private Map<Class, String> ann2impl;

	public SourceWriter getSw() {
		return this.sw;
	}

	public void generateAnnotationImplementation(
			Class<? extends Annotation> ann, SourceWriter sw2, String implName) {
		this.sw = sw2;
		List<Method> declaredMethods = new ArrayList<Method>(Arrays.asList(ann
				.getDeclaredMethods()));
		Collections.sort(declaredMethods,
				ToStringComparator.INSTANCE);
		StringBuffer constrParams = new StringBuffer();
		List<String> constrLines = new ArrayList<String>();
		boolean first = true;
		for (Method method : declaredMethods) {
			Class<?> returnType = method.getReturnType();
			String rn = returnType.getSimpleName();
			String mn = method.getName();
			sw.println(String.format("private %s %s;", rn, mn));
			sw.println(String.format("public %s %s(){return %s;}", rn, mn, mn));
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
		sw.indentln(String.format("return %s.class;", ann.getSimpleName()));
		sw.println("}");
		sw.println();
		sw.println(String.format("public %s (%s){", implName, constrParams));
		sw.indent();
		for (String s : constrLines) {
			sw.println(s);
		}
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
	}

	public void procDomain(List<Class> domainClasses,
			List<Class> instantiableClasses, SourceWriter sw,
			Map<Class, String> ann2impl,
			Map<Class, Set<RegistryLocation>> gwtRegisteringClasses,
			boolean childReflector, ScanInfo info) throws Exception {
		this.ann2impl = ann2impl;
		sw.indent();
		sw
				.println("Map<Class, ClientBeanReflector> gwbiMap = new HashMap<Class, ClientBeanReflector>();");
		sw.println(String.format("public %s() {", info
				.getDomainReflectorClassName()));
		sw.indent();
		sw.println("super();");
		sw.println("init();");
		if (childReflector) {
		} else {
			sw.println("ClientReflector.register(this);");
		}
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
		sw.println("public <T> T newInstance(Class<T> clazz, long localId) {");
		sw.indent();
		for (Class c : domainClasses) {
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
		for (Class c : domainClasses) {
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
					if (a.annotationType() == IntrospectorIgnore.class) {
						ignore = true;
					}
				}
				if (ignore) {
					continue;
				}
				sw.println("{");
				sw.indent();
				for (Annotation a : annotations) {
					if (a.annotationType() == IntrospectorIgnore.class) {
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
				if (!a.annotationType().isAnnotationPresent(
						ClientVisible.class)) {
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
		for (Class c : domainClasses) {
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
		Collections.sort(declaredMethods,
				ToStringComparator.INSTANCE);
		String implCN = ann2impl.get(a.annotationType());
		Class<?> impl = null;
		try {
			impl = Class.forName(implCN);
		} catch (Exception e) {
			System.err.println(a.getClass().getName());
			e.printStackTrace();
		}
		if (assignment) {
			sb.append(String.format("%s a%s = ", impl.getSimpleName(), count));
		}
		sb.append(String.format("new %s(", impl.getSimpleName()));
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

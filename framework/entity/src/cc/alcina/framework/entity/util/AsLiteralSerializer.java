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
package cc.alcina.framework.entity.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

/**
 * 
 * @author Nick Reddel
 */
public class AsLiteralSerializer {
	public static void main(String[] args) {
	}

	private ClassSourceFileComposerFactory composerFactory;

	@SuppressWarnings("unused")
	private final String className;

	private int methodLengthCounter;

	private int callCounter = 0;

	private IdentityHashMap<Object, OutputInstantiation> reached = new IdentityHashMap<Object, OutputInstantiation>();

	private List<OutputAssignment> assignments = new ArrayList<OutputAssignment>();

	private Map<OutputInstantiation, List<OutputInstantiation>> addToCollnMap = new HashMap<OutputInstantiation, List<OutputInstantiation>>();

	private Map<OutputInstantiation, List<OutputInstantiation>> addToMapMap = new HashMap<OutputInstantiation, List<OutputInstantiation>>();

	private Set<Class> reachedClasses = new LinkedHashSet<Class>();

	private int idCounter = 1;

	Map<Class, PropertyDescriptor[]> propertyDescriptorsPerClass = new HashMap<Class, PropertyDescriptor[]>();

	public AsLiteralSerializer(String packageName, String className) {
		this.className = className;
		composerFactory = new ClassSourceFileComposerFactory(packageName,
				className);
	}

	public String generate(Object source) throws Exception {
		// traverse
		traverse(source, null);
		reachedClasses.remove(null);
		for (Class c : reachedClasses) {
			if (isEnumSubClass(c)) {
				continue;
			}
			composerFactory.addImport(c.getName().replace("$", "."));
		}
		StringWriter stringWriter = new StringWriter();
		SourceWriter sw = composerFactory
				.createSourceWriter(new PrintWriter(stringWriter));
		sw.indent();
		ArrayList<OutputInstantiation> insts = new ArrayList<OutputInstantiation>(
				reached.values());
		Collections.sort(insts);
		for (OutputInstantiation inst : insts) {
			Class<? extends Object> valueClass = inst.value.getClass();
			String className = getClassName(valueClass);
			if (isEnumExt(valueClass) || inst.value instanceof Class) {
				sw.println(String.format("%s %s;", className,
						getObjLitRef(inst), getLiteralValue(inst.value)));
			} else {
				sw.println(
						String.format("%s %s;", className, getObjLitRef(inst),
								className, getLiteralValue(inst.value)));
			}
		}
		StringBuffer mainCall = new StringBuffer();
		newCall(mainCall, sw, false);
		for (OutputInstantiation inst : insts) {
			Class<? extends Object> valueClass = inst.value.getClass();
			String className = getClassName(valueClass);
			String add = null;
			if (isEnumExt(valueClass) || inst.value instanceof Class) {
				add = (String.format(" %s= %s;", getObjLitRef(inst),
						getLiteralValue(inst.value)));
			} else {
				add = (String.format(" %s= new %s (%s);", getObjLitRef(inst),
						className, getLiteralValue(inst.value)));
			}
			sw.println(add);
			methodLengthCounter += add.length() + 1;
			if (methodLengthCounter > 20000) {
				newCall(mainCall, sw, true);
			}
		}
		for (OutputAssignment assign : assignments) {
			String assignLit = String.format("%s.%s(%s);",
					getObjLitRef(assign.src),
					assign.pd.getWriteMethod().getName(),
					getObjLitRef(assign.target));
			sw.println(assignLit);
			methodLengthCounter += assignLit.length() + 1;
			if (methodLengthCounter > 20000) {
				newCall(mainCall, sw, true);
			}
		}
		for (OutputInstantiation inst : addToCollnMap.keySet()) {
			List<OutputInstantiation> elts = addToCollnMap.get(inst);
			for (OutputInstantiation elt : elts) {
				String add = String.format("%s.add(%s);", getObjLitRef(inst),
						getObjLitRef(elt));
				sw.println(add);
				methodLengthCounter += add.length() + 1;
				if (methodLengthCounter > 20000) {
					newCall(mainCall, sw, true);
				}
			}
		}
		for (OutputInstantiation inst : addToMapMap.keySet()) {
			List<OutputInstantiation> elts = addToMapMap.get(inst);
			Iterator<OutputInstantiation> itr = elts.iterator();
			for (; itr.hasNext();) {
				OutputInstantiation key = itr.next();
				OutputInstantiation value = itr.next();
				String add = String.format("%s.put(%s,%s);", getObjLitRef(inst),
						getObjLitRef(key), getObjLitRef(value));
				sw.println(add);
				methodLengthCounter += add.length() + 1;
				if (methodLengthCounter > 20000) {
					newCall(mainCall, sw, true);
				}
			}
		}
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		sw.println(String.format("public %s generate() {",
				source.getClass().getSimpleName()));
		sw.indent();
		sw.println(mainCall.toString());
		sw.println("return obj_1;");
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		return stringWriter.toString();
	}

	private String getClassName(Class<? extends Object> clazz) {
		if (isEnumSubClass(clazz)) {
			clazz = clazz.getSuperclass();
		}
		return clazz.getSimpleName();
	}

	private Class getEnumExt(Class c) {
		if (c.getSuperclass() != null
				&& c.getSuperclass().getSuperclass() == java.lang.Enum.class) {
			return c.getSuperclass();
		}
		return null;
	}

	private Object getLiteralValue(Object value) {
		if (value == null || !isSimple(value)) {
			return "";
		}
		if (value instanceof Enum) {
			return getClassName(value.getClass()) + "." + value;
		}
		if (value instanceof Date) {
			Date d = (Date) value;
			return d.getTime() + "L";
		}
		if (value instanceof String) {
			return "\"" + ((String) value).replace("\\", "\\\\")
					.replace("\"", "\\\"").replace("\n", "\\n")
					.replace("\r", "\\r").replace("\t", "\\t") + "\"";
		}
		if (value instanceof Character) {
			return "'" + value + "'";
		}
		if (value instanceof Class) {
			return ((Class) value).getSimpleName() + "." + "class";
		}
		if (value instanceof Long || value.getClass() == long.class) {
			return value + "L";
		}
		return value;
	}

	private String getObjLitRef(OutputInstantiation inst) {
		if (inst == null) {
			return "null";
		}
		return "obj_" + inst.id;
	}

	private PropertyDescriptor[] getPropertyDescriptorsForClassProperties(
			Object cloned) throws Exception {
		Class<? extends Object> clazz = cloned.getClass();
		if (!propertyDescriptorsPerClass.containsKey(clazz)) {
			List<PropertyDescriptor> allPropertyDescriptors = new ArrayList<PropertyDescriptor>();
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
					allPropertyDescriptors.add(pd);
				}
			}
			propertyDescriptorsPerClass.put(clazz,
					(PropertyDescriptor[]) allPropertyDescriptors.toArray(
							new PropertyDescriptor[allPropertyDescriptors
									.size()]));
		}
		return propertyDescriptorsPerClass.get(clazz);
	}

	private boolean isEnumExt(Class c) {
		return c.getSuperclass() == java.lang.Enum.class
				|| (c.getSuperclass() != null && c.getSuperclass()
						.getSuperclass() == java.lang.Enum.class);
	}

	private boolean isEnumSubClass(Class c) {
		return (c.getSuperclass() != null && !c.isEnum()
				&& c.getSuperclass().getSuperclass() == java.lang.Enum.class);
	}

	private boolean isSimple(Object source) {
		Class c = source.getClass();
		return (c.isPrimitive() || c == String.class || c == Boolean.class
				|| c == Character.class || isEnumExt(c) || c == Class.class
				|| (source instanceof Number) || (source instanceof Date));
	}

	private void newCall(StringBuffer mainCall, SourceWriter sw,
			boolean close) {
		if (close) {
			sw.outdent();
			sw.println("}");
			sw.outdent();
			sw.println("}");
		}
		sw.println(String.format("private class Generate_%s {", ++callCounter));
		sw.indent();
		sw.println("private void run() {");
		sw.indent();
		mainCall.append(String.format("new Generate_%s().run();", callCounter));
		methodLengthCounter = 0;
	}

	public OutputInstantiation traverse(Object source,
			GraphProjectionContext context) throws Exception {
		if (source == null) {
			return null;
		}
		if (reached.containsKey(source)) {
			return reached.get(source);
		}
		OutputInstantiation instance = new OutputInstantiation(idCounter++,
				source);
		reached.put(source, instance);
		Class c = source.getClass();
		reachedClasses.add(c);
		reachedClasses.add(getEnumExt(c));
		if (source instanceof Class) {
			reachedClasses.add((Class) source);
			reachedClasses.add(getEnumExt((Class) source));
		}
		if (isSimple(source)) {
			return instance;
		}
		PropertyDescriptor[] propertyDescriptors = getPropertyDescriptorsForClassProperties(
				source);
		Object template = source.getClass().getDeclaredConstructor()
				.newInstance();
		for (PropertyDescriptor pd : propertyDescriptors) {
			Object tgt = pd.getReadMethod().invoke(source,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			Object templateTgt = pd.getReadMethod().invoke(template,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			if (tgt != templateTgt) {
				assignments.add(new OutputAssignment(instance, pd,
						traverse(tgt, null)));
			}
		}
		if (source instanceof Collection) {
			Collection colln = (Collection) source;
			ArrayList<OutputInstantiation> elts = new ArrayList<OutputInstantiation>();
			addToCollnMap.put(instance, elts);
			for (Object object : colln) {
				elts.add(traverse(object, null));
			}
		}
		if (source instanceof Map) {
			Map map = (Map) source;
			ArrayList<OutputInstantiation> elts = new ArrayList<OutputInstantiation>();
			addToMapMap.put(instance, elts);
			for (Object object : map.keySet()) {
				elts.add(traverse(object, null));
				elts.add(traverse(map.get(object), null));
			}
		}
		return instance;
	}

	private static class OutputAssignment {
		OutputInstantiation src;

		PropertyDescriptor pd;

		OutputInstantiation target;

		public OutputAssignment(OutputInstantiation src,
				PropertyDescriptor setPropertyDescriptor,
				OutputInstantiation target) {
			super();
			this.src = src;
			this.pd = setPropertyDescriptor;
			this.target = target;
		}
	}

	static class OutputInstantiation
			implements Comparable<OutputInstantiation> {
		Integer id;

		Object value;

		public OutputInstantiation(Integer id, Object value) {
			super();
			this.id = id;
			this.value = value;
		}

		public int compareTo(OutputInstantiation o) {
			return id.compareTo(o.id);
		}
	}
}

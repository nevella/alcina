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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.GraphCloner.ClassFieldPair;

import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class AsLiteralSerializer {
	private ClassSourceFileComposerFactory composerFactory;

	@SuppressWarnings("unused")
	private final String className;

	private int methodLengthCounter;

	public AsLiteralSerializer(String packageName, String className) {
		this.className = className;
		composerFactory = new ClassSourceFileComposerFactory(packageName,
				className);
	}

	public String generate(Object source) throws Exception {
		// traverse
		traverse(source, null);
		for (Class c : reachedClasses) {
			composerFactory.addImport(c.getName().replace("$", "."));
		}
		StringWriter stringWriter = new StringWriter();
		SourceWriter sw = composerFactory.createSourceWriter(new PrintWriter(
				stringWriter));
		sw.indent();
		ArrayList<OutputInstantiation> insts = new ArrayList<OutputInstantiation>(
				reached.values());
		Collections.sort(insts);
		for (OutputInstantiation inst : insts) {
			String className = getClassName(inst.value.getClass());
			if (inst.value instanceof Enum || inst.value instanceof Class) {
				sw.println(String.format("%s %s= %s;", className,
						getObjLitRef(inst), getLiteralValue(inst.value)));
			} else {
				sw.println(String.format("%s %s= new %s (%s);", className,
						getObjLitRef(inst), className,
						getLiteralValue(inst.value)));
			}
		}
		StringBuffer mainCall = new StringBuffer();
		newCall(mainCall, sw, false);
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
		for (OutputAssignment assign : assignments) {
			String assignLit = String.format("%s.%s(%s);", getObjLitRef(assign.src),
					assign.pd.getWriteMethod().getName(),
					getObjLitRef(assign.target));
			sw.println(assignLit);
			methodLengthCounter += assignLit.length() + 1;
			if (methodLengthCounter > 20000) {
				newCall(mainCall, sw, true);
			}
		}
		sw.outdent();
		sw.println("}");
		sw.println(String.format("public %s generate() {", source.getClass()
				.getSimpleName()));
		sw.indent();
		sw.println(mainCall.toString());
		sw.println("return obj_1;");
		sw.outdent();
		sw.println("}");
		sw.outdent();
		sw.println("}");
		return stringWriter.toString();
	}

	private int callCounter = 0;

	private void newCall(StringBuffer mainCall, SourceWriter sw, boolean close) {
		if (close) {
			sw.outdent();
			sw.println("}");
		}
		sw
				.println(String.format("private void generate_%s() {",
						++callCounter));
		sw.indent();
		mainCall.append(String.format("generate_%s();", callCounter));
		methodLengthCounter=0;
	}

	private String getObjLitRef(OutputInstantiation inst) {
		if (inst == null) {
			return "null";
		}
		return "obj_" + inst.id;
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
			return "\""
					+ ((String) value).replace("\"", "\\\"").replace("\n",
							"\\n") + "\"";
		}
		if (value instanceof Character) {
			return "'" + value + "'";
		}
		if (value instanceof Class){
			return ((Class) value).getSimpleName() + "." + "class";
		}
		if (value instanceof Long || value.getClass() == long.class) {
			return value + "L";
		}
		return value;
	}

	public static void main(String[] args) {
	}

	private String getClassName(Class<? extends Object> clazz) {
		return clazz.getSimpleName();
	}

	private IdentityHashMap<Object, OutputInstantiation> reached = new IdentityHashMap<Object, OutputInstantiation>();

	private List<OutputAssignment> assignments = new ArrayList<OutputAssignment>();

	private Map<OutputInstantiation, List<OutputInstantiation>> addToCollnMap = new HashMap<OutputInstantiation, List<OutputInstantiation>>();

	private Set<Class> reachedClasses = new LinkedHashSet<Class>();

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

	static class OutputInstantiation implements Comparable<OutputInstantiation> {
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

	private int idCounter = 1;

	private boolean isSimple(Object source) {
		Class c = source.getClass();
		return (c.isPrimitive() || c == String.class || c == Boolean.class
				|| c == Character.class || c.isEnum() || c == Class.class
				|| (source instanceof Number) || (source instanceof Date));
	}

	public OutputInstantiation traverse(Object source, ClassFieldPair context)
			throws Exception {
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
		if (source instanceof Class){
			reachedClasses.add((Class) source);	
		}
		if (isSimple(source)) {
			return instance;
		}
		PropertyDescriptor[] propertyDescriptors = getPropertyDescriptorsForClassProperties(source);
		Object template = source.getClass().newInstance();
		for (PropertyDescriptor pd : propertyDescriptors) {
			Object tgt = pd.getReadMethod().invoke(source,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			Object templateTgt = pd.getReadMethod().invoke(template,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			if (tgt != templateTgt) {
				assignments.add(new OutputAssignment(instance, pd, traverse(
						tgt, null)));
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
		return instance;
	}

	Map<Class, PropertyDescriptor[]> propertyDescriptorsPerClass = new HashMap<Class, PropertyDescriptor[]>();

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
			propertyDescriptorsPerClass
					.put(
							clazz,
							(PropertyDescriptor[]) allPropertyDescriptors
									.toArray(new PropertyDescriptor[allPropertyDescriptors
											.size()]));
		}
		return propertyDescriptorsPerClass.get(clazz);
	}
}

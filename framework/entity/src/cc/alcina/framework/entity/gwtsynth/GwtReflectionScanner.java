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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.RegistryScanner;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */

 public class GwtReflectionScanner {
	boolean printOnly = true;

	private Map<Class, Set<RegistryLocation>> gwtRegisteringClasses = new HashMap<Class, Set<RegistryLocation>>();

	public GwtReflectionScanner() {
	}

	public void scan(ScanInfo info, Collection<String> classNames,
			Collection<String> ignore) throws Exception {
		scan(info, classNames, ignore, null, false);
	}

	public void scan(ScanInfo info, Collection<String> classNames,
			Collection<String> ignore, String packageRoot,
			boolean childReflector) throws Exception {
		ListeningRegistry lr = new ListeningRegistry();
		lr.packageRoot=packageRoot;
		new RegistryScanner().scan(classNames, ignore, lr);
		GwtReflectionGenerator rs = new GwtReflectionGenerator();
		List<String> reflectorImports = new ArrayList<String>();
		List<String> domainImports = new ArrayList<String>();
		List<Class> reflectableClasses = new ArrayList<Class>();
		List<Class> instantiableClasses = new ArrayList<Class>();
		Map<Class, String> ann2impl = new HashMap<Class, String>();
		Map<String, String> simpleNameCheck = new HashMap<String, String>();
		ClassSourceFileComposerFactory domainFactory = null;
		for (String className : classNames) {
			Class c = null;
			if (ignore.contains(className)) {
				continue;
			}
			try {
				// System.out.println(className);
				c = Class.forName(className);
				if ((!Modifier.isPublic(c.getModifiers()))
						|| (Modifier.isAbstract(c.getModifiers()) && !c
								.isInterface())) {
					continue;
				}
			} catch (Error eiie) {
				continue;
			} catch (Exception e) {
				continue;
			}
			boolean sa = c.isAnnotationPresent(ClientVisible.class);
			boolean bi = c.isAnnotationPresent(BeanInfo.class);
			boolean in = c.isAnnotationPresent(ClientInstantiable.class);
			String implClassName = (sa) ? className
					+ info.getAnnotationImplSuffix() : "";
			if (sa || bi || in) {
			} else {
				continue;
			}
			ClassSourceFileComposerFactory factory = null;
			String simpleName = c.getSimpleName();
			String packageName = c.getPackage().getName();
			if (sa) {
				factory = new ClassSourceFileComposerFactory(info
						.getAnnotationImplPackage(), simpleName
						+ info.getAnnotationImplSuffix());
				// add enum classes
				Method[] methods = c.getMethods();
				for (Method method : methods) {
					if (Enum.class.isAssignableFrom(method.getReturnType())) {
						factory.addImport(method.getReturnType().getName());
						reflectorImports.add(method.getReturnType().getName());
					}
				}
				factory.addImplementedInterface(simpleName);
				factory.addImport(Annotation.class.getName());
				factory.addImport(className);
				reflectorImports.add(implClassName);
				ann2impl.put(c, implClassName);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				SourceWriter srcW = factory.createSourceWriter(pw);
				rs.generateAnnotationImplementation(
						(Class<? extends Annotation>) c, srcW, simpleName
								+ info.getAnnotationImplSuffix());
				StringWriter s2 = new StringWriter();
				s2.write(sw.toString().replaceFirst("public class",
						"@SuppressWarnings(\"all\")\npublic class"));
				processResult(s2, info.getAnnotationImplPath() + File.separator
						+ File.separator + simpleName
						+ info.getAnnotationImplSuffix() + ".java");
			} else {
				
				if (Modifier.isAbstract(c.getModifiers())
						|| (packageRoot != null && !className
								.startsWith(packageRoot))) {
					continue;
				}
				if (domainFactory == null && (bi||in)) {
					domainFactory = new ClassSourceFileComposerFactory(info
							.getDomainReflectorPackage(), info
							.getDomainReflectorClassName());
					domainFactory.addImport(HashMap.class.getName());
					domainFactory.addImport(Map.class.getName());
					domainFactory.addImport(GWT.class.getName());
					domainFactory.addImport(Registry.class.getName());
					domainFactory.addImport(Annotation.class.getName());
					domainFactory.setSuperclass(ClientReflector.class
							.getName());
					domainFactory.addImport(ClientBeanReflector.class.getName());
					domainFactory.addImport(ClientPropertyReflector.class
							.getName());
					domainFactory.addImport(ClientReflector.class.getName());
				}
				domainImports.add(c.getName());
				if (simpleNameCheck.containsKey(c.getSimpleName())) {
					if (!simpleNameCheck.get(c.getSimpleName()).equals(
							c.getName())) {
						System.err.println("simple name check failed:"
								+ c.getName() + ":"
								+ simpleNameCheck.get(c.getSimpleName()));
					}
				} else {
					simpleNameCheck.put(c.getSimpleName(), c.getName());
				}
				if (bi) {
					reflectableClasses.add(c);
				}
				if (in) {
					instantiableClasses.add(c);
				}
			}
		}// classes
		if (domainFactory != null) {
			for (String ai : reflectorImports) {
				domainFactory.addImport(ai.replace('$', '.'));
			}
			for (String ai : domainImports) {
				domainFactory.addImport(ai.replace('$', '.'));
			}
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			SourceWriter srcW = domainFactory.createSourceWriter(pw);
			rs.procDomain(reflectableClasses, instantiableClasses, srcW,
					ann2impl, gwtRegisteringClasses, childReflector,info);
			processResult(sw, info.getDomainReflectorPath() + "/"
					+ info.getDomainReflectorClassName() + ".java");
		}
	}

	private void processResult(StringWriter sw, String outFn)
			throws IOException {
		File f = new File(outFn);
		try {
			String s = ResourceUtilities.readFileToString(f);
			if (s.equals(sw.toString())) {
				System.out.print(f.getName() + " - ");
				System.out.println(" (ignore)");
				return;
			} else {
				System.out.print("\n" + f.getName() + " - written");
			}
		} catch (Exception e) {
		}
		ResourceUtilities.writeStringToFile(sw.toString(), f);
	}

	public static class ScanInfo {
		private final String annotationImplSuffix;

		private final String domainReflectorClassName;

		private final String domainReflectorPath;

		private final String domainReflectorPackage;

		private final String annotationImplPath;

		private final String annotationImplPackage;

		public ScanInfo(String annotationImplSuffix,
				String domainReflectorClassName, String domainReflectorPath,
				String domainReflectorPackage, String annotationImplPath,
				String annotationImplPackage) {
			this.annotationImplSuffix = annotationImplSuffix;
			this.domainReflectorClassName = domainReflectorClassName;
			this.domainReflectorPath = domainReflectorPath;
			this.domainReflectorPackage = domainReflectorPackage;
			this.annotationImplPath = annotationImplPath;
			this.annotationImplPackage = annotationImplPackage;
		}

		public String getDomainReflectorPackage() {
			return this.domainReflectorPackage;
		}

		public String getAnnotationImplPath() {
			return this.annotationImplPath;
		}

		public String getAnnotationImplPackage() {
			return this.annotationImplPackage;
		}

		public String getAnnotationImplSuffix() {
			return this.annotationImplSuffix;
		}

		public String getDomainReflectorClassName() {
			return this.domainReflectorClassName;
		}

		public String getDomainReflectorPath() {
			return domainReflectorPath;
		}
	}

	class ListeningRegistry extends Registry {
		String packageRoot;
		@Override
		public void register(Class registeringClass, RegistryLocation info) {
			if (info.j2seOnly()) {
				return;
			}
			if(packageRoot != null && !registeringClass.getName()
					.startsWith(packageRoot)){
				return;
			}
			if (!registry.containsKey(registeringClass)) {
				gwtRegisteringClasses.put(registeringClass,
						new LinkedHashSet<RegistryLocation>());
			}
			gwtRegisteringClasses.get(registeringClass).add(info);
		}
	}
}

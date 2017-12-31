/*
 * IntrospectorGenerator.java
 *
 * Created on July 15, 2007, 2:21 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.rebind.beans;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.totsp.gwittir.client.beans.TreeIntrospector;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * (Nick) Pretty much a complete rewrite of Robert's generator - because we
 * don't care about GWT compiler optimisations (we do our own multi-pass
 * compilation), we can use native JSNI methods for much better obfuscated size
 * and improved performance
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 *
 */
@SuppressWarnings({ "deprecation" })
public class IntrospectorGenerator extends Generator {
	private String implementationName;

	private String packageName = com.totsp.gwittir.client.beans.Introspector.class
			.getCanonicalName()
			.substring(0, com.totsp.gwittir.client.beans.Introspector.class
					.getCanonicalName().lastIndexOf("."));

	private IntrospectorFilter filter;

	/** Creates a new instance of IntrospectorGenerator */
	public IntrospectorGenerator() {
	}

	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		filter = IntrospectorFilterHelper.getFilter(context);
		String superClassName = null;
		try {
			JClassType intrType = context.getTypeOracle().getType(typeName);
			if (intrType.isInterface() != null) {
				intrType = context.getTypeOracle()
						.getType(TreeIntrospector.class.getName());
			}
			ReflectionModule module = intrType
					.getAnnotation(ReflectionModule.class);
			if (module == null) {
				logger.log(TreeLogger.INFO,
						"introspectorGen debug - no module name - intrType: "
								+ intrType.getQualifiedSourceName());
				return null;
			}
			filter.setModuleName(module.value());
			this.implementationName = String.format("Introspector_Impl_%s",
					module.value());
			superClassName = intrType.getQualifiedSourceName();
		} catch (NotFoundException ex) {
			logger.log(TreeLogger.ERROR, typeName, ex);
			return null;
		}
		PrintWriter printWriter = context.tryCreate(logger, packageName,
				implementationName);
		List<BeanResolver> introspectables = this.getIntrospectableTypes(logger,
				context.getTypeOracle());
		MethodWrapper[] methods = this.findMethods(logger, introspectables);
		ClassSourceFileComposerFactory cfcf = new ClassSourceFileComposerFactory(
				this.packageName, this.implementationName);
		cfcf.setSuperclass(superClassName);
		cfcf.addImport("java.util.HashMap");
		cfcf.addImport(UnsafeNativeLong.class.getCanonicalName());
		cfcf.addImport(NoSuchPropertyException.class.getCanonicalName());
		cfcf.addImport(
				com.totsp.gwittir.client.beans.Method.class.getCanonicalName());
		cfcf.addImport(com.totsp.gwittir.client.beans.Property.class
				.getCanonicalName());
		cfcf.addImport(com.totsp.gwittir.client.beans.BeanDescriptor.class
				.getCanonicalName());
		cfcf.addImport(com.totsp.gwittir.client.beans.BeanDescriptorImpl.class
				.getCanonicalName());
		cfcf.addImport(com.totsp.gwittir.client.beans.NativeMethodWrapper.class
				.getCanonicalName());
		if (printWriter == null) {
			// .println( "Introspector Generate skipped.");
			return packageName + "." + implementationName;
		}
		System.out.format("Introspector - %s - %s introspectable types\n",
				filter.getModuleName(), introspectables.size());
		SourceWriter writer = cfcf.createSourceWriter(context, printWriter);
		writer.println();
		writer.println(
				"protected BeanDescriptor getDescriptor0( Object object ){ ");
		writer.indent();
		writer.println("return descriptorLookup.get(object.getClass());");
		writer.outdent();
		writer.println("}");
		{
			int descriptorIdx = 0;
			for (BeanResolver resolver : introspectables) {
				writer.println();
				writer.println(
						String.format("private void registerDescriptor_%s( ){ ",
								descriptorIdx));
				writer.indent();
				String name = resolver.getType().getQualifiedSourceName()
						.replaceAll("\\.", "_");
				logger.log(TreeLogger.DEBUG, "Writing : " + name, null);
				this.writeBeanDescriptor(logger, resolver, writer);
				writer.println(String.format(
						"descriptorLookup.put( %s.class, descriptor);",
						resolver.getType().getQualifiedSourceName()));
				writer.outdent();
				writer.println("}");
				descriptorIdx++;
			}
		}
		{
			writer.println("protected void  registerBeanDescriptors( ){ ");
			writer.indent();
			int descriptorIdx = 0;
			for (BeanResolver resolver : introspectables) {
				writer.println(String.format("registerDescriptor_%s( ); ",
						descriptorIdx));
				descriptorIdx++;
			}
			writer.outdent();
			writer.println("}");
		}
		writeMethods(logger, methods, writer);
		writer.outdent();
		writer.println("}");
		context.commit(logger, printWriter);
		filter.generationComplete();
		// .println( "Introspector Generate completed.");
		return packageName + "." + implementationName;
	}

	private String boxPrefix(JType type) {
		JPrimitiveType primitive = type.isPrimitive();
		if (primitive != null && !ignorePrimitiveGwt28(primitive)) {
			return String.format("@%s::new(%s)(",
					primitive.getQualifiedBoxedSourceName(),
					primitive.getJNISignature());
		} else {
			return "";
		}
	}

	private MethodWrapper[] findMethods(TreeLogger logger,
			List introspectables) {
		// declaring class _NAME_ (GWT outputs multiple types for different
		// generic variants), name
		UnsortedMultikeyMap<MethodWrapper> found = new UnsortedMultikeyMap<MethodWrapper>(
				2);
		for (Iterator it = introspectables.iterator(); it.hasNext();) {
			BeanResolver info = (BeanResolver) it.next();
			logger.branch(TreeLogger.DEBUG, "Method Scanning: "
					+ info.getType().getQualifiedSourceName(), null);
			try {
				Collection<RProperty> pds = info.getProperties().values();
				for (RProperty p : pds) {
					MethodWrapper method = p.getReadMethod();
					if (method != null) {
						found.put(
								method.getDeclaringType()
										.getQualifiedSourceName(),
								method.getBaseMethod().getName(), method);
					}
					method = p.getWriteMethod();
					if (method != null) {
						found.put(
								method.getDeclaringType()
										.getQualifiedSourceName(),
								method.getBaseMethod().getName(), method);
					}
				}
			} catch (Exception e) {
				logger.log(TreeLogger.ERROR,
						"Unable to introspect class. Is class a bean?", e);
			}
		}
		List<MethodWrapper> allValues = found.allValues();
		return (MethodWrapper[]) allValues
				.toArray(new MethodWrapper[allValues.size()]);
	}

	private Set<BeanResolver> getFileDeclaredTypes(TreeLogger logger,
			TypeOracle oralce) throws UnableToCompleteException {
		HashSet<BeanResolver> results = new HashSet<BeanResolver>();
		ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
		try {
			Enumeration<URL> introspections = ctxLoader
					.getResources("gwittir-introspection.properties");
			while (introspections.hasMoreElements()) {
				URL propsUrl = introspections.nextElement();
				logger.log(TreeLogger.Type.INFO,
						"Loading: " + propsUrl.toString());
				Properties props = new Properties();
				props.load(propsUrl.openStream());
				for (Entry entry : props.entrySet()) {
					String className = entry.getKey().toString();
					String[] includedProps = entry.getValue().toString()
							.split(",");
					JClassType type = oralce.findType(className);
					if (type == null) {
						logger.log(TreeLogger.Type.ERROR, "Unable to find type "
								+ className + " declared in " + propsUrl);
						throw new UnableToCompleteException();
					}
					results.add(new BeanResolver(logger, type, includedProps));
				}
			}
		} catch (IOException ioe) {
			logger.log(TreeLogger.Type.WARN,
					"Exception looking for properties files", ioe);
		}
		return results;
	}

	private String
			getNonParameterisedJsniSignature(MethodWrapper methodWrapper) {
		JMethod method = methodWrapper.getBaseMethod();
		StringBuilder sb = new StringBuilder("@");
		sb.append(methodWrapper.getDeclaringType().getQualifiedSourceName());
		sb.append("::");
		sb.append(method.getName());
		sb.append("(");
		for (JParameter param : method.getParameters()) {
			String jniSignature = param.getType().getJNISignature();
			JParameterizedType declarerParameterizedType = methodWrapper
					.getDeclaringType().isParameterized();
			if (declarerParameterizedType != null) {
				JMethod[] methods = declarerParameterizedType.getBaseType()
						.getMethods();
				for (JMethod jMethod : methods) {
					if (jMethod.getName().equals(method.getName())) {
						assert jMethod.getParameters().length == 1;
						JParameter superParam = jMethod.getParameters()[0];
						jniSignature = superParam.getType().getJNISignature();
						break;
					}
				}
			}
			sb.append(jniSignature);
		}
		sb.append(")");
		String result = sb.toString();
		if (result.contains("BoundWidget")) {
			int j = 3;
		}
		return result;
	}

	private boolean isIntrospectable(TreeLogger logger, JType type) {
		if (type == null)
			return false;
		JClassType ct = type.isClassOrInterface();
		if (ct != null) {
			if (ct.getAnnotation(Introspectable.class) != null) {
				return true;
			}
			for (JClassType iface : ct.getImplementedInterfaces()) {
				if (isIntrospectable(logger, iface)) {
					return true;
				}
			}
			if (isIntrospectable(logger, ct.getSuperclass())) {
				return true;
			}
		}
		return false;
	}

	private JType resolveType(final JType type) {
		JType ret = type;
		JParameterizedType pt = type.isParameterized();
		if (pt != null) {
			ret = pt.getRawType();
		}
		JTypeParameter tp = ret.isTypeParameter();
		if (tp != null) {
			ret = tp.getBaseType();
		}
		return ret;
	}

	private String unbox(JType param0type) {
		String arg = "arg";
		JPrimitiveType primitive = param0type.isPrimitive();
		if (primitive != null && !ignorePrimitiveGwt28(primitive)) {
			String extractMethod = primitive.toString().toLowerCase();
			return String.format("arg.@%s::%sValue()()",
					primitive.getQualifiedBoxedSourceName(), extractMethod);
		}
		return "arg";
	}

	private void writeBeanDescriptor(TreeLogger logger, BeanResolver info,
			SourceWriter writer) {
		writer.println(
				"BeanDescriptorImpl descriptor = new BeanDescriptorImpl(); ");
		Collection pds = info.getProperties().values();
		int i = 0;
		for (Iterator it = pds.iterator(); it.hasNext(); i++) {
			RProperty p = (RProperty) it.next();
			String propertyName = p.getName();
			writer.println("");
			writer.println("{");
			writer.indent();
			JType ptype = this.resolveType(p.getType());
			logger.log(TreeLogger.DEBUG,
					p.getName() + " (Erased) " + ptype.getQualifiedSourceName(),
					null);
			writer.print("Method readMethod = ");
			String returnType = p.getReadMethod().getBaseMethod()
					.getReturnType().getQualifiedSourceName();
			String extendsMarker = " extends ";
			int idx = returnType.indexOf(extendsMarker);
			if (idx != -1) {
				returnType = returnType.substring(idx + extendsMarker.length());
			}
			if (p.getReadMethod() == null) {
				writer.println("null;");
			} else {
				writer.println(String.format(
						"new NativeMethodWrapper(%s.class, \"%s\",%s.class,this);",
						p.getReadMethod().getDeclaringType()
								.getQualifiedSourceName(),
						p.getReadMethod().getBaseMethod().getName(),
						returnType));
			}
			writer.print("Method writeMethod = ");
			if (p.getWriteMethod() == null) {
				writer.println("null;");
			} else {
				writer.println(String.format(
						"new NativeMethodWrapper(%s.class, \"%s\",void.class,this);",
						p.getWriteMethod().getDeclaringType()
								.getQualifiedSourceName(),
						p.getWriteMethod().getBaseMethod().getName()));
			}
			logger.log(TreeLogger.DEBUG,
					p.getName() + " " + p.getType().getQualifiedSourceName(),
					null);
			writer.println("descriptor.registerProperty (new Property( \""
					+ p.getName() + "\", "
					+ ((p.getType() != null) ? ptype.getQualifiedSourceName()
							: "Object")
					+ ".class,  readMethod, writeMethod ));");
			writer.outdent();
			writer.println("}");
		}
	}

	private void writeMethods(TreeLogger logger, MethodWrapper[] methods,
			SourceWriter writer) {
		for (int i = 0; i < methods.length; i++) {
			MethodWrapper method = methods[i];
			writer.println();
			if (method.getBaseMethod().getReturnType() == JPrimitiveType.LONG) {
				writer.println("@UnsafeNativeLong");
			}
			JType param0type = null;
			if (method.getBaseMethod().getParameterTypes().length == 1) {
				param0type = method.getBaseMethod().getParameterTypes()[0];
			}
			if (param0type == JPrimitiveType.LONG) {
				writer.println("@UnsafeNativeLong");
			}
			writer.println(String.format(
					"private native void registerMethod_%s(Class declaringClass,String methodName)/*-{",
					i));
			writer.indent();
			writer.println(
					"this.@com.totsp.gwittir.client.beans.TreeIntrospector::"
							+ "methodLookup[declaringClass][methodName] = function("
							+ "object, arg) {");
			writer.indent();
			if (method.getBaseMethod().getReturnType() != JPrimitiveType.VOID) {
				// getter
				String boxPrefix = boxPrefix(
						method.getBaseMethod().getReturnType());
				String boxSuffix = boxPrefix.isEmpty() ? "" : ")";
				writer.println(String.format("return %sobject.%s()%s;",
						boxPrefix, getNonParameterisedJsniSignature(method),
						boxSuffix));
			} else {
				// setter
				String arg = unbox(param0type);
				writer.println(String.format("object.%s(%s);",
						getNonParameterisedJsniSignature(method), arg));
				writer.println("return null;");
			}
			writer.outdent();
			writer.println("};");
			writer.outdent();
			writer.println("}-*/;");
		}
		writer.println();
		writer.println("protected void  registerMethods( ){ ");
		writer.indent();
		Set<String> declaredClasses = new LinkedHashSet<String>();
		for (int i = 0; i < methods.length; i++) {
			MethodWrapper method = methods[i];
			if (declaredClasses
					.add(method.getDeclaringType().getQualifiedSourceName())) {
				writer.println(String.format(
						"registerMethodDeclaringType(%s.class);",
						method.getDeclaringType().getQualifiedSourceName()));
			}
			String methodName = method.getBaseMethod().getName();
			writer.println(String.format("registerMethod_%s(%s.class,\"%s\");",
					i, method.getDeclaringType().getQualifiedSourceName(),
					methodName));
		}
		writer.outdent();
		writer.println("}");
	}

	protected List<BeanResolver> getIntrospectableTypes(TreeLogger logger,
			TypeOracle oracle) {
		ArrayList<BeanResolver> results = new ArrayList<BeanResolver>();
		HashSet<BeanResolver> resolvers = new HashSet<BeanResolver>();
		HashSet<String> found = new HashSet<String>();
		try {
			JClassType[] types = oracle.getTypes();
			// .println("Found "+types.length +" types.");
			JClassType introspectable = oracle
					.getType(com.totsp.gwittir.client.beans.Introspectable.class
							.getCanonicalName());
			for (JClassType type : types) {
				if (!found.contains(type.getQualifiedSourceName())
						&& (isIntrospectable(logger, type)
								|| type.isAssignableTo(introspectable))
						&& (type.isInterface() == null)) {
					found.add(type.getQualifiedSourceName());
					BeanResolver resolver = new BeanResolver(logger, type);
					filter.filterProperties(resolver);
					resolvers.add(resolver);
				}
			}
			// Do a crazy assed sort to make sure least
			// assignable types are at the bottom of the list
			results.addAll(resolvers);
			results.addAll(this.getFileDeclaredTypes(logger, oracle));
			boolean swap = true;
			// .print("Ordering "+results.size()+" by heirarchy ");
			while (swap) {
				// .print(".");
				swap = false;
				for (int i = results.size() - 1; i >= 0; i--) {
					BeanResolver type = (BeanResolver) results.get(i);
					for (int j = i - 1; j >= 0; j--) {
						BeanResolver check = (BeanResolver) results.get(j);
						if (type.getType().isAssignableTo(check.getType())) {
							results.set(i, check);
							results.set(j, type);
							type = check;
							swap = true;
						}
					}
				}
			}
			// System.out.println();
		} catch (Exception e) {
			logger.log(TreeLogger.ERROR, "Unable to find Introspectable types.",
					e);
		}
		filter.filterIntrospectorResults(results);
		return results;
	}

	boolean ignorePrimitiveGwt28(JPrimitiveType primitive) {
		if (!GWT.isClient()) {// i.e. not compiling for devmode - compiling to
								// js
			switch (primitive) {
			case BOOLEAN:
			case DOUBLE:
				return true;
			}
		}
		return false;
	}
}

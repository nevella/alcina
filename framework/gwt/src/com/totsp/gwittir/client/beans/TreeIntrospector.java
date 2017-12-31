package com.totsp.gwittir.client.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;

@ReflectionModule(ReflectionModule.INITIAL)
public abstract class TreeIntrospector implements Introspector {
	protected HashMap<Class, BeanDescriptor> descriptorLookup = new HashMap<Class, BeanDescriptor>();

	private JavaScriptObject methodLookup;

	private List<TreeIntrospector> introspectors = new ArrayList<TreeIntrospector>();

	public TreeIntrospector() {
		introspectors.add(this);
		initMethodLookup();
		registerMethods();
		registerBeanDescriptors();
	}

	@Override
	public BeanDescriptor getDescriptor(Object object) {
		BeanDescriptor descriptor = getDescriptorOrNull(object);
		if (descriptor != null) {
			return descriptor;
		} else {
			throw new IllegalArgumentException(
					"Unknown type (introspector): " + object.getClass());
		}
	}

	public BeanDescriptor getDescriptorOrNull(Object object) {
		if (object == null) {
			throw new NullPointerException("Attempt to introspect null object");
		}
		if (descriptorLookup.containsKey(object.getClass())) {
			return descriptorLookup.get(object.getClass());
		}
		if (object instanceof SelfDescribed) {
			return ((SelfDescribed) object).__descriptor();
		}
		for (TreeIntrospector introspector : introspectors) {
			BeanDescriptor descriptor = introspector.getDescriptor0(object);
			if (descriptor != null) {
				descriptorLookup.put(object.getClass(), descriptor);
				return descriptor;
			}
		}
		return null;
	}

	public native JavaScriptObject getNativeMethod(Class declaringClass,
			String methodName) /*-{
								return this.@com.totsp.gwittir.client.beans.TreeIntrospector::methodLookup[declaringClass][methodName];
								}-*/;

	public void registerChild(TreeIntrospector child) {
		introspectors.add(0, child);
	}

	public native JavaScriptObject
			registerMethodDeclaringType(Class declaringClass) /*-{
																return this.@com.totsp.gwittir.client.beans.TreeIntrospector::methodLookup[declaringClass] = [];
																}-*/;

	@Override
	public Class resolveClass(Object instance) {
		throw new UnsupportedOperationException("Not implemented - but "
				+ "if it were, better to add resolved class to beandescriptor type ");
	}

	private native void initMethodLookup()/*-{
											this.@com.totsp.gwittir.client.beans.TreeIntrospector::methodLookup = [];
											}-*/;

	protected abstract BeanDescriptor getDescriptor0(Object object);

	protected abstract void registerBeanDescriptors();

	protected abstract void registerMethods();
}

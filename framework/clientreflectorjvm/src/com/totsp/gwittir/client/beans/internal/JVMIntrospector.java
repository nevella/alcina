/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.totsp.gwittir.client.beans.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.HashMap;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Method;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.beans.SelfDescribed;

/**
 * 
 * @author nreddel NOTE: registery registration in the constructor - may want to
 *         bypass this for testing
 */
@ClientInstantiable
public class JVMIntrospector implements Introspector, BeanDescriptorProvider {
	private HashMap<Class, BeanDescriptor> cache = new HashMap<Class, BeanDescriptor>();

	public BeanDescriptor getDescriptor(Object object) {
		if (cache.containsKey(object.getClass())) {
			return cache.get(object.getClass());
		}
		BeanDescriptor result = null;
		if (object instanceof SelfDescribed) {
			// System.out.println("SelfDescribed\t"+
			// object.getClass().getName());
			result = ((SelfDescribed) object).__descriptor();
		} else {
			// System.out.println("Reflection\t"+ object.getClass().getName());
			result = new ReflectionBeanDescriptor(object.getClass());
			cache.put(object.getClass(), result);
		}
		return result;
	}

	public Class resolveClass(Object instance) {
		return instance.getClass();
	}

	public JVMIntrospector() {
		Registry.putSingleton1( BeanDescriptorProvider.class,this);
	}

	private static class ReflectionBeanDescriptor implements BeanDescriptor {
		BeanInfo info;

		Property[] props;

		String className;

		ReflectionBeanDescriptor(Class clazz) {
			try {
				className = clazz.getName();
				info = java.beans.Introspector.getBeanInfo(clazz);
				props = new Property[info.getPropertyDescriptors().length - 1];
				int index = 0;
				Class enumSubclass = null;
				for (PropertyDescriptor d : info.getPropertyDescriptors()) {
					Class<?> propertyType = d.getPropertyType();
					if (propertyType != null && propertyType.isEnum()
							&& propertyType.getSuperclass() == Enum.class) {
						enumSubclass = propertyType;
					}
				}
				for (PropertyDescriptor d : info.getPropertyDescriptors()) {
					Class<?> propertyType = d.getPropertyType();
					if (propertyType == Enum.class
							&& d.getName().equals("value")) {
						propertyType = enumSubclass;
						assert propertyType != null;
					}
					if (d.getName().equals("class")) {
						continue;
					}
					props[index] = new Property(d.getName(), propertyType,
							d.getReadMethod() == null ? null
									: new MethodWrapper(d.getReadMethod()),
							d.getWriteMethod() == null ? null
									: new MethodWrapper(d.getWriteMethod()));
					// System.out.println(clazz+" mapped property: "+props[index]);
					index++;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public Property[] getProperties() {
			return this.props;
		}

		public Property getProperty(String name) {
			for (Property p : props) {
				if (p.getName().equals(name)) {
					return p;
				}
			}
			throw new RuntimeException("Unknown property: " + name
					+ " on class " + className);
		}
	}

	public static class MethodWrapper implements Method {
		private final java.lang.reflect.Method inner;

		public MethodWrapper(java.lang.reflect.Method inner) {
			assert inner != null;
			this.inner = inner;
		}

		// @Override
		// For JDK1.5 compatibility, don't override methods inherited from an
		// interface
		public String getName() {
			return ((java.lang.reflect.Method) inner).toString();
		}

		// @Override
		public Object invoke(Object target, Object[] args) throws Exception {
			return inner.invoke(target, args);
		}

		@Override
		public String toString() {
			return inner.toString();
		}
	}
}

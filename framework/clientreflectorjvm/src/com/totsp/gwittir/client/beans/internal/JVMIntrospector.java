/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.totsp.gwittir.client.beans.internal;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Method;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.beans.SelfDescribed;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

/**
 *
 * @author nreddel NOTE: registery registration in the constructor - may want to
 *         bypass this for testing
 */
@ClientInstantiable
public class JVMIntrospector implements Introspector, BeanDescriptorProvider {
	private HashMap<Class, BeanDescriptor> cache = new HashMap<Class, BeanDescriptor>();

	private Predicate<String> filter;

	public JVMIntrospector() {
		Reflections.registerBeanDescriptorProvider(this);
		String filterClassName = System
				.getProperty(ClientReflectorJvm.PROP_FILTER_CLASSNAME);
		if (filterClassName != null) {
			try {
				filter = (Predicate<String>) Class.forName(filterClassName)
						.getConstructor().newInstance();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	@Override
	public BeanDescriptor getDescriptor(Object object) {
		Class<? extends Object> clazz = object.getClass();
		if (cache.containsKey(clazz)) {
			return cache.get(clazz);
		}
		BeanDescriptor result = null;
		if (object instanceof SelfDescribed) {
			// System.out.println("SelfDescribed\t"+
			// object.getClass().getName());
			result = ((SelfDescribed) object).__descriptor();
		} else {
			// System.out.println("Reflection\t"+ object.getClass().getName());
			if (filter != null && !filter.test(clazz.getName())) {
				GWT.log(Ax.format(
						"Warn: accessing filtered (reflection) class:\n%s",
						clazz.getName()));
			}
			if (ClientReflectorJvm.canIntrospect(clazz)) {
				result = new ReflectionBeanDescriptor(clazz);
			}
			cache.put(clazz, result);
		}
		return result;
	}

	@Override
	public BeanDescriptor getDescriptorOrNull(Object object) {
		return getDescriptor(object);
	}

	@Override
	public Class resolveClass(Object instance) {
		return instance.getClass();
	}

	public static class MethodWrapper implements Method {
		private final java.lang.reflect.Method inner;

		public MethodWrapper(java.lang.reflect.Method inner) {
			assert inner != null;
			this.inner = inner;
		}

		@Override
		public Class getDeclaringClass() {
			return inner.getDeclaringClass();
		}

		public java.lang.reflect.Method getInner() {
			return this.inner;
		}

		@Override
		public String getName() {
			return ((java.lang.reflect.Method) inner).toString();
		}

		@Override
		public Object invoke(Object target, Object[] args) throws Exception {
			return inner.invoke(target, args);
		}

		@Override
		public String toString() {
			return inner.getDeclaringClass() + ":" + inner.toString();
		}
	}

	public static class ReflectionBeanDescriptor implements BeanDescriptor {
		Property[] props;

		String className;

		public ReflectionBeanDescriptor(Class clazz) {
			try {
				className = clazz.getName();
				ClientReflectorJvm.checkClassAnnotations(clazz);
				List<Property> properties = new ArrayList<>();
				for (PropertyDescriptor d : SEUtilities
						.getPropertyDescriptorsSortedByField(clazz)) {
					Class<?> propertyType = d.getPropertyType();
					if (d.getReadMethod() != null
							&& d.getReadMethod().getReturnType() != propertyType
							&& d.getReadMethod()
									.getReturnType() == Enum.class) {
						// GWT compiler returning more exact generic type?
						propertyType = Enum.class;
					}
					// if (propertyType == Enum.class
					// && d.getName().equals("value")) {
					// propertyType = enumSubclass;
					// assert propertyType != null;
					// }
					if (d.getName().equals("class")
							|| d.getName().equals("propertyChangeListeners")) {
						continue;
					}
					properties.add(new Property(d.getName(), propertyType,
							d.getReadMethod() == null ? null
									: new MethodWrapper(d.getReadMethod()),
							d.getWriteMethod() == null ? null
									: new MethodWrapper(d.getWriteMethod())));
				}
				props = (Property[]) properties
						.toArray(new Property[properties.size()]);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Property[] getProperties() {
			return this.props;
		}

		@Override
		public Property getProperty(String name) {
			for (Property p : props) {
				if (p.getName().equals(name)) {
					return p;
				}
			}
			throw new NoSuchPropertyException(
					"Unknown property: " + name + " on class " + className);
		}
	}
}

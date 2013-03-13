package com.totsp.gwittir.client.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;

@ReflectionModule(ReflectionModule.INITIAL)
public abstract class TreeIntrospector implements Introspector {
	private HashMap<Class, BeanDescriptor> beanDescriptorLookup = new HashMap<Class, BeanDescriptor>();

	@Override
	public BeanDescriptor getDescriptor(Object object) {
		getDescriptorOrNull(object);
		BeanDescriptor descriptor = getDescriptorOrNull(object);
		if (descriptor != null) {
			return descriptor;
		} else {
			throw new IllegalArgumentException("Unknown type (introspector): "
					+ object.getClass());
		}
	}

	public BeanDescriptor getDescriptorOrNull(Object object) {
		if (object == null) {
			throw new NullPointerException("Attempt to introspect null object");
		}
		if (beanDescriptorLookup.containsKey(object.getClass())) {
			return beanDescriptorLookup.get(object.getClass());
		}
		if (object instanceof SelfDescribed) {
			return ((SelfDescribed) object).__descriptor();
		}
		for (TreeIntrospector introspector : introspectors) {
			BeanDescriptor descriptor = introspector.getDescriptor0(object);
			if (descriptor != null) {
				beanDescriptorLookup.put(object.getClass(), descriptor);
				return descriptor;
			}
		}
		return null;
	}

	@Override
	public Class resolveClass(Object instance) {
		throw new UnsupportedOperationException("Not implemented - but " +
				"if it were, better to add resolved class to beandescriptor type ");
	}

	public TreeIntrospector() {
		introspectors.add(this);
	}

	private List<TreeIntrospector> introspectors = new ArrayList<TreeIntrospector>();

	protected abstract BeanDescriptor getDescriptor0(Object object);

	public void registerChild(TreeIntrospector child) {
		introspectors.add(0, child);
	}


	

}

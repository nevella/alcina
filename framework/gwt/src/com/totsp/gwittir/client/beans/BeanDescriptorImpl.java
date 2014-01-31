package com.totsp.gwittir.client.beans;

import java.util.Collection;
import java.util.HashMap;

public class BeanDescriptorImpl implements BeanDescriptor {
	private HashMap<String, Property> properties = new HashMap<String, Property>();

	public void registerProperty(Property property) {
		properties.put(property.getName(), property);
	}

	@Override
	public Property[] getProperties() {
		Collection<Property> values = properties.values();
		return (Property[]) values.toArray(new Property[values.size()]);
	}

	@Override
	public Property getProperty(String name) {
		return properties.get(name);
	}
}

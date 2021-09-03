package cc.alcina.framework.entity.transform;

import java.beans.PropertyDescriptor;
import java.util.List;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.MethodWrapper;

public class ReflectionBeanDescriptor implements BeanDescriptor {
	Property[] props;

	String className;

	ReflectionBeanDescriptor(Class clazz) {
		try {
			className = clazz.getName();
			List<PropertyDescriptor> propertyDescriptors = SEUtilities
					.getSortedPropertyDescriptors(clazz);
			props = new Property[propertyDescriptors.size() - 1];
			int index = 0;
			Class enumSubclass = null;
			// FIXME - mvcc.5 - is this 'enumSubclass' necessary?
			for (PropertyDescriptor d : propertyDescriptors) {
				Class<?> propertyType = d.getPropertyType();
				if (propertyType != null && propertyType.isEnum()
						&& propertyType.getSuperclass() == Enum.class
						&& propertyType != Direction.class) {
					// hacky - but works
					enumSubclass = propertyType;
				}
			}
			for (PropertyDescriptor d : propertyDescriptors) {
				Class<?> propertyType = d.getPropertyType();
				if (propertyType == Enum.class && d.getName().equals("value")) {
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
				index++;
			}
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
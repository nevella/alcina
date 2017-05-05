package cc.alcina.framework.entity.domaintransform;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliComparator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;
import cc.alcina.framework.entity.SEUtilities;

public class MethodIndividualPropertyAccessor
		implements IndividualPropertyAccessor {
	private Object[] emptyValue = new Object[0];

	private Method readMethod;

	private Class methodDeclaringClass;

	private String propertyName;

	private Method writeMethod;

	private int propertyIndex = -1;

	private String fullPath;

	public MethodIndividualPropertyAccessor(Class clazz, String propertyName) {
		methodDeclaringClass = null;// be lazy
		Pattern indexedPattern = Pattern.compile("(.+)\\[(\\d+)\\]");
		Matcher m = indexedPattern.matcher(propertyName);
		if (m.matches()) {
			this.fullPath = propertyName;
			this.propertyName = m.group(1);
			this.propertyIndex = Integer.parseInt(m.group(2));
		} else {
			this.propertyName = propertyName;
		}
	}

	@Override
	public Class getPropertyType(Object bean) {
		try {
			ensureMethods(bean);
			if (isIndexed()) {
				throw new UnsupportedOperationException();
			}
			return readMethod.getReturnType();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Object getPropertyValue(Object bean) {
		try {
			ensureMethods(bean);
			Object value = readMethod.invoke(bean, emptyValue);
			if (isIndexed() && value != null) {
				Collection c = (Collection) value;
				if (c.isEmpty()) {
					return null;
				}
				List list = new ArrayList(c);
				if (c.iterator().next() instanceof HasIdAndLocalId) {
					list.sort(HiliComparator.INSTANCE);
				}
				if (propertyIndex < list.size()) {
					return list.get(propertyIndex);
				} else {
					return null;
				}
			}
			return value;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void setPropertyValue(Object bean, Object value) {
		try {
			if (isIndexed()) {
				throw new UnsupportedOperationException(
						"Invalid set index: " + fullPath);
			}
			ensureMethods(bean);
			writeMethod.invoke(bean, value);
		} catch (Exception e) {
			System.err.format(
					"Exception setting property value: \nObject: %s\nProperty: %s - %s\nValue: %s\n",
					bean, propertyName, fullPath, value);
			throw new WrappedRuntimeException(e);
		}
	}

	private boolean isIndexed() {
		return fullPath != null;
	}

	protected void ensureMethods(Object value) throws IntrospectionException {
		Class<? extends Object> clazz = value.getClass();
		if (clazz != methodDeclaringClass) {
			PropertyDescriptor pd = SEUtilities
					.getPropertyDescriptorByName(clazz, propertyName);
			this.readMethod = pd.getReadMethod();
			this.writeMethod = pd.getWriteMethod();
			methodDeclaringClass = clazz;
		}
	}
}
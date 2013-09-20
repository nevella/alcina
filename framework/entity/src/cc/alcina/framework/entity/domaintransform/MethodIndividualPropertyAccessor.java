package cc.alcina.framework.entity.domaintransform;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;
import cc.alcina.framework.entity.SEUtilities;

public class MethodIndividualPropertyAccessor implements
		IndividualPropertyAccessor {
	private Object[] emptyValue = new Object[0];

	private Method readMethod;

	private Class methodDeclaringClass;

	private String propertyName;

	private Method writeMethod;

	public MethodIndividualPropertyAccessor(Class clazz, String propertyName) {
		methodDeclaringClass = null;// be lazy
		this.propertyName = propertyName;
	}

	@Override
	public Object getPropertyValue(Object bean) {
		try {
			ensureMethods(bean);
			return readMethod.invoke(bean, emptyValue);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void ensureMethods(Object value) throws IntrospectionException {
		Class<? extends Object> clazz = value.getClass();
		if (clazz != methodDeclaringClass) {
			PropertyDescriptor pd = SEUtilities.getPropertyDescriptorByName(
					clazz, propertyName);
			this.readMethod = pd.getReadMethod();
			this.writeMethod = pd.getWriteMethod();
			methodDeclaringClass = clazz;
		}
	}

	@Override
	public void setPropertyValue(Object bean, Object value) {
		try {
			ensureMethods(bean);
			writeMethod.invoke(bean, value);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
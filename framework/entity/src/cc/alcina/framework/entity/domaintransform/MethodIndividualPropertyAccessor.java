package cc.alcina.framework.entity.domaintransform;

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

	public MethodIndividualPropertyAccessor(Class clazz, String propertyName) {
		methodDeclaringClass = null;//be lazy
		this.propertyName = propertyName;
	}


	@Override
	public Object getPropertyValue(Object value) {
		try {
			Class<? extends Object> clazz = value.getClass();
			if (clazz != methodDeclaringClass) {
				this.readMethod = SEUtilities.getPropertyDescriptorByName(clazz,
						propertyName).getReadMethod();
				methodDeclaringClass=clazz;
			}
			return readMethod.invoke(value, emptyValue);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
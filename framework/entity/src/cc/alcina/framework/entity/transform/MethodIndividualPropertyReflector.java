package cc.alcina.framework.entity.transform;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.SEUtilities;

/*
 * FIXME - reflection - won't this be concurrently accessed by queries? I think remove prop reflector impl and have it delegate to various properties
 */
public class MethodIndividualPropertyReflector implements PropertyReflector {
	private static MultikeyMap<MethodIndividualPropertyReflector> cache = new UnsortedMultikeyMap<>(
			2);

	public static synchronized MethodIndividualPropertyReflector
			get(Class clazz, String propertyName) {
		return cache.ensure(
				() -> new MethodIndividualPropertyReflector(
						Domain.resolveEntityClass(clazz), propertyName),
				Domain.resolveEntityClass(clazz), propertyName);
	}

	private Object[] emptyValue = new Object[0];

	private Method readMethod;

	private Class methodDeclaringClass;

	private String propertyName;

	private Method writeMethod;

	private int propertyIndex = -1;

	private String fullPath;

	private Class constructorTimeClass;

	private MethodIndividualPropertyReflector(Class clazz,
			String propertyName) {
		this.constructorTimeClass = clazz;
		methodDeclaringClass = clazz;
		/*
		 * Because we may be hitting subclass methods, we need to dynamically
		 * check object (sub)-type each time (if accessing a specific object) to
		 * see if we have the correct methods cached.
		 * 
		 * But some methods (e.g. getAnnotation) won't have a target object - so
		 * popuplate on startup
		 */
		Pattern indexedPattern = Pattern.compile("(.+)\\[(\\d+)\\]");
		Matcher m = indexedPattern.matcher(propertyName);
		if (m.matches()) {
			this.fullPath = propertyName;
			this.propertyName = m.group(1);
			this.propertyIndex = Integer.parseInt(m.group(2));
		} else {
			this.propertyName = propertyName;
		}
		PropertyDescriptor pd = SEUtilities.getPropertyDescriptorByName(clazz,
				propertyName);
		if (pd == null) {
			return;
		}
		this.readMethod = pd.getReadMethod();
		this.writeMethod = pd.getWriteMethod();
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return readMethod.getAnnotation(annotationClass);
	}

	@Override
	public Class getDefiningType() {
		return constructorTimeClass;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Class getPropertyType() {
		try {
			if (isIndexed()) {
				throw new UnsupportedOperationException();
			}
			return readMethod.getReturnType();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Class getPropertyType(Object bean) {
		try {
			ensureMethods(bean);
			return getPropertyType();
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
				if (c.iterator().next() instanceof Entity) {
					list.sort(Entity.EntityComparator.INSTANCE);
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

	public boolean isInvalid() {
		return readMethod == null && writeMethod == null;
	}

	@Override
	public boolean isReadOnly() {
		return writeMethod == null;
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

	@Override
	public String toString() {
		return Ax.format("%s.%s", constructorTimeClass.getSimpleName(),
				Optional.ofNullable(fullPath).orElse(propertyName));
	}

	private boolean isIndexed() {
		return fullPath != null;
	}

	protected void ensureMethods(Object value) throws IntrospectionException {
		Class<? extends Object> clazz = Domain
				.resolveEntityClass(value.getClass());
		if (clazz != methodDeclaringClass) {
			PropertyDescriptor pd = SEUtilities
					.getPropertyDescriptorByName(clazz, propertyName);
			if (pd == null) {
				Ax.err("No property descriptor - %s.%s", clazz.getSimpleName(),
						propertyName);
			}
			this.readMethod = pd.getReadMethod();
			this.writeMethod = pd.getWriteMethod();
			methodDeclaringClass = clazz;
		}
	}
}
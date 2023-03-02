package cc.alcina.framework.entity;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalCollection;

public class ObjectUtil {
	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections) {
		return copyBeanProperties(srcBean, tgtBean, methodFilterAnnotation,
				cloneCollections, new ArrayList<String>());
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections,
			Collection<String> ignorePropertyNames) {
		for (PropertyDescriptor targetDescriptor : SEUtilities
				.getPropertyDescriptorsSortedByName(tgtBean.getClass())) {
			if (ignorePropertyNames.contains(targetDescriptor.getName())) {
				continue;
			}
			PropertyDescriptor sourceDescriptor = SEUtilities
					.getPropertyDescriptorByName(srcBean.getClass(),
							targetDescriptor.getName());
			if (sourceDescriptor == null) {
				continue;
			}
			Method readMethod = sourceDescriptor.getReadMethod();
			if (readMethod == null) {
				continue;
			}
			if (methodFilterAnnotation != null) {
				if (readMethod.isAnnotationPresent(methodFilterAnnotation)) {
					continue;
				}
			}
			Method setMethod = targetDescriptor.getWriteMethod();
			if (setMethod != null) {
				try {
					Object obj = readMethod.invoke(srcBean, (Object[]) null);
					if (cloneCollections && obj instanceof Collection
							&& obj instanceof Cloneable) {
						Method clone = obj.getClass().getMethod("clone",
								new Class[0]);
						clone.setAccessible(true);
						obj = clone.invoke(obj, CommonUtils.EMPTY_OBJECT_ARRAY);
					}
					setMethod.invoke(tgtBean, obj);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		return tgtBean;
	}

	public static <T> T fieldwiseClone(T t) {
		return ObjectUtil.fieldwiseClone(t, false, false);
	}

	public static <T> T fieldwiseClone(T t, boolean withTransients,
			boolean withCollectionProjection) {
		try {
			T instance = newInstanceForCopy(t);
			return fieldwiseCopy(t, instance, withTransients,
					withCollectionProjection);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T fieldwiseCopy(T t, T toInstance, boolean withTransients,
			boolean withShallowCopiedCollections) {
		return fieldwiseCopy(t, toInstance, withTransients,
				withShallowCopiedCollections, null);
	}

	public static <T> T fieldwiseCopy(T t, T toInstance, boolean withTransients,
			boolean withShallowCopiedCollections,
			Set<String> ignoreFieldNames) {
		try {
			List<Field> fields = ObjectUtil.getFieldsForCopyOrLog(t,
					withTransients, ignoreFieldNames);
			for (Field field : fields) {
				Object value = field.get(t);
				boolean project = false;
				if (value != null && withShallowCopiedCollections) {
					if (value instanceof Map || value instanceof Collection) {
						project = !(value instanceof TransactionalCollection);
					}
				}
				if (project) {
					if (value instanceof Map) {
						Map map = (Map) value;
						Map newMap = (Map) map.getClass()
								.getDeclaredConstructor().newInstance();
						newMap.putAll(map);
						value = newMap;
					} else {
						Collection collection = (Collection) value;
						Collection newCollection = (Collection) newInstanceForCopy(
								collection);
						if (newCollection instanceof LiSet) {
							// handled by newInstanceForCopy/clone
						} else {
							newCollection.addAll(collection);
						}
						Preconditions.checkState(
								collection.size() == newCollection.size());
						value = newCollection;
					}
				}
				field.set(toInstance, value);
			}
			return toInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static <T> T newInstanceForCopy(T t)
			throws NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		if (t instanceof LiSet) {
			return (T) ((LiSet) t).clone();
		}
		Constructor<T> constructor = null;
		try {
			constructor = (Constructor<T>) t.getClass()
					.getConstructor(new Class[0]);
		} catch (NoSuchMethodException e) {
			constructor = (Constructor<T>) t.getClass()
					.getDeclaredConstructor(new Class[0]);
		}
		constructor.setAccessible(true);
		T instance = constructor.newInstance();
		return instance;
	}

	protected static <T> List<Field> getFieldsForCopyOrLog(T t,
			boolean withTransients, Set<String> ignoreFieldNames) {
		List<Field> result = new ArrayList<>();
		Class c = t.getClass();
		while (c != Object.class) {
			Field[] fields = c.getDeclaredFields();
			for (Field field : fields) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (Modifier.isFinal(field.getModifiers())) {
					continue;
				}
				if (Modifier.isTransient(field.getModifiers())
						&& !withTransients) {
					continue;
				}
				if (ignoreFieldNames != null
						&& ignoreFieldNames.contains(field.getName())) {
					continue;
				}
				field.setAccessible(true);
				result.add(field);
			}
			c = c.getSuperclass();
		}
		return result;
	}
}

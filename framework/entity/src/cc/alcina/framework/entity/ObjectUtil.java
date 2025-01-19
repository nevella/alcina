package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CloneHelper.FieldwiseCloner;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalCollection;
import cc.alcina.framework.entity.projection.GraphProjection;

public class ObjectUtil {
	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections) {
		return copyBeanProperties(srcBean, tgtBean, methodFilterAnnotation,
				cloneCollections, new ArrayList<String>());
	}

	public static <T> T copyBeanProperties(Object sourceBean, T targetBean,
			Class methodFilterAnnotation, boolean cloneCollections,
			Collection<String> ignorePropertyNames) {
		ClassReflector<? extends Object> targetReflector = Reflections
				.at(targetBean.getClass());
		ClassReflector<? extends Object> sourceReflector = Reflections
				.at(sourceBean.getClass());
		targetReflector.properties().stream().forEach(targetProperty -> {
			if (ignorePropertyNames.contains(targetProperty.getName())) {
				return;
			}
			if (targetProperty.isReadOnly()) {
				return;
			}
			Property sourceProperty = sourceReflector
					.property(targetProperty.getName());
			if (sourceProperty == null) {
				return;
			}
			if (sourceProperty.isWriteOnly()) {
				return;
			}
			if (methodFilterAnnotation != null
					&& sourceProperty.has(methodFilterAnnotation)) {
				return;
			}
			Object value = sourceProperty.get(sourceBean);
			if (cloneCollections && value instanceof Collection
					&& value instanceof Cloneable) {
				try {
					Method clone = value.getClass().getMethod("clone",
							new Class[0]);
					clone.setAccessible(true);
					value = clone.invoke(value, CommonUtils.EMPTY_OBJECT_ARRAY);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			targetProperty.set(targetBean, value);
		});
		return targetBean;
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

	public static class CopyObservable implements ProcessObservable {
		public Object fromInstance;

		public Object toInstance;

		public CopyObservable(Object fromInstance, Object toInstance) {
			this.fromInstance = fromInstance;
			this.toInstance = toInstance;
		}
	}

	public static <T> T fieldwiseCopy(T t, T toInstance, boolean withTransients,
			boolean withShallowCopiedCollections,
			Set<String> ignoreFieldNames) {
		try {
			new CopyObservable(t, toInstance).publish();
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

	public static String objectOrPrimitiveToString(Object object) {
		if (object == null) {
			return null;
		}
		return object.toString();
	}

	public static Map<String, String> primitiveFieldValues(Object t) {
		try {
			Map<String, String> map = new LinkedHashMap<>();
			List<Field> fields = getFieldsForCopyOrLog(t, false, null);
			for (Field field : fields) {
				if (GraphProjection.isPrimitiveOrDataClass(field.getType())) {
					Object value = field.get(t);
					map.put(field.getName(), String.valueOf(value));
				}
			}
			return map;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static Object serialClone(Object bean) {
		Object result = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(bean);
			out.close();
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(baos.toByteArray()));
			result = in.readObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return result;
	}

	public static void setField(Object object, String fieldPath,
			Object newValue) throws Exception {
		Object cursor = object;
		Field field = null;
		String[] segments = fieldPath.split("\\.");
		for (int idx = 0; idx < segments.length; idx++) {
			String segment = segments[idx];
			field = SEUtilities.getFieldByName(cursor.getClass(), segment);
			field.setAccessible(true);
			if (idx < segments.length - 1) {
				cursor = field.get(cursor);
			} else {
				field.set(cursor, newValue);
			}
		}
	}

	public static class FieldwiseClonerImpl implements FieldwiseCloner {
		@Override
		public <T> T fieldwiseClone(T source) {
			return ObjectUtil.fieldwiseClone(source);
		}
	}
}

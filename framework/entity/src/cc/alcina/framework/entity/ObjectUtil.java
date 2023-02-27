package cc.alcina.framework.entity;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;

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
}

package cc.alcina.framework.entity.domaintransform;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor.ReflectiveChainedPropertyTypeProvider;
import cc.alcina.framework.entity.SEUtilities;

public class ReflectiveChainedPropertyTypeProviderImpl
		extends ReflectiveChainedPropertyTypeProvider {
	@Override
	public Class getLookupIndexClass(Class clazz, String propertyPath) {
		String[] paths = propertyPath.split("\\.");
		Class cursor = clazz;
		for (String path : paths) {
			Field field = SEUtilities.getFieldByName(cursor, path);
			cursor = field.getType();
			if (Collection.class.isAssignableFrom(cursor)) {
				Type pt = field.getGenericType();
				if (pt instanceof ParameterizedType) {
					Type genericType = ((ParameterizedType) pt)
							.getActualTypeArguments()[0];
					if (genericType instanceof Class) {
						cursor = (Class) genericType;
					}
				}
			}
		}
		return cursor;
	}
}

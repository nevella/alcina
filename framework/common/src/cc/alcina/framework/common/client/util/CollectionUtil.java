package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;

public class CollectionUtil {
	public static <T> Class<T> getCommonType(Collection<?> collection) {
		List<Class> types = (List) collection.stream().map(Object::getClass)
				.distinct().toList();
		Preconditions.checkArgument(types.size() <= 1);
		return Ax.first(types);
	}
}

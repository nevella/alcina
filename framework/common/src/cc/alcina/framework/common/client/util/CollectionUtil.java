package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

public class CollectionUtil {
	public static <T> Class<T> getCommonType(Collection<?> collection) {
		List<Class> types = (List) collection.stream().map(Object::getClass)
				.distinct().collect(Collectors.toList());
		Preconditions.checkArgument(types.size() <= 1);
		return Ax.first(types);
	}

	public static <T> List<T> preferExisting(List<T> existing,
			List<T> incoming) {
		if (existing == null) {
			return incoming;
		}
		Map<T, T> equalityLookup = existing.stream()
				.collect(AlcinaCollectors.toKeyMap(t -> t));
		List<T> result = new ArrayList<>();
		incoming.forEach(t -> {
			T existingElement = equalityLookup.get(t);
			if (existingElement != null) {
				result.add(existingElement);
			} else {
				result.add(t);
			}
		});
		return result;
	}
}

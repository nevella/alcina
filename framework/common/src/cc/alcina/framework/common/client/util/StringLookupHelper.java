package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.KeyValueMapper.FromObjectConverterMapper;

public class StringLookupHelper {
	private static final String NULL = "<--null-->";

	public static <T> String collectionEquivalence(
			Collection<? extends T> collection,
			Converter<T, String> converter) {
		if (collection == null) {
			collection = new ArrayList<T>();
		}
		return CommonUtils.join(generateLookup(collection, converter).keySet(),
				"\n");
	}

	public static String equivalenceString(Object... args) {
		StringBuilder sb = new StringBuilder();
		for (Object object : args) {
			sb.append(object == null ? NULL : object.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static <T> Multimap<String, List<T>> generateLookup(
			Collection<? extends T> collection,
			Converter<T, String> converter) {
		return CollectionFilters.multimap(collection,
				new FromObjectConverterMapper(converter));
	}

	public static <T> Collection<T> intersection(Collection<T> o1,
			Collection<T> o2, Converter<T, String> converter) {
		Multimap<String, List<T>> l1 = generateLookup(o1, converter);
		Multimap<String, List<T>> l2 = generateLookup(o2, converter);
		l1.keySet().retainAll(l2.keySet());
		return l1.allItems();
	}

	public static String
			nullSafeEqString(HasEquivalenceString hasEquivalenceString) {
		return hasEquivalenceString == null ? NULL
				: hasEquivalenceString.equivalenceString();
	}

	public static <T> Collection<T> removeAll(Collection<T> o1,
			Collection<T> o2, Converter<T, String> converter) {
		Multimap<String, List<T>> l1 = generateLookup(o1, converter);
		Multimap<String, List<T>> l2 = generateLookup(o2, converter);
		l1.keySet().removeAll(l2.keySet());
		return l1.allItems();
	}
}
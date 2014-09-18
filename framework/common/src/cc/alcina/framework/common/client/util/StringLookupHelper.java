package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.KeyValueMapper.FromObjectConverterMapper;
import cc.alcina.framework.common.client.util.HasEquivalenceString.HasEquivalenceStringConverter;

import com.totsp.gwittir.client.beans.Converter;

public class StringLookupHelper {
	public static <T> String collectionEquivalence(Collection<? extends T> collection,
			Converter<T, String> converter) {
		if (collection == null) {
			collection = new ArrayList<T>();
		}
		return CommonUtils.join(generateLookup(collection, converter).keySet(),
				"\n");
	}

	public static String nullSafeEqString(
			HasEquivalenceString hasEquivalenceString) {
		return hasEquivalenceString == null ? "<null>" : hasEquivalenceString
				.equivalenceString();
	}

	public static <T> Map<String, T> generateLookup(Collection<? extends T> collection,
			Converter<T, String> converter) {
		return CollectionFilters.map(collection, new FromObjectConverterMapper(
				converter));
	}

	public static <T> Collection<T> intersection(Collection<T> o1,
			Collection<T> o2, Converter<T, String> converter) {
		Map<String, T> l1 = generateLookup(o1, converter);
		Map<String, T> l2 = generateLookup(o2, converter);
		l1.keySet().retainAll(l2.keySet());
		return l1.values();
	}

	public static <T> Collection<T> removeAll(Collection<T> o1,
			Collection<T> o2, Converter<T, String> converter) {
		Map<String, T> l1 = generateLookup(o1, converter);
		Map<String, T> l2 = generateLookup(o2, converter);
		l1.keySet().removeAll(l2.keySet());
		return l1.values();
	}
}
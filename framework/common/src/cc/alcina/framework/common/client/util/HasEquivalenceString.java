package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.totsp.gwittir.client.beans.Converter;

public interface HasEquivalenceString<T> extends HasEquivalence<T> {
	String NULL = "<--null-->";

	public static <T extends HasEquivalenceString> Map<String, T>
			toEquivalanceStringMap(Stream<T> stream) {
		return stream.collect(AlcinaCollectors
				.toKeyMap(HasEquivalenceString::equivalenceString));
	}

	static <T> String collectionEquivalenceString(
			Collection<? extends T> collection, Function<T, String> mapper) {
		if (collection == null) {
			collection = new ArrayList<T>();
		}
		return collection.stream().map(mapper)
				.collect(Collectors.joining("\n"));
	}

	static String equivalenceStringArray(Object... args) {
		StringBuilder sb = new StringBuilder();
		for (Object object : args) {
			sb.append(object == null ? NULL : object.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	static String nullSafeEquivalenceString(
			HasEquivalenceString hasEquivalenceString) {
		return hasEquivalenceString == null ? HasEquivalenceString.NULL
				: hasEquivalenceString.equivalenceString();
	}

	@Override
	default public int equivalenceHash() {
		return Objects.hashCode(equivalenceString());
	}

	public String equivalenceString();

	@Override
	default public boolean equivalentTo(T other) {
		return equivalenceString()
				.equals(((HasEquivalenceString<T>) other).equivalenceString());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface HasEquivalenceInfo {
		String value();
	}

	public static class HasEquivalenceStringMapper
			implements Function<HasEquivalenceString, String> {
		@Override
		public String apply(HasEquivalenceString original) {
			return original.equivalenceString();
		}
	}

	public class StringLookup {
		public static <T> Multimap<String, List<T>> generateLookup(
				Collection<? extends T> collection,
				Converter<T, String> converter) {
			return collection.stream()
					.collect(AlcinaCollectors.toKeyMultimap(converter));
		}

		public static <T> Collection<T> intersection(Collection<T> o1,
				Collection<T> o2, Converter<T, String> converter) {
			Multimap<String, List<T>> l1 = generateLookup(o1, converter);
			Multimap<String, List<T>> l2 = generateLookup(o2, converter);
			l1.keySet().retainAll(l2.keySet());
			return l1.allValues();
		}

		public static <T> Collection<T> removeAll(Collection<T> o1,
				Collection<T> o2, Converter<T, String> converter) {
			Multimap<String, List<T>> l1 = generateLookup(o1, converter);
			Multimap<String, List<T>> l2 = generateLookup(o2, converter);
			l1.keySet().removeAll(l2.keySet());
			return l1.allValues();
		}
	}
}

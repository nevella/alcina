package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;

public class Ax {
	private static boolean test;

	public static String blankTo(String string, String defaultValue) {
		return isBlank(string) ? defaultValue : string;
	}

	public static String blankTo(String string,
			Supplier<String> defaultValueSupplier) {
		return isBlank(string) ? defaultValueSupplier.get() : string;
	}

	public static String blankToEmpty(String string) {
		return blankTo(string, "");
	}

	public static String clip(String string, int maxLength) {
		if (string == null) {
			return null;
		}
		if (string.length() <= maxLength) {
			return string;
		}
		return string.substring(0, maxLength);
	}

	public static Date cloneDate(Date date) {
		return date == null ? null : new Date(date.getTime());
	}

	public static String commaJoin(Collection collection) {
		return (String) collection.stream().map(Object::toString)
				.collect(Collectors.joining(", "));
	}

	public static String cssify(String s) {
		if (CommonUtils.isNullOrEmpty(s)) {
			return s;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(s.substring(0, 1).toLowerCase());
		for (int i = 1; i < s.length(); i++) {
			String c = s.substring(i, i + 1);
			builder.append(c.toUpperCase().equals(c) ? "-" : "");
			builder.append(c.toLowerCase());
		}
		return builder.toString();
	}

	public static String dateSlash(Date date) {
		return CommonUtils.formatDate(date, DateStyle.AU_DATE_SLASH);
	}

	public static String dateTimeSlash(Date date) {
		return CommonUtils.formatDate(date, DateStyle.AU_DATE_TIME);
	}

	public static String displayName(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof HasDisplayName) {
			return ((HasDisplayName) object).displayName();
		}
		return friendly(object);
	}

	public static void err(Object object) {
		System.err.println(object);
	}

	public static void err(String template, Object... args) {
		System.err.println(format(template, args));
	}

	public static <T> T first(Collection<T> collection) {
		return collection.size() == 0 ? null
				: collection instanceof List ? ((List<T>) collection).get(0)
						: collection.iterator().next();
	}

	public static <T> Optional<T> firstOptional(Collection<T> collection) {
		return collection.size() == 0 ? Optional.empty()
				: Optional.ofNullable(collection.iterator().next());
	}

	public static String format(String template, Object... args) {
		return CommonUtils.format(template, args);
	}

	public static double fourPlaces(double d) {
		return CommonUtils.roundNumeric(d, 4);
	}

	public static String friendly(Object o) {
		return CommonUtils.friendlyConstant(o);
	}

	public static boolean isBlank(String string) {
		return CommonUtils.isNullOrEmpty(string);
	}

	public static boolean isNull(Object object) {
		return object == null;
	}

	public static boolean isTest() {
		return test;
	}

	public static <T> BinaryOperator<T> last() {
		return (a, b) -> b;
	}

	public static <T> T last(List<T> list) {
		return CommonUtils.last(list);
	}

	public static int length(double[] array) {
		return array.length;
	}

	public static int length(Object[] array) {
		return array.length;
	}

	public static boolean matches(String test, String regex) {
		if (test == null || regex == null) {
			return false;
		}
		if (regex.equals(".*")) {
			return true;
		}
		return test.matches(regex);
	}

	public static void newlineDump(Collection collection) {
		System.out.println(CommonUtils.joinWithNewlines(collection));
	}

	public static <T> T next(Collection<T> collection, T element) {
		Iterator<T> itr = collection.iterator();
		boolean matched = false;
		while (itr.hasNext()) {
			T next = itr.next();
			if (matched) {
				return next;
			} else {
				matched = Objects.equals(next, element);
			}
		}
		return null;
	}

	public static <T> T next(List<T> list, T element) {
		int index = list.indexOf(element);
		if (index == -1 || index + 1 == list.size()) {
			return null;
		} else {
			return list.get(index + 1);
		}
	}

	public static boolean notBlank(String string) {
		return !isBlank(string);
	}

	public static boolean notPresent(Optional<?> optional) {
		return !optional.isPresent();
	}

	public static String ntrim(String s) {
		return TextUtils.normalizeWhitespaceAndTrim(s);
	}

	public static String nullSafe(String string) {
		return string == null ? "" : string;
	}

	public static <T> T nullTo(T t, T ifNull) {
		return t == null ? ifNull : t;
	}

	public static void out(Object o) {
		if (o instanceof Collection) {
			System.out.println(CommonUtils.joinWithNewlines((Collection) o));
		} else {
			System.out.println(o);
		}
	}

	public static void out(String template, Object... args) {
		System.out.println(format(template, args));
	}

	public static <E> Iterator<E> reversedIterator(List<E> list) {
		ListIterator<E> itr = list.listIterator();
		while (itr.hasNext()) {
			itr.next();
		}
		return new ReversedListIterator(itr);
	}

	public static <E> Stream<E> reversedStream(List<E> list) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
				reversedIterator(list), Spliterator.ORDERED), false);
	}

	public static RuntimeException runtimeException(String template,
			Object... args) {
		return new RuntimeException(format(template, args));
	}

	public static void setTest(boolean test) {
		Ax.test = test;
	}

	public static void simpleExceptionOut(Throwable t) {
		System.err.println(CommonUtils.toSimpleExceptionMessage(t));
	}

	public static void sysLogHigh(String template, Object... args) {
		System.out.println(CommonUtils.highlightForLog(template, args));
	}

	public static String timestamp(Date date) {
		return CommonUtils.formatDate(date, DateStyle.AU_DATE_TIME_MS);
	}

	public static String timestampYmd(Date date) {
		return CommonUtils.formatDate(date, DateStyle.TIMESTAMP);
	}

	public static String transforms() {
		return TransformManager.get().getTransforms().toString();
	}

	public static String trim(String s, int maxChars) {
		return CommonUtils.trimToWsChars(s, maxChars, true);
	}

	public static double twoPlaces(double d) {
		return CommonUtils.roundNumeric(d, 2);
	}

	private static class ReversedListIterator<E> implements Iterator<E> {
		private ListIterator<E> listIterator;

		public ReversedListIterator(ListIterator<E> listIterator) {
			this.listIterator = listIterator;
		}

		@Override
		public boolean hasNext() {
			return listIterator.hasPrevious();
		}

		@Override
		public E next() {
			return listIterator.previous();
		}

		@Override
		public void remove() {
			listIterator.remove();
		}
	}
}

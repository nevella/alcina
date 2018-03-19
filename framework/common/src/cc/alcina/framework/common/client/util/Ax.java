package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;

public class Ax {
	public static AxStreams streams = new AxStreams();
	
	

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

	public static Date cloneDate(Date date) {
		return date == null ? null : new Date(date.getTime());
	}

	public static String dateSlash(Date date) {
		return CommonUtils.formatDate(date, DateStyle.AU_DATE_SLASH);
	}

	public static String dateTimeSlash(Date date) {
		return CommonUtils.formatDate(date, DateStyle.AU_DATE_TIME);
	}

	public static void err(String template, Object... args) {
		System.err.println(format(template, args));
	}

	public static <T> Optional<T> first(Collection<T> collection) {
		return collection.size() == 0 ? Optional.empty()
				: Optional.of(collection.iterator().next());
	}

	public static String format(String template, Object... args) {
		return CommonUtils.formatJ(template, args);
	}

	public static String friendly(Object o) {
		return CommonUtils.friendlyConstant(o);
	}

	public static boolean isBlank(String string) {
		return CommonUtils.isNullOrEmpty(string);
	}

	public static <T> T last(List<T> list) {
		return CommonUtils.last(list);
	}

	public static void newlineDump(Collection collection) {
		System.out.println(CommonUtils.joinWithNewlines(collection));
	}

	public static boolean notBlank(String string) {
		return !isBlank(string);
	}

	public static String nullSafe(String string) {
		return string == null ? "" : string;
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

	public static void runtimeException(String template, Object... args) {
		throw new RuntimeException(format(template, args));
	}

	public static void sysLogHigh(String template, Object... args) {
		System.out.println(CommonUtils.highlightForLog(template, args));
	}

	public static class AxStreams {
		public <T> Function<T, T> visit(Consumer<T> consumer) {
			return t -> {
				consumer.accept(t);
				return t;
			};
		}
	}

	private static boolean test;
	public static boolean isTest() {
		return test;
	}

	public static void setTest(boolean test) {
		Ax.test = test;
	}

	public static void err(Object object) {
		System.err.println(object);
	}
}

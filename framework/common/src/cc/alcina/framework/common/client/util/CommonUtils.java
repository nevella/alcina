/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class CommonUtils {
	public static final String XML_PI = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	// for GWT reflection gets, this gets used...a lot
	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	public static final String[] MONTH_NAMES = { "invalid", "January",
			"February", "March", "April", "May", "June", "July", "August",
			"September", "October", "November", "December" };

	public static final String[] DAY_NAMES = { "Sunday", "Monday", "Tuesday",
			"Wednesday", "Thursday", "Friday", "Saturday" };

	private static final Map<String, Class> stdClassMap = new HashMap<String, Class>();
	static {
		Class[] stds = { Long.class, Double.class, Float.class, Short.class,
				Byte.class, Integer.class, Boolean.class, Character.class,
				Date.class, String.class };
		for (Class std : stds) {
			stdClassMap.put(std.getName(), std);
		}
	}

	private static final Map<String, Class> primitiveClassMap = new HashMap<String, Class>();
	static {
		Class[] prims = { long.class, int.class, short.class, char.class,
				byte.class, boolean.class, double.class, float.class };
		for (Class prim : prims) {
			primitiveClassMap.put(prim.getName(), prim);
		}
	}

	public static final Map<String, Class> stdAndPrimitivesMap = new HashMap<String, Class>();
	static {
		stdAndPrimitivesMap.putAll(stdClassMap);
		stdAndPrimitivesMap.putAll(primitiveClassMap);
	}

	public static final Set<Class> stdAndPrimitives = new HashSet<Class>(
			stdAndPrimitivesMap.values());

	public static Supplier<Set> setSupplier = () -> new LinkedHashSet();

	/**
	 * For trimming a utf8 string for insertion into a 255-char varchar db
	 * field. Oh, the pain
	 */
	public static final int SAFE_VARCHAR_MAX_CHARS = 230;

	private static UnsortedMultikeyMap<Enum> enumValueLookup = new UnsortedMultikeyMap<Enum>(
			2);

	private static Set<String> done = new LinkedHashSet<>();

	// https://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style/Titles
	private static Set<String> standardLowercaseEnglish = Arrays
			// not exactly - can imagine "like" in a company name
			// "A,An,The,And,But,Or,Nor,For,Yet,So,As,In,Of,On,To,For,From,Into,Like,Over,With,Upon"
			.stream("A,An,The,And,But,Or,Nor,For,Yet,So,As,In,Of,On,To,For,From,Into,LikeExcluded,Over,With,Upon"
					.split(","))
			.collect(Collectors.toSet());

	static Logger logger = LoggerFactory.getLogger(CommonUtils.class);

	public static void addIfNotNull(List l, Object o) {
		if (o != null) {
			l.add(o);
		}
	}

	public static String applyStandardTitleCaseLowercaseRules(String string) {
		String[] words = string.split(" ");
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < words.length; idx++) {
			if (idx > 0) {
				sb.append(" ");
			}
			String word = words[idx];
			if (idx == 0 || idx == words.length - 1) {
			} else {
				if (standardLowercaseEnglish.contains(word)) {
					word = word.toLowerCase();
				}
			}
			sb.append(word);
		}
		return sb.toString();
	}

	public static String buildSpaceSeparatedStrings(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			if (CommonUtils.isNotNullOrEmpty(string)) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(string.trim());
			}
		}
		return sb.toString();
	}

	public static boolean bv(Boolean b) {
		return b == null || b == false ? false : true;
	}

	public static String capitaliseFirst(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static String classSimpleName(Class c) {
		return c.getName().substring(c.getName().lastIndexOf('.') + 1);
	}

	public static Date cloneDate(Date date) {
		return date == null ? null : new Date(date.getTime());
	}

	public static boolean closeDates(Date d1, Date d2, long ms) {
		if (d1 == null || d2 == null) {
			return d1 == d2;
		}
		return Math.abs(d1.getTime() - d2.getTime()) < ms;
	}

	public static int compareBoolean(Boolean o1, Boolean o2) {
		int i = 0;
		if (bv(o1)) {
			i++;
		}
		if (bv(o2)) {
			i--;
		}
		return i;
	}

	public static int compareDates(Date d1, Date d2) {
		long t1 = d1 == null ? 0 : d1.getTime();
		long t2 = d2 == null ? 0 : d2.getTime();
		return t1 < t2 ? -1 : t1 == t2 ? 0 : 1;
	}

	public static int compareDatesNullHigh(Date d1, Date d2) {
		long t1 = d1 == null ? Long.MAX_VALUE : d1.getTime();
		long t2 = d2 == null ? Long.MAX_VALUE : d2.getTime();
		return t1 < t2 ? -1 : t1 == t2 ? 0 : 1;
	}

	public static int compareDoubles(double d1, double d2) {
		return (d1 < d2 ? -1 : (d1 == d2 ? 0 : 1));
	}

	public static int compareFloats(float f1, float f2) {
		return (f1 < f2 ? -1 : (f1 == f2 ? 0 : 1));
	}

	public static int compareIgnoreCaseWithNullMinusOne(String o1, String o2) {
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		}
		return o2 == null ? 1 : o1.compareToIgnoreCase(o2);
	}

	public static int compareInts(int i1, int i2) {
		return (i1 < i2 ? -1 : (i1 == i2 ? 0 : 1));
	}

	public static int compareLongs(long l1, long l2) {
		return (l1 < l2 ? -1 : (l1 == l2 ? 0 : 1));
	}

	public static int compareNonNull(Object o1, Object o2) {
		int i = 0;
		if (o1 != null) {
			i++;
		}
		if (o2 != null) {
			i--;
		}
		return i;
	}

	public static ComparatorResult compareNullCheck(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return ComparatorResult.BOTH_NULL;
		}
		if (o1 == null) {
			return ComparatorResult.FIRST_NULL;
		}
		if (o2 == null) {
			return ComparatorResult.SECOND_NULL;
		}
		return ComparatorResult.BOTH_NON_NULL;
	}

	public static int compareWithNullMinusOne(Comparable o1, Comparable o2) {
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		}
		return o2 == null ? 1 : o1.compareTo(o2);
	}

	public static boolean containsAny(Collection container,
			Collection containees) {
		for (Iterator itr = containees.iterator(); itr.hasNext();) {
			if (container.contains(itr.next())) {
				return true;
			}
		}
		return false;
	}

	public static boolean containsWithNull(Object obj, String lcText) {
		if (obj == null || lcText == null) {
			return false;
		}
		String string = obj.toString();
		return string != null && string.toLowerCase().contains(lcText);
	}

	public static int countOccurrences(String content, String occurrence) {
		int result = 0;
		int idx = 0;
		while (true) {
			idx = content.indexOf(occurrence, idx);
			if (idx == -1) {
				break;
			}
			result++;
			idx += occurrence.length();
		}
		return result;
	}

	public static boolean currencyEquals(double d1, double d2) {
		return Math.abs(d1 - d2) < 0.005;
	}

	@SuppressWarnings("deprecation")
	public static String dateStampMillis() {
		Date d = new Date();
		return format("%s%s%s%s%s%s", padFour(d.getYear() + 1900),
				padTwo(d.getMonth() + 1), padTwo(d.getDate()),
				padTwo(d.getHours()), padTwo(d.getMinutes()),
				padTwo(d.getSeconds()), padThree((int) (d.getTime() % 1000)));
	}

	public static List dedupe(List objects) {
		return new ArrayList(new LinkedHashSet(objects));
	}

	public static void dedupeInPlace(List list) {
		List mod = dedupe(list);
		list.clear();
		list.addAll(mod);
	}

	public static String deInfix(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			String c = s.substring(i, i + 1);
			buf.append(c.toUpperCase().equals(c) ? " " : "");
			buf.append(i == 0 ? c.toUpperCase() : c.toLowerCase());
		}
		return buf.toString();
	}

	public static void doOnce(Class clazz, Runnable runnable) {
		doOnce(clazz, null, runnable);
	}

	public static void doOnce(Class clazz, String key, Runnable runnable) {
		if (done.add(format("%s::%s", clazz.getName(), key))) {
			runnable.run();
		}
	}

	public static void dumpAround(String text, String subString) {
		int idx = text.indexOf(subString);
		if (idx != -1) {
			System.out.println(
					"before:" + text.substring(Math.max(0, idx - 100), idx));
			System.out.println(subString);
			System.out.println("after: " + text.substring(
					idx + subString.length(),
					Math.min(idx + subString.length() + 100, text.length())));
		}
	}

	public static double dv(Double d) {
		return d == null ? 0.0 : d.doubleValue();
	}

	public static int elideList(List list, int maxElements) {
		int elidedCount = 0;
		while (list.size() > maxElements) {
			list.remove(maxElements / 2);
			elidedCount++;
		}
		return elidedCount;
	}

	public static String ellipsisText(String sourceText, int charWidth) {
		if (sourceText.length() < charWidth) {
			return sourceText;
		}
		String result = trimToWsChars(sourceText, (charWidth * 2) / 3, true);
		int from = sourceText.length() - result.length();
		int spIdx = sourceText.substring(0, from).lastIndexOf(" ");
		if (spIdx != -1) {
			result += sourceText.substring(spIdx);
		}
		return result;
	}

	public static Date ensureMonthOfDate(Date date) {
		// called only for dates which might be slightly before start of month
		// because of tz offsets - i.e. mm/01 on server
		return new Date(date.getTime() + TimeConstants.ONE_DAY_MS);
	}

	public static String enumStringRep(Enum e) {
		if (e == null) {
			return null;
		}
		return e.getDeclaringClass().getName() + "." + e.toString();
	}

	public static boolean equals(Object... objects) {
		if (objects.length % 2 != 0) {
			throw new RuntimeException("Array length must be divisible by two");
		}
		for (int i = 0; i < objects.length; i += 2) {
			Object o1 = objects[i];
			Object o2 = objects[i + 1];
			if (o1 == null && o2 == null) {
			} else {
				if (o1 == null || o2 == null) {
					return false;
				} else {
					if (!o1.equals(o2)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static boolean equalsIgnoreCase(String s1, String s2) {
		if (s1 == s2) {
			return true;
		}
		if (s1 == null || s2 == null) {
			return false;
		}
		return s1.toLowerCase().equals(s2.toLowerCase());
	}

	public static boolean equalsWithForgivingDates(Object... objects) {
		if (objects.length % 2 != 0) {
			throw new RuntimeException("Array length must be divisible by two");
		}
		for (int i = 0; i < objects.length; i += 2) {
			Object o1 = objects[i];
			Object o2 = objects[i + 1];
			if (o1 == null && o2 == null) {
			} else {
				if (o1 == null || o2 == null) {
					Object nonNull = o1 == null ? o2 : o1;
					return false;
				} else {
					if (!o1.equals(o2)) {
						if (o1 instanceof Date && o2 instanceof Date
								&& Math.abs(((Date) o1).getTime() - ((Date) o2)
										.getTime()) < TimeConstants.ONE_DAY_MS) {
						} else {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public static boolean equalsWithForgivingStrings(Object... objects) {
		if (objects.length % 2 != 0) {
			throw new RuntimeException("Array length must be divisible by two");
		}
		for (int i = 0; i < objects.length; i += 2) {
			Object o1 = objects[i];
			Object o2 = objects[i + 1];
			if (o1 == null && o2 == null) {
			} else {
				if (o1 == null || o2 == null) {
					Object nonNull = o1 == null ? o2 : o1;
					if (nonNull instanceof String
							&& nonNull.toString().trim().length() == 0) {
						// keep going
					} else {
						return false;
					}
				} else {
					if (!o1.equals(o2)) {
						if (o1 instanceof String && o2 instanceof String
								&& o1.toString().trim().toLowerCase().equals(
										o2.toString().trim().toLowerCase())) {
						} else {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public static boolean equalsWithNullEmptyEquality(Object o1, Object o2) {
		if (o1 instanceof String && o1.toString().isEmpty()) {
			o1 = null;
		}
		if (o2 instanceof String && o2.toString().isEmpty()) {
			o2 = null;
		}
		if (o1 instanceof Collection && ((Collection) o1).isEmpty()) {
			o1 = null;
		}
		if (o2 instanceof Collection && ((Collection) o2).isEmpty()) {
			o2 = null;
		}
		if (o1 == null) {
			return o2 == null;
		}
		return o1.equals(o2);
	}

	public static boolean equalsWithNullEquality(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		return o1.equals(o2);
	}

	public static String escapeRegex(String s) {
		if (s.contains("\\")) {
			throw new RuntimeException("can't escape escaped strings");
		}
		s = s.replace("(", "\\(");
		s = s.replace(")", "\\)");
		s = s.replace("]", "\\]");
		s = s.replace("[", "\\[");
		s = s.replace("$", "\\$");
		s = s.replace("+", "\\+");
		s = s.replace("?", "\\?");
		s = s.replace(".", "\\.");
		return s;
	}

	public static <T extends Throwable> T
			extractCauseOfClass(Throwable throwable, Class<T> throwableClass) {
		while (true) {
			if (isDerivedFrom(throwable, throwableClass)) {
				return (T) throwable;
			}
			if (throwable.getCause() == throwable
					|| throwable.getCause() == null) {
				return null;
			}
			throwable = throwable.getCause();
		}
	}

	public static String extractHostAndPort(String url) {
		return url.replaceFirst("(.+?://.+?)(/.+)", "$1");
	}

	public static <T> T first(Collection<T> coll) {
		if (coll != null && coll.iterator().hasNext()) {
			return coll.iterator().next();
		}
		return null;
	}

	public static <T> T first(Iterator<T> itr) {
		return itr.hasNext() ? itr.next() : null;
	}

	public static String firstNonEmpty(String... strings) {
		return Arrays.asList(strings).stream()
				.filter(s -> CommonUtils.isNotNullOrEmpty(s)).findFirst()
				.orElse(null);
	}

	public static List flattenMap(Map m) {
		List result = new ArrayList();
		for (Entry e : (Collection<Map.Entry>) m.entrySet()) {
			result.add(e.getKey());
			result.add(e.getValue());
		}
		return result;
	}

	public static String format(String source, Object... args) {
		if (source == null) {
			return null;
		}
		if (args.length == 0) {
			return source;
		}
		boolean modSource = source.endsWith("%s");
		String s2 = modSource ? source + "." : source;
		String[] strs = s2.split("%s");
		String s;
		for (int i = 1; i < strs.length; i++) {
			strs[i] = args[i - 1]
					+ ((modSource && i == strs.length - 1) ? "" : strs[i]);
		}
		return join(strs, "");
	}

	public static String formatDate(Date date, DateStyle style) {
		return formatDate(date, style, " ");
	}

	@SuppressWarnings("deprecation")
	public static String formatDate(Date date, DateStyle style,
			String nullMarker) {
		if (date == null) {
			return nullMarker;
		}
		switch (style) {
		case AU_DATE_SLASH:
			return format("%s/%s/%s", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900));
		case US_DATE_SLASH:
			return format("%s/%s/%s", padTwo(date.getMonth() + 1),
					padTwo(date.getDate()), padTwo(date.getYear() + 1900));
		case AU_DATE_SLASH_MONTH:
			return format("%s/%s", padTwo(date.getMonth() + 1),
					padTwo(date.getYear() + 1900));
		case AU_DATE_DOT:
			return format("%s.%s.%s", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900));
		case AU_DATE_TIME:
			return format("%s/%s/%s - %s:%s:%s", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()));
		case AU_DATE_TIME_HUMAN:
			return formatDate(date, DateStyle.AU_LONG_DAY) + format(
					" at %s:%s %s", padTwo((date.getHours() - 1) % 12 + 1),
					padTwo(date.getMinutes()),
					date.getHours() < 12 ? "AM" : "PM");
		case NAMED_MONTH_DATE_TIME_HUMAN:
			return formatDate(date, DateStyle.NAMED_MONTH_DAY) + format(
					" at %s:%s %s", padTwo((date.getHours() - 1) % 12 + 1),
					padTwo(date.getMinutes()),
					date.getHours() < 12 ? "AM" : "PM");
		case NAMED_MONTH_DAY:
			return format("%s, %s %s %s", DAY_NAMES[date.getDay()],
					MONTH_NAMES[date.getMonth() + 1], padTwo(date.getDate()),
					padTwo(date.getYear() + 1900));
		case AU_DATE_TIME_MS:
			return format("%s/%s/%s - %s:%s:%s:%s", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()), date.getTime() % 1000);
		case AU_DATE_MONTH:
			return format("%s %s %s", padTwo(date.getDate()),
					MONTH_NAMES[date.getMonth() + 1],
					padTwo(date.getYear() + 1900));
		case AU_DATE_MONTH_NO_PAD_DAY:
			return format("%s %s %s", date.getDate(),
					MONTH_NAMES[date.getMonth() + 1],
					padTwo(date.getYear() + 1900));
		case AU_DATE_MONTH_DAY:
			return format("%s %s, %s", MONTH_NAMES[date.getMonth() + 1],
					padTwo(date.getDate()), padTwo(date.getYear() + 1900));
		case AU_SHORT_MONTH:
			return format("%s %s %s", date.getDate(),
					MONTH_NAMES[date.getMonth() + 1].substring(0, 3),
					padTwo(date.getYear() + 1900));
		case AU_SHORT_MONTH_SLASH:
			return format("%s/%s/%s", padTwo(date.getDate()),
					MONTH_NAMES[date.getMonth() + 1].substring(0, 3),
					padTwo(date.getYear() + 1900));
		case AU_SHORT_DAY:
			return format("%s - %s.%s.%s",
					DAY_NAMES[date.getDay()].substring(0, 3),
					padTwo(date.getDate()), padTwo(date.getMonth() + 1),
					padTwo(date.getYear() + 1900));
		case AU_LONG_DAY:
			return format("%s, %s.%s.%s", DAY_NAMES[date.getDay()],
					padTwo(date.getDate()), padTwo(date.getMonth() + 1),
					padTwo(date.getYear() + 1900));
		case TIMESTAMP:
			return format("%s%s%s_%s%s%s_%s", padTwo(date.getYear() + 1900),
					padTwo(date.getMonth() + 1), padTwo(date.getDate()),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()), date.getTime() % 1000);
		case TIMESTAMP_HUMAN:
			return format("%s.%s.%s %s:%s:%s", padTwo(date.getYear() + 1900),
					padTwo(date.getMonth() + 1), padTwo(date.getDate()),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()));
		case TIMESTAMP_NO_DAY:
			return format("%s:%s:%s,%s", padTwo(date.getHours()),
					padTwo(date.getMinutes()), padTwo(date.getSeconds()),
					padThree((int) (date.getTime() % 1000)));
		case AU_SHORT_MONTH_NO_DAY:
			return format("%s %s",
					MONTH_NAMES[date.getMonth() + 1].substring(0, 3),
					padTwo(date.getYear() + 1900));
		case AU_DATE_TIME_SHORT:
			return format("%s/%s/%s - %s:%s:%s", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()));
		}
		return date.toString();
	}

	public static String formatNumbered(String source, Object... args) {
		String[] strs = source.split("%");
		String s;
		for (int i = 0; i < strs.length; i++) {
			s = strs[i];
			if (i != 0) {
				strs[i] = args[Integer.parseInt(s.substring(0, 1)) - 1]
						+ s.substring(1);
			}
		}
		return join(strs, "");
	}

	public static void formatOut(String string, Object... objects) {
		System.out.println(format(string, objects));
	}

	public static String friendlyConstant(Object o) {
		return friendlyConstant(o, " ");
	}

	public static String friendlyConstant(Object o, String sep) {
		if (o == null) {
			return "";
		}
		String s = capitaliseFirst(o.toString().toLowerCase());
		int x = 0, y = 0;
		while (true) {
			y = s.indexOf("__");
			if (y != -1) {
				s = s.substring(0, x) + ((x == 0) ? "" : sep)
						+ s.substring(x, y).toUpperCase() + sep
						+ s.substring(y + 2);
				x = y + 2;
			} else {
				break;
			}
		}
		return s.replace("_", sep).trim();
	}

	public static Integer friendlyParseInt(String toParse) {
		String sub = getNumericSubstring(toParse);
		return sub == null ? null : Integer.parseInt(sub);
	}

	public static Long friendlyParseLong(String toParse) {
		String sub = getNumericSubstring(toParse);
		return sub == null ? null : Long.parseLong(sub);
	}

	public static <T> T get(Iterator<T> iterator, int index) {
		T last = null;
		while (iterator.hasNext()) {
			last = iterator.next();
			if (index-- == 0) {
				return last;
			}
		}
		return null;
	}

	public static <E extends Enum> E getEnumValueOrNull(Class<E> enumClass,
			String value) {
		return getEnumValueOrNull(enumClass, value, false, null);
	}

	public static <E extends Enum> E getEnumValueOrNull(Class<E> enumClass,
			String value, boolean withFriendlyNames, E defaultValue) {
		if (!enumValueLookup.containsKey(enumClass)) {
			for (E ev : enumClass.getEnumConstants()) {
				enumValueLookup.put(enumClass, ev.toString(), ev);
				enumValueLookup.put(enumClass, ev.toString().toLowerCase(), ev);
				if (withFriendlyNames) {
					enumValueLookup.put(enumClass,
							friendlyConstant(ev, "-").toLowerCase(), ev);
					enumValueLookup.put(enumClass, friendlyConstant(ev, "-"),
							ev);
					enumValueLookup.put(enumClass, friendlyConstant(ev, " "),
							ev);
					enumValueLookup.put(enumClass,
							friendlyConstant(ev, " ").toLowerCase(), ev);
				}
			}
		}
		E result = (E) enumValueLookup.get(enumClass, value);
		return result == null ? defaultValue : result;
	}

	public static String getSimpleTimeBefore(Date d) {
		return getSimpleTimeBefore(d, new Date());
	}

	public static String getSimpleTimeBefore(Date d, Date currentDate) {
		long timeDiff = (long) Math.ceil(
				(double) (currentDate.getTime() - d.getTime()) / (60 * 1000));
		if (timeDiff < 60) {
			return timeDiff + " minutes ago";
		}
		if (timeDiff < 24 * 60) {
			return Math.round(timeDiff / 60) + " hours ago";
		}
		return Math.round(timeDiff / (60 * 24)) + " days ago";
	}

	public static String getUniqueNumberedString(String base,
			String postfixTemplate, Collection<String> existingValues) {
		if (!existingValues.contains(base)) {
			return base;
		}
		int i = 1;
		while (true) {
			String value = base + format(postfixTemplate, i++);
			if (!existingValues.contains(value)) {
				return value;
			}
		}
	}

	public static Class getWrapperType(Class clazz) {
		if (!clazz.isPrimitive()) {
			return clazz;
		}
		if (clazz == int.class) {
			return Integer.class;
		}
		if (clazz == long.class) {
			return Long.class;
		}
		if (clazz == double.class) {
			return Double.class;
		}
		if (clazz == boolean.class) {
			return Boolean.class;
		}
		if (clazz == short.class) {
			return Short.class;
		}
		if (clazz == float.class) {
			return Float.class;
		}
		if (clazz == byte.class) {
			return Byte.class;
		}
		if (clazz == char.class) {
			return Character.class;
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public static int getYear(Date d) {
		return d.getYear() + 1900;
	}

	public static String hangingIndent(String text, boolean noTabsFirstLine,
			int tabs) {
		StringBuilder result = new StringBuilder();
		String[] lines = text.split("\n");
		for (int i = 0; i < lines.length; i++) {
			if (noTabsFirstLine && i == 0) {
			} else {
				for (int j = 0; j < tabs; j++) {
					result.append("\t");
				}
			}
			result.append(lines[i]);
			if (i < lines.length - 1) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	public static <T extends Throwable> boolean
			hasCauseOfClass(Throwable throwable, Class<T> throwableClass) {
		return extractCauseOfClass(throwable, throwableClass) != null;
	}

	public static boolean hasCauseOfClass(Throwable throwable,
			CollectionFilter<Throwable> causeFilter) {
		while (true) {
			if (causeFilter.allow(throwable)) {
				return true;
			}
			if (throwable.getCause() == throwable
					|| throwable.getCause() == null) {
				return false;
			}
			throwable = throwable.getCause();
		}
	}

	public static String highlightForLog(String template, Object... args) {
		String inner = format(template, args);
		String star = padStringLeft("", 40, "*");
		return format("\n\n%s%s\n%s\n%s%s\n\n", star, star, inner, star, star);
	}

	public static <T> T indexedOrNullWithDelta(List<T> list, T item,
			int delta) {
		int idx = list.indexOf(item);
		if (idx == -1) {
			return null;
		}
		idx += delta;
		if (idx < 0 || idx >= list.size()) {
			return null;
		}
		return list.get(idx);
	}

	public static int indexOf(Iterator iterator, Object obj) {
		int i = 0;
		while (iterator.hasNext()) {
			if (obj == iterator.next()) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static <T> int indexOf(Iterator<T> itr, Predicate<T> test) {
		int count = 0;
		while (itr.hasNext() && !test.test(itr.next())) {
			count++;
		}
		return count;
	}

	public static String infix(String s) {
		return isNullOrEmpty(s) ? null
				: s.substring(0, 1).toLowerCase()
						+ (s.length() == 1 ? "" : s.substring(1));
	}

	public static Set intersection(Collection c1, Collection c2) {
		Set result = setSupplier.get();
		if (c1.size() > c2.size()) {
			Collection tmp = c1;
			c1 = c2;
			c2 = tmp;
		}
		if (c2.size() > 10 && !(c2 instanceof Set)) {
			Set tmp = setSupplier.get();
			tmp.addAll(c2);
			c2 = tmp;
		}
		for (Object o : c1) {
			if (c2.contains(o)) {
				result.add(o);
			}
		}
		return result;
	}

	public static boolean isDerivedFrom(Object o, Class c) {
		if (o == null) {
			return false;
		}
		Class c2 = o.getClass();
		while (c2 != Object.class) {
			if (c2 == c) {
				return true;
			}
			c2 = c2.getSuperclass();
		}
		return false;
	}

	public static boolean isEnumSubclass(Class c) {
		return c.getSuperclass() != null && c.getSuperclass().isEnum();
	}

	@SuppressWarnings("deprecation")
	public static boolean isInCurrentMonth(Date date) {
		if (date == null) {
			return false;
		}
		Date now = new Date();
		return now.getYear() == date.getYear()
				&& now.getMonth() == date.getMonth();
	}

	public static boolean isLetterOnly(String string) {
		return string.matches("[a-zA-Z]+");
	}

	public static boolean isNotNullOrEmpty(Collection c) {
		return c != null && !c.isEmpty();
	}

	public static boolean isNotNullOrEmpty(String string) {
		return string != null && string.length() != 0;
	}

	public static boolean isNullOrEmpty(Collection c) {
		return c == null || c.isEmpty();
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isOneOf(Class clazz, Class[] possibleClasses) {
		for (Class c : possibleClasses) {
			if (clazz == c) {
				return true;
			}
		}
		return false;
	}

	public static boolean isStandardJavaClass(Class clazz) {
		return stdAndPrimitivesMap.containsValue(clazz);
	}

	public static boolean isStandardJavaClassOrEnum(Class clazz) {
		return isStandardJavaClass(clazz) || clazz.isEnum()
				|| isEnumSubclass(clazz);
	}

	public static boolean isWholeNumber(double d) {
		return d % 1 == 0.0;
	}

	public static <T> List<T> iteratorToList(Iterator<T> itr) {
		List<T> result = new ArrayList<>();
		while (itr.hasNext()) {
			result.add(itr.next());
		}
		return result;
	}

	public static int iv(Integer i) {
		return i == null ? 0 : i;
	}

	public static String join(Collection objects, String separator) {
		Object[] objs = objects.toArray(new Object[objects.size()]);
		return join(objs, separator);
	}

	public static String join(int[] objects, String separator) {
		Integer[] wrapped = new Integer[objects.length];
		for (int idx = 0; idx < objects.length; idx++) {
			wrapped[idx] = objects[idx];
		}
		return join(wrapped, separator, false);
	}

	public static String join(Object[] objects, String separator) {
		return join(objects, separator, false);
	}

	public static String join(Object[] objects, String separator,
			boolean ignoreEmpties) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : objects) {
			String app = obj == null ? "null" : obj.toString();
			if (sb.length() > 0 && (app.length() != 0 || !ignoreEmpties)
					&& separator != null) {
				sb.append(separator);
			}
			sb.append(app);
		}
		return sb.toString();
	}

	public static String joinAsEnglishList(List<String> phrases,
			String phraseTemplate) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < phrases.size(); i++) {
			String phrase = phrases.get(i);
			if (i > 0) {
				result.append((i == phrases.size() - 1) ? " and " : ", ");
			}
			result.append(phraseTemplate == null ? phrase
					: format(phraseTemplate, phrase));
		}
		return result.toString();
	}

	public static String joinPaths(String path, String sub) {
		return (path + "/" + sub).replaceAll("/+", "/");
	}

	public static String joinWithComma(Collection c) {
		return join(c, ",");
	}

	public static String joinWithNewlines(Collection c) {
		return join(c, "\n");
	}

	public static String joinWithNewlineTab(Collection c) {
		return join(c, "\n\t");
	}

	public static <T> T last(Iterator<T> iterator) {
		T last = null;
		while (iterator.hasNext()) {
			last = iterator.next();
		}
		return last;
	}

	public static <T> T last(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	public static Object last(Object[] array) {
		return array.length == 0 ? null : array[array.length - 1];
	}

	public static <V> List<V> lastNMembers(List<V> list, int n) {
		if (list.size() <= n) {
			return list;
		}
		return new ArrayList<>(list.subList(list.size() - n, list.size()));
	}

	public static String lastPathSegment(String url) {
		return url == null || !url.matches(".+/.+") ? ""
				: url.replaceFirst(".+/(.+)", "$1");
	}

	public static <T> Set<T> lazyUnion(Set<T> c1, Set<T> c2) {
		if (c1.size() == 0) {
			return c2;
		}
		if (c2.size() == 0) {
			return c1;
		}
		Set<T> result = new LinkedHashSet<>();
		result.addAll(c1);
		result.addAll(c2);
		return result;
	}

	public static String lcFirst(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static int luhnChecksum(String numericalString,
			boolean hasCheckDigit) {
		int length = numericalString.length();
		int sum = 0;
		boolean alternate = !hasCheckDigit;
		for (int i = numericalString.length() - 1; i >= 0; i--) {
			int n = Integer.parseInt(numericalString.substring(i, i + 1));
			if (alternate) {
				n *= 2;
				if (n > 9) {
					n = n - 9;
				}
			}
			sum += n;
			alternate = !alternate;
		}
		int checksum = sum % 10 == 0 ? 0 : 10 - sum % 10;
		return checksum;
	}

	public static long lv(Long l) {
		return l == null ? 0 : l;
	}

	public static <U, V> Comparator<U> mappingComparator(Function<U, V> mapping,
			Comparator<V> vComparator) {
		return new Comparator<U>() {
			@Override
			public int compare(U o1, U o2) {
				return vComparator.compare(mapping.apply(o1),
						mapping.apply(o2));
			}
		};
	}

	@SuppressWarnings("deprecation")
	public static Date monthsFromNow(int months) {
		Date d = roundDate(new Date(), false);
		int m = d.getMonth() + months;
		d.setMonth(m % 12);
		d.setYear(d.getYear() + m / 12);
		return d;
	}

	public static String namedFormat(String source,
			Map<String, ? extends Object> args) {
		if (source == null) {
			return null;
		}
		for (String s : args.keySet()) {
			source = source.replace("%" + s + "%", args.get(s).toString());
		}
		return source;
	}

	public static Set nonNullSet(Set value) {
		return value == null ? new LinkedHashSet<>() : value;
	}

	public static String normaliseForMatch(String string) {
		if (string == null) {
			return null;
		}
		return string.trim().toLowerCase();
	}

	public static <T> Stream<T> nullableStream(T t) {
		List<T> list = new ArrayList<>();
		if (t != null) {
			list.add(t);
		}
		return list.stream();
	}

	public static String nullSafeToString(Object o) {
		return o == null ? null : o.toString();
	}

	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	@SuppressWarnings("deprecation")
	public static Date oldDate(int year, int month, int dayOfMonth) {
		Date date = new Date();
		date.setYear(year - 1900);
		date.setMonth(month - 1);
		date.setDate(dayOfMonth);
		date = CommonUtils.roundDate(date, false);
		return date;
	}

	public static <T extends Comparable> List<T>
			order(Collection<T> comparableCollection) {
		List<T> items = new ArrayList<T>(comparableCollection);
		Collections.sort(items);
		return items;
	}

	public static String padEight(int number) {
		if (number < 10000000) {
			String s = String.valueOf(number);
			return "00000000".substring(s.length()) + s;
		} else {
			return String.valueOf(number);
		}
	}

	public static String padFive(int number) {
		if (number < 10000) {
			String s = String.valueOf(number);
			return "00000".substring(s.length()) + s;
		} else {
			return String.valueOf(number);
		}
	}

	public static String padFour(int number) {
		if (number < 1000) {
			return "0" + padThree(number);
		} else {
			return String.valueOf(number);
		}
	}

	public static String padLinesLeft(String block, String prefix) {
		StringBuilder sb = new StringBuilder();
		for (String line : block.split("\n")) {
			sb.append(prefix);
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}

	public static String padStringLeft(String input, int length, char padChar) {
		return padStringLeft(input, length, String.valueOf(padChar));
	}

	public static String padStringLeft(String input, int length, String pad) {
		input = input == null ? "(null)" : input;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length - input.length(); i++) {
			sb.append(pad);
		}
		sb.append(input);
		return sb.toString();
	}

	public static String padStringRight(String input, int length,
			char padChar) {
		input = input == null ? "(null)" : input;
		StringBuffer sb = new StringBuffer();
		sb.append(input);
		for (int i = 0; i < length - input.length(); i++) {
			sb.append(padChar);
		}
		return sb.toString();
	}

	public static String padThree(int number) {
		if (number < 100) {
			return "0" + padTwo(number);
		} else {
			return String.valueOf(number);
		}
	}

	public static String padTwo(int number) {
		if (number < 10) {
			return "0" + number;
		} else {
			return String.valueOf(number);
		}
	}

	public static int parseIntOrZero(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static String pluralise(String s, Collection c) {
		return pluralise(s, c == null ? 0 : c.size(), false);
	}

	public static String pluralise(String s, int size, boolean withCount) {
		if (withCount) {
			s = format("%s %s", size, s);
		}
		if (size == 1) {// note 0/null gives a plural form
			// (what a strange
			// language...)
			return s;
		}
		if (s.endsWith("s")) {
			return s;
		}
		if (s.endsWith(" by")) {
			return s;
		}
		if (s.endsWith("y")) {
			return s.substring(0, s.length() - 1) + "ies";
		}
		if (s.endsWith("ch")) {
			return s + "es";
		}
		return s + "s";
	}

	public static String pluraliseWithCount(String s, Collection c) {
		return pluralise(s, c == null ? 0 : c.size(), true);
	}

	public static void putIfKeyNotNull(Map m, Object k, Object v) {
		if (k != null) {
			m.put(k, v);
		}
	}

	public static List<String> removeNullsAndEmpties(List<String> parts) {
		List<String> dedupe = CommonUtils.dedupe(parts);
		dedupe.remove(null);
		dedupe.remove("");
		return dedupe;
	}

	public static String renderWholeOrTwoPlaces(double d) {
		if (d == Math.floor(d)) {
			return String.valueOf((int) d);
		} else {
			int whole = (int) Math.floor(d);
			int fractional = (int) Math.round((d - whole) * 100);
			return format("%s.%s", whole,
					padStringLeft(String.valueOf(fractional), 2, ' '));
		}
	}

	public static <T> Collection<T> reverse(Collection<T> collection) {
		List list = new ArrayList<>(collection);
		Collections.reverse(list);
		return list;
	}

	public static String round(float f, int places) {
		int multiplier = 1;
		for (int i = 0; i < places; i++) {
			multiplier *= 10;
		}
		String s = String.valueOf((int) (f * multiplier));
		s = padStringLeft(s, places + 1, '0');
		int len = s.length();
		return s.substring(0, len - places) + "." + s.substring(len - places);
	}

	// to 00.00:00 or 23:59.59
	@SuppressWarnings("deprecation")
	public static Date roundDate(Date d, boolean up) {
		d.setHours(up ? 23 : 0);
		d.setMinutes(up ? 59 : 0);
		d.setSeconds(up ? 59 : 0);
		d.setTime(d.getTime() - d.getTime() % 1000);
		return d;
	}

	public static double roundNumeric(double d, int places) {
		int multiplier = 1;
		// cos Math.round((1.005 ) * 100) / 100 = 1, not 1.01
		double pad = 0.0001;
		for (int i = 0; i < places; i++) {
			multiplier *= 10;
			pad /= 10;
		}
		double absD = Math.abs(d);
		String s = String.valueOf(Math.round(absD * multiplier + pad));
		s = padStringLeft(s, places + 1, '0');
		int len = s.length();
		double val = Double.valueOf(
				s.substring(0, len - places) + "." + s.substring(len - places));
		if (d < 0) {
			val *= -1;
		}
		return val;
	}

	public static String safeToString(Object obj) {
		try {
			return obj == null ? "(null)" : obj.toString();
		} catch (Exception e) {
			return "Exception in toString() - " + e.getMessage();
		}
	}

	public static <T> Set<T> setOf(T... values) {
		return new LinkedHashSet<T>(Arrays.asList(values));
	}

	public static <T extends Collection> T
			shallowCollectionClone(T collection) {
		try {
			T clone = null;
			if (collection instanceof ArrayList) {
				clone = (T) ((ArrayList) collection).clone();
			} else if (collection instanceof LinkedHashSet) {
				clone = (T) ((LinkedHashSet) collection).clone();
			} else if (collection instanceof LiSet) {
				clone = (T) ((LiSet) collection).clone();
			} else if (collection instanceof HashSet) {
				clone = (T) ((HashSet) collection).clone();
			} else if (collection instanceof LightSet) {
				clone = (T) ((LightSet) collection).clone();
			} else {
				if (GWT.isClient()) {
					throw new UnsupportedOperationException();
				} else {
					logger.warn("Cloning unexpected shallow collection: {}",
							collection.getClass());
					clone = (T) Reflections.classLookup()
							.newInstance(collection.getClass());
				}
			}
			return clone;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String shortMonthName(int month) {
		return MONTH_NAMES[month].substring(0, 3);
	}

	public static String simpleClassName(Class c) {
		String s = c.getName();
		return s.substring(s.lastIndexOf('.') + 1);
	}

	public static int sizeOrZero(Collection collection) {
		return collection == null ? 0 : collection.size();
	}

	// use when tostring is relatively expensive
	public static ArrayList sortByStringValue(Collection c) {
		Map<String, List> m = new HashMap<String, List>();
		for (Object o : c) {
			String key = o == null ? null : o.toString();
			if (!m.containsKey(key)) {
				m.put(key, new ArrayList());
			}
			m.get(key).add(o);
		}
		List nullValues = m.get(null);
		m.remove(null);
		ArrayList<String> keys = new ArrayList<String>(m.keySet());
		Collections.sort(keys);
		ArrayList result = new ArrayList();
		if (nullValues != null) {
			result.addAll(nullValues);
		}
		for (String key : keys) {
			result.addAll(m.get(key));
		}
		return result;
	}

	public static List<String> split(String content, String split) {
		List<String> result = new ArrayList<String>();
		int idx0 = 0;
		int idx1 = 0;
		while (true) {
			idx1 = content.indexOf(split, idx0);
			if (idx1 == -1) {
				result.add(content.substring(idx0));
				break;
			}
			result.add(content.substring(idx0, idx1));
			idx0 = idx1 + split.length();
		}
		return result;
	}

	public static String tabify(String value, int charsPerLine, int tabCount) {
		int fuzz = 15;
		StringBuilder sb = new StringBuilder();
		String ss = null;
		for (int idx = 0; idx < value.length();) {
			int idy = Math.min(idx + charsPerLine, value.length());
			ss = value.substring(idx, idy);
			int idn = ss.indexOf("\n");
			if (idn != -1) {
				ss = value.substring(idx, idx + idn);
				idx = idx + idn + 1;
			} else {
				int ls = ss.lastIndexOf(" ");
				if (ls != -1 && charsPerLine - ls < fuzz) {
					idy = idx + ls + 1;
					ss = value.substring(idx, idy);
				}
				idx = idy;
			}
			for (int j = 0; j < tabCount; j++) {
				sb.append("\t");
			}
			sb.append(ss);
			sb.append("\n");
		}
		return sb.toString();
	}

	public static <T> ThreeWaySetResult<T> threeWaySplit(Collection<T> c1,
			Collection<T> c2) {
		ThreeWaySetResult<T> result = new ThreeWaySetResult<T>();
		Set intersection = intersection(c1, c2);
		result.intersection = intersection;
		result.firstOnly = new LinkedHashSet<T>(c1);
		result.secondOnly = new LinkedHashSet<T>(c2);
		result.firstOnly.removeAll(intersection);
		result.secondOnly.removeAll(intersection);
		return result;
	}

	public static <T> ThreeWaySetResult<T>
			threeWaySplitIdentity(Collection<T> c1, Collection<T> c2) {
		ThreeWaySetResult<T> result = new ThreeWaySetResult<T>();
		IdentityHashMap m1 = new IdentityHashMap();
		IdentityHashMap m2 = new IdentityHashMap();
		c1.forEach(o -> m1.put(o, true));
		c2.forEach(o -> m2.put(o, true));
		result.firstOnly = m1.keySet();
		result.secondOnly = m2.keySet();
		IdentityHashMap intersection = new IdentityHashMap();
		result.intersection = intersection.keySet();
		for (Object o : m1.keySet()) {
			if (m2.keySet().contains(o)) {
				intersection.put(o, true);
			}
		}
		result.firstOnly.removeAll(result.intersection);
		result.secondOnly.removeAll(result.intersection);
		return result;
	}

	public static String titleCase(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		String[] strings = s.split(" ");
		StringBuffer sb = new StringBuffer();
		for (String string : strings) {
			if (sb.length() != 0) {
				sb.append(" ");
			}
			String title = upperCaseFirstLetterOnly(string);
			if (title.contains("-") && title.length() > 1) {
				title = Arrays.asList(title.split("-")).stream()
						.map(CommonUtils::upperCaseFirstLetterOnly)
						.collect(Collectors.joining("-"));
			}
			if (title.contains(".") && title.length() > 1) {
				title = Arrays.asList(title.split("\\.")).stream()
						.map(CommonUtils::upperCaseFirstLetterOnly)
						.collect(Collectors.joining("."));
			}
			sb.append(title);
		}
		return sb.toString();
	}

	public static String titleCaseKeepAcronyms(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		String[] strings = s.split(" ");
		StringBuffer sb = new StringBuffer();
		for (String string : strings) {
			if (sb.length() != 0) {
				sb.append(" ");
			}
			if (!isLetterOnly(string)) {
				sb.append(string);
			} else if (string.toUpperCase().equals(string)) {
				sb.append(string);
			} else if (isCamelCaseIsh(string)) {
				sb.append(string);
			} else {
				sb.append(upperCaseFirstLetterOnly(string));
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("deprecation")
	public static String toFinancialYear(Date date) {
		int year = date.getYear() + 1900;
		if (date.getMonth() < 6) {
			year -= 1;
		}
		return format("FY%s%s", year, year + 1);
	}

	public static String toSimpleExceptionMessage(Throwable caught) {
		return format("%s:%s", caught.getClass().getSimpleName(),
				caught.getMessage());
	}

	@SuppressWarnings("deprecation")
	public static String toYearMonth(Date date) {
		return date == null ? null
				: format("%sM%s", padFour(1900 + date.getYear()),
						padTwo(date.getMonth() + 1));
	}

	public static String trimIgnoreWs(String s, int length) {
		return Ax.isBlank(s) || s.length() < length ? s
				: s.substring(0, length);
	}

	public static String trimLinesToChars(String string, int i) {
		return Arrays.asList(string.split("\n")).stream()
				.map(s -> trimToWsChars(s, 100))
				.collect(Collectors.joining("\n"));
	}

	public static String trimToWsChars(String s, int maxChars) {
		return trimToWsChars(s, maxChars, "");
	}

	public static String trimToWsChars(String s, int maxChars,
			boolean withDotDot) {
		return trimToWsChars(s, maxChars, "...");
	}

	public static String trimToWsChars(String s, int maxChars,
			String ellipsis) {
		if (maxChars < 0) {
			maxChars = 100;
		}
		if (s == null || s.length() <= maxChars) {
			return s;
		}
		if (s.substring(maxChars / 2, maxChars).indexOf(" ") == -1) {
			return s.substring(0, maxChars) + ellipsis;
		}
		return s.substring(0, s.substring(0, maxChars).lastIndexOf(' '))
				+ ellipsis;
	}

	public static String trimToWsCharsMiddle(String s, int maxChars) {
		if (s == null || s.length() <= maxChars) {
			return s;
		}
		String left = s.substring(0, maxChars / 2);
		int idx0 = left.lastIndexOf(" ");
		if (idx0 != -1) {
			left = left.substring(0, idx0);
		}
		String right = s.substring(s.length() - maxChars / 2);
		idx0 = right.indexOf(" ");
		if (idx0 != -1) {
			right = right.substring(idx0);
		}
		return left + " ... " + right;
	}

	public static String trimToWsReverse(String s, int maxChars) {
		return trimToWsReverse(s, maxChars, false);
	}

	public static String trimToWsReverse(String s, int maxChars,
			boolean withEllipsis) {
		if (s.length() <= maxChars) {
			return s;
		}
		if (maxChars < 0) {
			maxChars = 100;
		}
		String sub = s.substring(s.length() - maxChars);
		String ellipsis = withEllipsis ? "..." : "";
		if (sub.indexOf(" ") > maxChars / 2) {
			return ellipsis + sub;
		}
		int firstSpace = sub.indexOf(' ');
		return firstSpace == -1 ? "" : ellipsis + sub.substring(firstSpace);
	}

	public static String upperCaseFirstLetterOnly(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		String pre = "";
		if (s.length() > 1 && s.matches("[({\\[].+")) {
			pre += s.substring(0, 1);
			s = s.substring(1);
		}
		if (s.length() > 2 && s.matches("[A-Z]['’].+")) {
			pre += s.substring(0, 2);
			s = s.substring(2);
		}
		if (s.length() > 3 && s.toLowerCase().startsWith("mc")) {
			pre = "Mc";
			s = s.substring(2);
		}
		return pre + s.substring(0, 1).toUpperCase()
				+ s.substring(1).toLowerCase();
	}

	public static <T> void validateComparator(List<T> list,
			Comparator<T> comparator) {
		SystemoutCounter counter = SystemoutCounter
				.standardJobCounter(list.size(), "validate-comparator");
		for (int idx0 = 0; idx0 < list.size(); idx0++) {
			for (int idx1 = 0; idx1 < list.size(); idx1++) {
				for (int idx2 = 0; idx2 < list.size(); idx2++) {
					if (idx0 == idx1 || idx1 == idx2 || idx0 == idx2) {
						continue;
					}
					T o0 = list.get(idx0);
					T o1 = list.get(idx1);
					T o2 = list.get(idx2);
					if (!CommonUtils.validateComparator(o0, o1, o2,
							comparator.compare(o0, o1),
							comparator.compare(o1, o2),
							comparator.compare(o0, o2))) {
						comparator.compare(o0, o1);
						comparator.compare(o1, o2);
						comparator.compare(o0, o2);
						throw Ax.runtimeException(
								"Comparator relation issue: %s %s %s :: %s %s %s",
								o0, o1, o2, comparator.compare(o0, o1),
								comparator.compare(o1, o2),
								comparator.compare(o0, o2));
					}
				}
			}
			counter.tick();
		}
	}

	public static boolean validateComparator(Object o1, Object o2, Object o3,
			int cmp0_1, int cmp1_2, int cmp0_2) {
		if (cmp0_1 < 0 && cmp1_2 < 0) {
			if (cmp0_2 < 0) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public static Collection wrapInCollection(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Collection) {
			return (Collection) o;
		} else {
			ArrayList arr = new ArrayList();
			arr.add(o);
			return arr;
		}
	}

	public static List<Integer> wrapIntArray(int[] ints) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < ints.length; i++) {
			result.add(ints[i]);
		}
		return result;
	}

	public static List<Long> wrapLongArray(long[] longs) {
		List<Long> result = new ArrayList<Long>();
		if (longs != null) {
			for (int i = 0; i < longs.length; i++) {
				result.add(longs[i]);
			}
		}
		return result;
	}

	public static Exception wrapThrowable(Throwable e) {
		return (Exception) (e instanceof Exception ? e : new Exception(e));
	}

	public static <T> T wrapThrowing(ThrowingSupplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public static Date yearAsDate(Integer year) {
		if (year == null) {
			year = 0;
		}
		Date d = new Date(0);
		d.setYear(year - 1900);
		d.setMonth(0);
		d.setDate(1);
		return d;
	}

	private static String getNumericSubstring(String toParse) {
		if (toParse == null) {
			return null;
		}
		toParse = toParse.trim();
		if (toParse.isEmpty()) {
			return null;
		}
		int i = 0;
		char c = toParse.charAt(0);
		if ((c == '+' || c == '-') && toParse.length() > 1) {
			i++;
		}
		for (; i < toParse.length(); i++) {
			c = toParse.charAt(i);
			if (c > '9' || c < '0') {
				break;
			}
		}
		return i == 0 ? null : toParse.substring(0, i);
	}

	private static boolean isCamelCaseIsh(String string) {
		boolean seenLower = false;
		for (int idx = 0; idx < string.length(); idx++) {
			char c = string.charAt(idx);
			boolean upper = c >= 'A' && c <= 'Z';
			boolean lower = c >= 'a' && c <= 'z';
			if (seenLower && upper) {
				return true;
			}
			if (lower) {
				seenLower = true;
			}
		}
		return false;
	}

	public enum ComparatorResult {
		BOTH_NON_NULL, BOTH_NULL, FIRST_NULL, SECOND_NULL;
		public int direction() {
			switch (this) {
			case FIRST_NULL:
				return -1;
			case SECOND_NULL:
				return 1;
			default:
				return 0;
			}
		}

		public boolean hadNull() {
			switch (this) {
			case BOTH_NON_NULL:
				return false;
			default:
				return true;
			}
		}
	}

	public enum DateStyle {
		AU_DATE_SLASH, AU_DATE_MONTH, AU_DATE_MONTH_DAY, AU_DATE_TIME,
		AU_DATE_TIME_HUMAN, AU_DATE_TIME_MS, AU_SHORT_DAY, AU_DATE_DOT,
		AU_LONG_DAY, AU_SHORT_MONTH, AU_DATE_SLASH_MONTH, TIMESTAMP,
		NAMED_MONTH_DATE_TIME_HUMAN, NAMED_MONTH_DAY, AU_SHORT_MONTH_SLASH,
		AU_SHORT_MONTH_NO_DAY, TIMESTAMP_HUMAN, US_DATE_SLASH, TIMESTAMP_NO_DAY,
		AU_DATE_MONTH_NO_PAD_DAY, AU_DATE_TIME_SHORT
	}

	public static class DeduplicatePredicate<C, K> implements Predicate<C> {
		Set<K> seen = new LinkedHashSet<>();

		private Function<C, K> keyMapper;

		public DeduplicatePredicate(Function<C, K> keyMapper) {
			this.keyMapper = keyMapper;
		}

		@Override
		public boolean test(C t) {
			K key = keyMapper.apply(t);
			return !seen.add(key);
		}
	}

	public static interface IidGenerator {
		String generate();
	}

	public static class ThreeWaySetResult<T> {
		public Set<T> firstOnly;

		public Set<T> secondOnly;

		public Set<T> intersection;

		public boolean isEmpty() {
			return firstOnly.isEmpty() && secondOnly.isEmpty()
					&& intersection.isEmpty();
		}

		public String toSizes() {
			return format("First: %s\tBoth: %s\tSecond: %s", firstOnly.size(),
					intersection.size(), secondOnly.size());
		}

		public String toSizes(String firstType, String secondType) {
			return format("%s: %s\tBoth: %s\t%s: %s", firstType,
					firstOnly.size(), intersection.size(), secondType,
					secondOnly.size());
		}

		@Override
		public String toString() {
			return format("First: %s\nBoth: %s\nSecond: %s", firstOnly,
					intersection, secondOnly);
		}
	}

	public static boolean hasSuperClass(Class clazz, Class superClass) {
		while(clazz!=null&&clazz!=Object.class) {
			if(clazz==superClass) {
				return true;
			}
			clazz=clazz.getSuperclass();
		}
		return false;
	}

	public static String restId(String string) {
		return deInfix(string).replaceFirst("^ ", "").replace(" ", "-").toLowerCase();
	}
}

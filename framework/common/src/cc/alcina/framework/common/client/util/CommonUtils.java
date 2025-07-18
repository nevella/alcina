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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * @author Nick Reddel
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class CommonUtils {
	private static final Predicate<?> PREDICATE_FALSE = o -> false;

	private static final Predicate<?> PREDICATE_TRUE = o -> true;

	public static final String XML_PI = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	// for GWT reflection gets, this gets used...a lot
	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

	public static final String[] MONTH_NAMES = { "invalid", "January",
			"February", "March", "April", "May", "June", "July", "August",
			"September", "October", "November", "December" };

	public static final String[] DAY_NAMES = { "Sunday", "Monday", "Tuesday",
			"Wednesday", "Thursday", "Friday", "Saturday" };

	public static Supplier<Set> setSupplier = () -> new LinkedHashSet();

	/**
	 * For trimming a utf8 string for insertion into a 255-char varchar db
	 * field. Oh, the pain
	 */
	public static final int SAFE_VARCHAR_MAX_CHARS = 230;

	/*
	 * name,friendly,string
	 */
	private static UnsortedMultikeyMap<Enum> enumValueLookup = new UnsortedMultikeyMap<Enum>(
			3);

	// https://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style/Titles
	private static Set<String> standardLowercaseEnglish = Arrays.stream(
			"A,An,The,And,But,Or,Nor,For,Yet,So,As,In,Of,On,To,For,From,Into,LikeExcluded,Over,With,Upon"
					.split(","))
			.collect(Collectors.toSet());

	static Logger logger = LoggerFactory.getLogger(CommonUtils.class);

	public static final Set<String> COLLECTION_CLASS_NAMES = Arrays
			.asList(ArrayList.class, LinkedList.class, HashSet.class,
					LinkedHashSet.class, TreeSet.class, HashMap.class,
					LinkedHashMap.class, TreeMap.class, LightSet.class,
					LiSet.class, LightMap.class, CountingMap.class,
					IdentityArrayList.class, Multimap.class, Multiset.class,
					MultikeyMap.class, UnsortedMultikeyMap.class,
					SortedMultikeyMap.class)
			.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

	public static final Set<String> CORE_CLASS_NAMES = Arrays
			.asList(Class.class, Timestamp.class, Date.class, String.class)
			.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

	public static final Set<String> PRIMITIVE_CLASS_NAMES = Arrays
			.asList(long.class, int.class, short.class, char.class, byte.class,
					boolean.class, double.class, float.class, void.class)
			.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

	public static final Set<String> PRIMITIVE_WRAPPER_CLASS_NAMES = Arrays
			.asList(Long.class, Double.class, Float.class, Short.class,
					Byte.class, Integer.class, Boolean.class, Character.class,
					Void.class)
			.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

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

	public static Class classOrSelf(Object o) {
		return o instanceof Class ? (Class) o : o.getClass();
	}

	// assume slash-delineated
	public static String combinePaths(String absPath, String relPath) {
		if (relPath.contains("://")) {
			return relPath;
		}
		if (relPath.startsWith("//")) {
			return absPath.replaceFirst("(.+?:)//.+", "$1") + relPath;
		}
		if (relPath.startsWith("?")) {
			return absPath + relPath;
		}
		if (relPath.startsWith("/")) {
			if (absPath.contains("://")) {
				int idx0 = absPath.indexOf("://") + 3;
				int idx1 = absPath.indexOf("/", idx0);
				return (idx1 == -1 ? absPath : absPath.substring(0, idx1))
						+ relPath;
			} else {
				return relPath;
			}
		}
		String parentSep = "../";
		String voidSep = "./";
		int x = 0;
		x = absPath.lastIndexOf("/");
		if (x != -1) {
			absPath = absPath.substring(0, x);
		}
		while (relPath.startsWith(parentSep)) {
			x = absPath.lastIndexOf("/");
			absPath = absPath.substring(0, x);
			relPath = relPath.substring(parentSep.length());
		}
		if (relPath.startsWith(voidSep)) {
			relPath = relPath.substring(voidSep.length());
		}
		if (!absPath.endsWith("/")) {
			absPath += "/";
		}
		return absPath + relPath;
	}

	public static String commaCommaAnd(List<String> list) {
		StringBuilder sb = new StringBuilder();
		int size = list.size();
		for (int idx = 0; idx < size; idx++) {
			if (idx > 0) {
				if (idx == size - 1) {
					sb.append(" and ");
				} else {
					sb.append(", ");
				}
			}
			sb.append(list.get(idx));
		}
		return sb.toString();
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

	/**
	 * Compare two dates with null low
	 * 
	 * @param d1
	 *            The first date
	 * @param d2
	 *            The second date
	 * @return the comparator result (with a null first date being low)
	 */
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

	public static <T> int compareWithNullMinusOne(T o1, T o2,
			Comparator<T> comparator) {
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		}
		return o2 == null ? 1 : comparator.compare(o1, o2);
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

	public static boolean containsIgnoreCase(String string, String contains) {
		if (string == null || contains == null) {
			return false;
		}
		return string.toLowerCase().contains(contains.toLowerCase());
	}

	public static <T> boolean
			containsNonNull(Collection<? extends T> collection, T element) {
		return element == null ? false : collection.contains(element);
	}

	public static boolean containsWithNull(Object obj, String text) {
		if (obj == null || text == null) {
			return false;
		}
		String string = obj.toString();
		return string != null && string.contains(text);
	}

	public static boolean containsWithNullLowerCased(Object obj,
			String lowerCasedText) {
		if (obj == null || lowerCasedText == null) {
			return false;
		}
		String string = obj.toString();
		return string != null && string.toLowerCase().contains(lowerCasedText);
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

	public static String cssify(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString();
		if (CommonUtils.isNullOrEmpty(s)) {
			return s;
		}
		if (s.contains("_")) {
			return s.toLowerCase().replace('_', '-');
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

	public static boolean currencyEquals(double d1, double d2) {
		return Math.abs(d1 - d2) < 0.005;
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
		for (int idx = 0; idx < s.length(); idx++) {
			String c = s.substring(idx, idx + 1);
			if (c.equals("_")) {
				/*
				 * break at underscore - anything from underscore on is output
				 * verbatim
				 */
				buf.append(" ");
				buf.append(s.substring(idx + 1));
				break;
			}
			buf.append(c.toUpperCase().equals(c) && idx != 0 ? " " : "");
			buf.append(idx == 0 ? c.toUpperCase() : c.toLowerCase());
		}
		return buf.toString();
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

	public static void dumpBytes(byte[] bs, int width) {
		dumpBytes(bs, width, true);
	}

	public static void dumpBytes(byte[] bs, int width, boolean indexAsHex) {
		StringBuilder bd = new StringBuilder();
		int len = bs.length;
		for (int i = 0; i < len; i += width) {
			bd.append(CommonUtils.padStringLeft(
					(indexAsHex ? Integer.toHexString(i) : String.valueOf(i)),
					8, '0'));
			bd.append(":  ");
			for (int j = 0; j < width; j++) {
				boolean in = j + i < len;
				// int rather than byte so we can unsign
				int b = in ? bs[i + j] : 0;
				if (b < 0) {
					b += 256;
				}
				bd.append(in ? CommonUtils.padStringLeft(Integer.toHexString(b),
						2, '0') : "  ");
				bd.append("  ");
			}
			for (int j = 0; j < width; j++) {
				boolean in = j + i < len;
				char c = in ? (char) bs[i + j] : ' ';
				c = c < '\u0020' || c >= '\u007F' ? '.' : c;
				bd.append(c);
			}
			bd.append('\n');
		}
		System.out.println(bd.toString());
	}

	public static void dumpHashes(Collection collection) {
		StringBuilder sb = new StringBuilder();
		for (Object e : collection) {
			sb.append(e.hashCode());
			sb.append("\t ");
			sb.append(e);
			sb.append("\n");
		}
		System.out.println(sb);
	}

	public static void dumpStringBytes(String s) {
		try {
			dumpBytes(s.getBytes("UTF-16"), 8);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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

	public static String ensureTrailingSlash(String path) {
		return path.endsWith("/") ? path : path + "/";
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
					if (!Objects.deepEquals(o1, o2)) {
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
		s = s.replace("*", "\\*");
		s = s.replace(".", "\\.");
		return s;
	}

	public static <T extends Throwable> T
			extractCauseOfClass(Throwable throwable, Class<T> throwableClass) {
		while (true) {
			if (ClassUtil.isDerivedFrom(throwable, throwableClass)) {
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

	public static <I, O> List<O> filterByClass(
			Collection<? extends I> collection,
			Class<? extends O> filterClass) {
		ArrayList<O> result = new ArrayList<O>();
		for (I i : collection) {
			if (i.getClass() == filterClass) {
				result.add((O) i);
			}
		}
		return result;
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
		StringBuilder sb = new StringBuilder();
		int from = 0;
		int len = source.length();
		int argsIndex = 0;
		while (from < len) {
			int to = source.indexOf("%s", from);
			to = to == -1 ? len : to;
			sb.append(source, from, to == -1 ? len : to);
			if (to != len) {
				sb.append(args[argsIndex++]);
				to += 2;
			}
			from = to;
		}
		return sb.toString();
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

	public static Class getComparableType(Object o) {
		Class<? extends Object> clazz = o.getClass();
		return ClassUtil.isEnumSubclass(clazz) ? clazz.getSuperclass() : clazz;
	}

	public static Class<? extends Enum> getEnumType(Enum e) {
		if (e.getClass().getSuperclass().isEnum()) {
			return (Class<? extends Enum>) e.getClass().getSuperclass();
		} else {
			return e.getClass();
		}
	}

	public static <E extends Enum> E getEnumValueOrNull(Class<E> enumClass,
			String value) {
		return getEnumValueOrNull(enumClass, value, false, null);
	}

	public static synchronized <E extends Enum> E getEnumValueOrNull(
			Class<E> enumClass, String value, boolean withFriendlyNames,
			E defaultValue) {
		if (enumValueLookup.asMapEnsure(false, enumClass,
				withFriendlyNames) == null) {
			for (E ev : enumClass.getEnumConstants()) {
				enumValueLookup.put(enumClass, withFriendlyNames, ev.toString(),
						ev);
				enumValueLookup.put(enumClass, withFriendlyNames,
						ev.toString().toLowerCase(), ev);
				if (withFriendlyNames) {
					// handle double__ default
					enumValueLookup.put(enumClass, withFriendlyNames,
							ev.toString().toLowerCase().replace('_', '-'), ev);
					enumValueLookup.put(enumClass, withFriendlyNames,
							friendlyConstant(ev, "-").toLowerCase(), ev);
					enumValueLookup.put(enumClass, withFriendlyNames,
							friendlyConstant(ev, "-"), ev);
					enumValueLookup.put(enumClass, withFriendlyNames,
							friendlyConstant(ev, " "), ev);
					enumValueLookup.put(enumClass, withFriendlyNames,
							friendlyConstant(ev, " ").toLowerCase(), ev);
				}
			}
		}
		E result = (E) enumValueLookup.get(enumClass, withFriendlyNames, value);
		return result == null ? defaultValue : result;
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
			Predicate<Throwable> causeFilter) {
		while (true) {
			if (causeFilter.test(throwable)) {
				return true;
			}
			if (throwable.getCause() == throwable
					|| throwable.getCause() == null) {
				return false;
			}
			throwable = throwable.getCause();
		}
	}

	public static boolean hasIntersection(Set<?> set1, Set<?> set2) {
		Set<?> s1 = set1;
		Set<?> s2 = set2;
		if (s1.size() > s2.size()) {
			s2 = s1;
			s1 = set2;
		}
		Iterator<?> itr = s1.iterator();
		while (itr.hasNext()) {
			Object next = itr.next();
			if (s2.contains(next)) {
				return true;
			}
		}
		return false;
	}

	public static String highlightForLog(String template, Object... args) {
		String inner = format(template, args);
		String star = padStringLeft("", 40, "*");
		return format("\n\n%s%s\n%s\n%s%s\n\n", star, star, inner, star, star);
	}

	// https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
	public static int indexOf(byte[] source, int sourceOffset, int sourceCount,
			byte[] target, int targetOffset, int targetCount, int fromIndex) {
		if (fromIndex >= sourceCount) {
			return (targetCount == 0 ? sourceCount : -1);
		}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (targetCount == 0) {
			return fromIndex;
		}
		byte first = target[targetOffset];
		int max = sourceOffset + (sourceCount - targetCount);
		for (int i = sourceOffset + fromIndex; i <= max; i++) {
			/* Look for first character. */
			if (source[i] != first) {
				while (++i <= max && source[i] != first)
					;
			}
			/* Found first character, now look at the rest of v2 */
			if (i <= max) {
				int j = i + 1;
				int end = j + targetCount - 1;
				for (int k = targetOffset + 1; j < end
						&& source[j] == target[k]; j++, k++)
					;
				if (j == end) {
					/* Found whole string. */
					return i - sourceOffset;
				}
			}
		}
		return -1;
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

	public static boolean isOrHasSuperClass(Class clazz, Class superClass) {
		while (clazz != null && clazz != Object.class) {
			if (clazz == superClass) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	public static boolean isStandardJavaClass(Class clazz) {
		return ClassReflector.stdAndPrimitivesMap.containsValue(clazz);
	}

	public static boolean isStandardJavaClassOrEnum(Class clazz) {
		return isStandardJavaClass(clazz) || clazz.isEnum()
				|| ClassUtil.isEnumSubclass(clazz);
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
			app = app == null ? "null" : app;
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

	/**
	 * Convert a List into a Stream of sublists of given length If not enough
	 * elements present, will present a smaller sublist
	 *
	 * @param <T>
	 *            List item type
	 * @param source
	 *            Original list
	 * @param length
	 *            Maximum list of sublists
	 * @return Stream of smaller Lists
	 * @throws IllegalArgumentException
	 *             if length is negative or 0
	 */
	public static <T> Stream<List<T>> listToBatches(List<T> source,
			int length) {
		// Length must be postive
		if (length <= 0) {
			throw new IllegalArgumentException("length = " + length);
		}
		// If we have an empty original list, return an empty stream
		int size = source.size();
		if (size <= 0) {
			return Stream.empty();
		}
		// Number of chunks to generate
		int numChunks = (size - 1) / length;
		// Generate the stream to generate sublists
		return IntStream.range(0, numChunks + 1).mapToObj(n -> source
				.subList(n * length, n == numChunks ? size : (n + 1) * length));
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
		Date d = DateUtil.roundDate(new Date(), false);
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

	public static <T extends Comparable> List<T>
			order(Collection<T> comparableCollection) {
		List<T> items = new ArrayList<T>(comparableCollection);
		Collections.sort(items);
		return items;
	}

	public static <E> Set<E> orderedSet(E... elements) {
		return Arrays.stream(elements)
				.collect(AlcinaCollectors.toLinkedHashSet());
	}

	public static String padEight(int number) {
		String s = String.valueOf(number);
		if (s.length() < 8) {
			return "00000000".substring(s.length()) + s;
		} else {
			return s;
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
		if (size == 1) {
			// note 0/null gives a plural form
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

	public static <T> Predicate<T> predicateFalse() {
		return (Predicate<T>) PREDICATE_FALSE;
	}

	public static <T> Predicate<T> predicateTrue() {
		return (Predicate<T>) PREDICATE_TRUE;
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

	public static String restId(String string) {
		return deInfix(string).replaceFirst("^ ", "").replace(" ", "-")
				.toLowerCase();
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

	/**
	 * <li>Returns whether a given string starts with a given prefix.
	 * <li>Returns false for null strings or null prefixes
	 *
	 * @param str
	 *            String to check
	 * @param prefix
	 *            Prefix to check for
	 * @return
	 */
	public static boolean safeStartsWith(String str, String prefix) {
		return str == null || prefix == null ? false : str.startsWith(prefix);
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
					clone = (T) Reflections.newInstance(collection.getClass());
				}
			}
			return clone;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String shortenPath(String path, int maxLength) {
		if (path.length() < maxLength) {
			return path;
		}
		String preName = path.replaceFirst("(.+)/(.+)", "$1");
		String name = preName.contains("/")
				? path.replaceFirst("(.+)/(.+)", "$2")
				: "";
		if (name.length() > maxLength / 2) {
			name = name.substring(name.length() - maxLength / 2);
		}
		String ellipsis = "...";
		int preMaxLength = maxLength - name.length() - ellipsis.length();
		return Ax.format("%s%s/%s",
				preName.substring(0, Math.min(preName.length(), preMaxLength)),
				ellipsis, name);
	}

	public static String shortMonthName(int month) {
		return MONTH_NAMES[month].substring(0, 3);
	}

	public static String simpleClassName(Class c) {
		String s = c.getName();
		return s.substring(s.lastIndexOf('.') + 1);
	}

	public static String simpleClassName(Object o) {
		return o == null ? "(null)" : o.getClass().getSimpleName();
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

	/**
	 * <li>Split a string by separators</li>
	 *
	 * @param content
	 *            String to split
	 * @param split
	 *            Separator to split by
	 * @return ArrayList of separated strings
	 */
	public static ArrayList<String> split(String content, String split) {
		ArrayList<String> result = new ArrayList<String>();
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

	/**
	 * <li>Split a string by a regex expression</li>
	 *
	 * @param content
	 *            String to split
	 * @param regex
	 *            Regex to split by
	 * @return ArrayList of separated strings
	 */
	public static ArrayList<String> splitByRegex(String content, String regex) {
		ArrayList<String> result = new ArrayList<String>();
		String[] splits = content.split(regex);
		Collections.addAll(result, splits);
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

	public static String tf(boolean b) {
		return b ? "t" : "f";
	}

	public static void throwIfCompletedWithException(List<Future> futures)
			throws Exception {
		for (Future future : futures) {
			// will throw if there was an exception
			future.get();
		}
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

	public static String toHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String toLimitedCollectionString(Collection<?> collection,
			int maxLength) {
		if (collection.size() <= maxLength) {
			return collection.toString();
		}
		return Ax.format("(%s) [%s]...", collection.size(),
				collection.stream().limit(maxLength).map(Object::toString)
						.collect(Collectors.joining(",")));
	}

	public static String toSimpleExceptionMessage(Throwable caught) {
		return format("%s:%s", caught.getClass().getSimpleName(),
				caught.getMessage());
	}

	public static String toUrlFragment(Object object) {
		if (object == null) {
			return "null";
		}
		String fragment = object.toString();
		if (Objects.equals(fragment.toUpperCase(), fragment.toLowerCase())
				|| fragment.contains("-") || fragment.contains("_")) {
			fragment = fragment.toLowerCase();
			return fragment.replace("_", "-").replace(" ", "-");
		} else {
			return fragment.replaceAll("[/:-?#]]", "_");
		}
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
		if (maxChars >= 10
				&& s.substring(maxChars - 10, maxChars).indexOf(" ") == -1) {
			return s.substring(0, maxChars) + ellipsis;
		}
		int lastIndex = s.substring(0, maxChars).lastIndexOf(' ');
		lastIndex = lastIndex == -1 ? maxChars : lastIndex;
		return s.substring(0, lastIndex) + ellipsis;
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
		if (s.matches(".+[\\-\u2013\u2014\u2011\u2012].+")) {
			// handle hyphenated-names
			return Arrays.stream(s.split("[\\-\u2013\u2014\u2011\u2012]"))
					.map(CommonUtils::upperCaseFirstLetterOnly)
					.collect(Collectors.joining("-"));
		}
		String pre = "";
		if (s.length() > 1 && s.matches("[({\\[].+")) {
			pre += s.substring(0, 1);
			s = s.substring(1);
		}
		// question mark is bad unicode conversion possibility
		if (s.length() > 2 && s.matches("[A-Z]['’?].+")) {
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

	public static String upperCaseFirstLetterOnlyWords(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return Arrays.stream(s.split(" "))
				.map(CommonUtils::upperCaseFirstLetterOnly)
				.collect(Collectors.joining(" "));
	}

	// see also
	// https://stackoverflow.com/questions/11441666/java-error-comparison-method-violates-its-general-contract
	public static <T> void validateComparator(List<T> list,
			Comparator<T> comparator) {
		SystemoutCounter counter = SystemoutCounter
				.standardJobCounter(list.size(), "validate-comparator");
		for (int idx0 = 0; idx0 < list.size(); idx0++) {
			for (int idx1 = 0; idx1 < list.size(); idx1++) {
				for (int idx2 = 0; idx2 < list.size(); idx2++) {
					// if (idx0 == idx1 || idx1 == idx2 || idx0 == idx2) {
					// continue;
					// }
					T o0 = list.get(idx0);
					T o1 = list.get(idx1);
					T o2 = list.get(idx2);
					int cmp0_1 = comparator.compare(o0, o1);
					int cmp1_0 = comparator.compare(o1, o0);
					if (cmp0_1 != -cmp1_0) {
						comparator.compare(o0, o1);
						comparator.compare(o1, o0);
						throw Ax.runtimeException(
								"Comparator relation issue: %s %s %s :: %s %s %s",
								o0, o1, o2, comparator.compare(o0, o1),
								comparator.compare(o1, o2),
								comparator.compare(o0, o2));
					}
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

	public static Long zeroToNull(Long id) {
		return id != null && id == 0 ? null : id;
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

	public static class Paths {
		public static String ensureSlashTerminated(String path) {
			return path.endsWith("/") ? path : path + "/";
		}

		public static String sanitizeForUnixPaths(String fileName) {
			return fileName.replaceAll("[:]", "").replaceAll("[ ]", "_");
		}
	}

	public static String getFullExceptionMessage(Throwable t) {
		// stream not writer for gwt compat
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(baos);
		printStream.println(t.getClass().getName() + "\n");
		printStream.println(t.getMessage() + "\n");
		t.printStackTrace(printStream);
		try {
			return new String(baos.toByteArray(), "UTF-8");
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static int nullSafeOrdinal(Enum enunValue) {
		if (enunValue == null) {
			return -1;
		} else {
			return enunValue.ordinal();
		}
	}

	public static Set<?> wrapInSet(Collection<?> collection) {
		if (collection instanceof Set) {
			return (Set<?>) collection;
		} else {
			return new HashSet<>(collection);
		}
	}

	public static int charIncidenceCount(String string, char ofChar) {
		int result = 0;
		for (int idx = 0; idx < string.length(); idx++) {
			char c = string.charAt(idx);
			if (c == ofChar) {
				result++;
			}
		}
		return result;
	}

	public static Date max(Date d0, Date d1) {
		if (d0 == null) {
			return d1;
		}
		if (d1 == null) {
			return d0;
		}
		return d0.after(d1) ? d0 : d1;
	}
}

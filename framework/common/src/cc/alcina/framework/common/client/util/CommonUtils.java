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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
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

	public static boolean bv(Boolean b) {
		return b == null || b == false ? false : true;
	}

	public static String capitaliseFirst(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static int parseIntOrZero(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static String lcFirst(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static String classSimpleName(Class c) {
		return c.getName().substring(c.getName().lastIndexOf('.') + 1);
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

	public static int compareWithNullMinusOne(Comparable o1, Comparable o2) {
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		}
		return o2 == null ? 1 : o1.compareTo(o2);
	}

	public static boolean containsWithNull(Object obj, String lcText) {
		if (obj == null || lcText == null) {
			return false;
		}
		String string = obj.toString();
		return string != null && string.toLowerCase().contains(lcText);
	}

	public static boolean equalsWithNullEquality(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		return o1.equals(o2);
	}

	public static String format(String source, Object... args) {
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

	public static String formatJ(String source, Object... args) {
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

	@SuppressWarnings("deprecation")
	public static String formatDate(Date date, DateStyle style) {
		if (date == null) {
			return " ";
		}
		switch (style) {
		case AU_DATE_SLASH:
			return format("%1/%2/%3", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900));
		case AU_DATE_SLASH_MONTH:
			return format("%1/%2", padTwo(date.getMonth() + 1),
					padTwo(date.getYear() + 1900));
		case AU_DATE_DOT:
			return format("%1.%2.%3", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900));
		case AU_DATE_TIME:
			return format("%1/%2/%3 - %4:%5:%6", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()));
		case AU_DATE_TIME_HUMAN:
			return formatDate(date, DateStyle.AU_LONG_DAY)
					+ format(" at %1:%2 %3",
							padTwo((date.getHours() - 1) % 12 + 1),
							padTwo(date.getMinutes()),
							date.getHours() < 12 ? "AM" : "PM");
		case AU_DATE_TIME_MS:
			return format("%1/%2/%3 - %4:%5:%6:%7", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()), date.getTime() % 1000);
		case AU_DATE_MONTH:
			return format("%1 %2 %3", padTwo(date.getDate()),
					MONTH_NAMES[date.getMonth() + 1],
					padTwo(date.getYear() + 1900));
		case AU_SHORT_MONTH:
			return format("%1 %2 %3", padTwo(date.getDate()),
					MONTH_NAMES[date.getMonth() + 1].substring(0, 3),
					padTwo(date.getYear() + 1900));
		case AU_SHORT_DAY:
			return format("%4 - %1.%2.%3", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					DAY_NAMES[date.getDay()].substring(0, 3));
		case AU_LONG_DAY:
			return format("%4, %1.%2.%3", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					DAY_NAMES[date.getDay()]);
		case TIMESTAMP:
			return format("%3%2%1_%4%5%6_%7", padTwo(date.getDate()),
					padTwo(date.getMonth() + 1), padTwo(date.getYear() + 1900),
					padTwo(date.getHours()), padTwo(date.getMinutes()),
					padTwo(date.getSeconds()), date.getTime() % 1000);
		}
		return date.toString();
	}

	public static String friendlyConstant(Object o) {
		if (o == null) {
			return "";
		}
		String s = capitaliseFirst(o.toString().toLowerCase());
		int x = 0, y = 0;
		while (true) {
			y = s.indexOf("__");
			if (y != -1) {
				s = s.substring(0, x) + ((x == 0) ? "" : " ")
						+ s.substring(x, y).toUpperCase() + " "
						+ s.substring(y + 2);
				x = y + 2;
			} else {
				break;
			}
		}
		return s.replace('_', ' ').trim();
	}

	public static String getSimpleTimeBefore(Date d) {
		return getSimpleTimeBefore(d, new Date());
	}

	public static String getSimpleTimeBefore(Date d, Date currentDate) {
		long timeDiff = (long) Math.ceil((double) (currentDate.getTime() - d
				.getTime()) / (60 * 1000));
		if (timeDiff < 60) {
			return timeDiff + " minutes ago";
		}
		if (timeDiff < 24 * 60) {
			return Math.round(timeDiff / 60) + " hours ago";
		}
		return Math.round(timeDiff / (60 * 24)) + " days ago";
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

	public static String infix(String s) {
		return isNullOrEmpty(s) ? null : s.substring(0, 1).toLowerCase()
				+ (s.length() == 1 ? "" : s.substring(1));
	}

	public static Collection intersection(Collection c1, Collection c2) {
		ArrayList result = new ArrayList();
		for (Object o : c1) {
			if (c2.contains(o)) {
				result.add(o);
			}
		}
		return result;
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isNotNullOrEmpty(String string) {
		return string != null && string.length() != 0;
	}

	public static int iv(Integer i) {
		return i == null ? 0 : i;
	}

	public static String join(Collection objects, String separator) {
		Object[] objs = (Object[]) objects.toArray(new Object[objects.size()]);
		return join(objs, separator);
	}

	public static String join(Object[] objects, String separator) {
		return join(objects, separator, false);
	}

	public static String join(Object[] objects, String separator,
			boolean ignoreEmpties) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : objects) {
			String app = obj == null ? "null" : obj.toString();
			if (sb.length() > 0 && (app.length() != 0 || !ignoreEmpties)) {
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
			result.append(phraseTemplate == null ? phrase : CommonUtils
					.formatJ(phraseTemplate, phrase));
		}
		return result.toString();
	}

	public static long lv(Long l) {
		return l == null ? 0 : l;
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

	public static String padStringLeft(String input, int length, char padChar) {
		input = input == null ? "(null)" : input;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length - input.length(); i++) {
			sb.append(padChar);
		}
		sb.append(input);
		return sb.toString();
	}

	public static String padStringRight(String input, int length, char padChar) {
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

	public static String pluralise(String s, Collection c) {
		if (c != null && c.size() == 1) {// note 0/null gives a plural form
			// (what a strange
			// language...)
			return s;
		}
		if (s.endsWith("s")) {
			return s;
		}
		if (s.endsWith("y")) {
			return s.substring(0, s.length() - 1) + "ies";
		}
		return s + "s";
	}

	// to 00.00:00 or 23:59.59
	@SuppressWarnings("deprecation")
	public static Date roundDate(Date d, boolean up) {
		d.setHours(up ? 23 : 0);
		d.setMinutes(up ? 59 : 0);
		d.setSeconds(up ? 59 : 0);
		return d;
	}

	@SuppressWarnings("deprecation")
	public static int getYear(Date d) {
		return d.getYear() + 1900;
	}

	public static <T extends Collection> T shallowCollectionClone(T coll) {
		try {
			T c = null;
			if (coll instanceof ArrayList) {
				c = (T) ((ArrayList) coll).clone();
			} else if (coll instanceof LinkedHashSet) {
				c = (T) ((LinkedHashSet) coll).clone();
			} else if (coll instanceof HashSet) {
				c = (T) ((HashSet) coll).clone();
			}
			return c;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String simpleClassName(Class c) {
		String s = c.getName();
		return s.substring(s.lastIndexOf('.') + 1);
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
			sb.append(upperCaseFirstLetterOnly(string));
		}
		return sb.toString();
	}

	public static String trimToWsChars(String s, int maxChars) {
		return trimToWsChars(s, maxChars, false);
	}

	public static String trimToWsChars(String s, int maxChars,
			boolean withDotDot) {
		if (maxChars < 0) {
			maxChars = 100;
		}
		if (s == null || s.length() <= maxChars) {
			return s;
		}
		if (s.substring(maxChars / 2, maxChars).indexOf(" ") == -1) {
			return s.substring(0, maxChars) + (withDotDot ? "..." : "");
		}
		return s.substring(0, s.substring(0, maxChars).lastIndexOf(' '))
				+ (withDotDot ? "..." : "");
	}

	public static String trimToWsReverse(String s, int maxChars) {
		if (s.length() <= maxChars) {
			return s;
		}
		if (maxChars < 0) {
			maxChars = 100;
		}
		String sub = s.substring(s.length() - maxChars);
		if (sub.indexOf(" ") > maxChars / 2) {
			return sub;
		}
		int firstSpace = sub.indexOf(' ');
		return firstSpace == -1 ? "" : sub.substring(firstSpace);
	}

	public static String upperCaseFirstLetterOnly(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
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

	public static boolean isStandardJavaClass(Class clazz) {
		return stdAndPrimitivesMap.containsValue(clazz);
	}

	public enum DateStyle {
		AU_DATE_SLASH, AU_DATE_MONTH, AU_DATE_TIME, AU_DATE_TIME_HUMAN,
		AU_DATE_TIME_MS, AU_SHORT_DAY, AU_DATE_DOT, AU_LONG_DAY,
		AU_SHORT_MONTH, AU_DATE_SLASH_MONTH, TIMESTAMP
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

	public static <T> T last(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	public static <T> T last(Iterator<T> iterator) {
		T last = null;
		while (iterator.hasNext()) {
			last = iterator.next();
		}
		return last;
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

	public static void addIfNotNull(List l, Object o) {
		if (o != null) {
			l.add(o);
		}
	}

	public static void putIfKeyNotNull(Map m, Object k, Object v) {
		if (k != null) {
			m.put(k, v);
		}
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

	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
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
}

/**
 *
 */
package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StringMap extends LinkedHashMap<String, String> {
	private static final transient long serialVersionUID = 8219302205025519855L;

	public static final StringMap EMPTY_PROPS = new StringMap();

	public static StringMap fromKvStringList(String list) {
		return fromKvStringList(list, true);
	}

	public static StringMap fromKvStringList(String list, boolean trim) {
		StringMap map = new StringMap();
		if (list == null) {
			return map;
		}
		String[] lines = list.split("\n");
		if (lines.length % 2 == 1) {
			List<String> trunc = Arrays.asList(lines).subList(0,
					lines.length / 2 * 2);
			lines = (String[]) trunc.toArray(new String[trunc.size()]);
		}
		for (int i = 0; i < lines.length; i += 2) {
			String key = lines[i];
			String value = lines[i + 1];
			if (trim) {
				key = key.trim();
				value = value.trim();
			}
			map.put(unescapeList(key), unescapeList(value));
		}
		return map;
	}

	public static StringMap fromPropertyString(String props) {
		return fromPropertyString(props, false);
	}

	public static StringMap fromPropertyString(String props, boolean unQuote) {
		StringMap map = new StringMap();
		if (props == null) {
			return map;
		}
		Escaper replacer = new Escaper();
		char[] from = { 'n', '=', '\\' };
		String[] to = { "\n", "=", "\\" };
		for (String line : props.split("\n")) {
			int idx = line.indexOf("=");
			if (idx != -1 && !line.startsWith("#")) {
				int end = line.length();
				int idx1 = idx + 1;
				if (unQuote && idx1 < line.length() && line.charAt(idx1) == '\"'
						&& line.charAt(end - 1) == '\"') {
					idx1++;
					end--;
				}
				String value = line.substring(idx1, end);
				String unescaped = replacer.unescapeBackslahed(value, from, to);
				String key = line.substring(0, idx);
				map.put(key, unescaped);
			}
		}
		return map;
	}

	public static StringMap fromStringList(String list) {
		StringMap map = new StringMap();
		if (list == null) {
			return map;
		}
		for (String line : list.split("\n")) {
			if (!line.startsWith("#") && !line.trim().isEmpty()) {
				map.put(line.trim(), "true");
			}
		}
		return map;
	}

	public static StringMap properties(String... kvs) {
		StringMap map = new StringMap();
		for (int i = 0; i < kvs.length; i += 2) {
			map.put(kvs[i], kvs[i + 1]);
		}
		return map;
	}

	public static StringMap property(String key, String value) {
		StringMap map = new StringMap();
		map.put(key, value);
		return map;
	}

	private static String unescapeList(String string) {
		char[] from = { 'n', 'r', '\\' };
		String[] to = { "\n", "\r", "\\" };
		return new Escaper().unescapeBackslahed(string, from, to);
	}

	public StringMap() {
	}

	public StringMap(List<String> list) {
		for (Iterator<String> itr = list.iterator(); itr.hasNext();) {
			put(itr.next(), itr.next());
		}
	}

	public StringMap(Map<String, String> otherMap) {
		super(otherMap);
	}

	public Map<String, String> asLinkedHashMap() {
		return new LinkedHashMap<>(this);
	}

	@Override
	public StringMap clone() {
		return new StringMap(this);
	}

	public boolean contains(String s1, String s2) {
		return containsKey(s1) && Objects.equals(get(s1), s2);
	}

	public boolean existsAndIsBooleanFalse(String key) {
		return containsKey(key) && !is(key);
	}

	public String firstKey() {
		return size() == 0 ? null : keySet().iterator().next();
	}

	public boolean is(String key) {
		return Boolean.valueOf(get(key));
	}

	public boolean matchesPatternKeys(String string) {
		for (Map.Entry<String, String> entry : entrySet()) {
			if (Ax.matches(string, entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	public String replaceMatch(String string) {
		return containsKey(string) ? get(string) : string;
	}

	public String replaceSubstrings(String string) {
		for (Map.Entry<String, String> entry : entrySet()) {
			string = string.replace(entry.getKey(), entry.getValue());
		}
		return string;
	}

	public void setBoolean(String key, boolean value) {
		put(key, String.valueOf(value));
	}

	public void setBooleanOrRemove(String key, boolean value) {
		if (value) {
			put(key, String.valueOf(true));
		} else {
			remove(key);
		}
	}

	public StringMap sorted() {
		return sorted(Comparator.naturalOrder());
	}

	public StringMap sorted(Comparator<String> comparator) {
		StringMap result = new StringMap();
		keySet().stream().sorted(comparator)
				.forEach(key -> result.put(key, get(key)));
		return result;
	}

	public String toKvStringList() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : entrySet()) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(entry.getKey());
			sb.append("\n");
			sb.append(escapeListString(entry.getValue()));
		}
		return sb.toString();
	}

	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		char[] from = { '\n', '=', '\\' };
		String[] to = { "\\n", "\\=", "\\\\" };
		for (Map.Entry<String, String> entry : entrySet()) {
			String value = entry.getValue();
			if (value == null) {
				continue;
			}
			if (sb.length() != 0) {
				sb.append("\n");
			}
			String key = entry.getKey();
			sb.append(key);
			sb.append('=');
			sb.append(new Escaper().escape(value, from, to));
		}
		return sb.toString();
	}

	private String escapeListString(String string) {
		char[] from = { '\n', '\r', '\\' };
		String[] to = { "\\n", "\\r", "\\\\" };
		return new Escaper().escape(string, from, to);
	}

	public static class Escaper {
		public String escape(String string, char[] from, String[] to) {
			StringBuilder sb = new StringBuilder();
			int end = string.length();
			for (int idx = 0; idx < end;) {
				char c = string.charAt(idx++);
				boolean replaced = false;
				for (int idx1 = 0; idx1 < from.length; idx1++) {
					char check = from[idx1];
					if (c == check) {
						sb.append(to[idx1]);
						replaced = true;
						break;
					}
				}
				if (!replaced) {
					sb.append(c);
				}
			}
			return sb.toString();
		}

		public String unescapeBackslahed(String string, char[] from,
				String[] to) {
			StringBuilder sb = new StringBuilder();
			int end = string.length() - 1;
			int idx = 0;
			for (; idx < end;) {
				char c = string.charAt(idx++);
				if (c == '\\') {
					char next = string.charAt(idx++);
					boolean replaced = false;
					for (int idx1 = 0; idx1 < from.length; idx1++) {
						char check = from[idx1];
						if (next == check) {
							sb.append(to[idx1]);
							replaced = true;
							break;
						}
					}
					if (!replaced) {
						sb.append(c);
						sb.append(next);
					}
				} else {
					sb.append(c);
				}
			}
			if (idx < string.length()) {
				char c = string.charAt(idx++);
				sb.append(c);
			}
			return sb.toString();
		}
	}

	public void setTrue(String key) {
		setBoolean(key, true);
	}
}

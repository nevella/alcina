/**
 *
 */
package cc.alcina.framework.common.client.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
			map.put(unescape(key), unescape(value));
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
		for (String line : props.split("\n")) {
			int idx = line.indexOf("=");
			if (idx != -1 && !line.startsWith("#")) {
				String value = line.substring(idx + 1).replace("\\n", "\n")
						.replace("\\=", "=").replace("\\\\", "\\");
				if (unQuote && value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				map.put(line.substring(0, idx), value);
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

	private static String unescape(String string) {
		return string.replace("\\n", "\n").replace("\\r", "\r").replace("\\\\",
				"\\");
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
		return new StringMap(new TreeMap(this));
	}

	public String toKvStringList() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : entrySet()) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(entry.getKey());
			sb.append("\n");
			sb.append(escape(entry.getValue()));
		}
		return sb.toString();
	}

	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(entry.getValue().replace("\\", "\\\\").replace("=", "\\=")
					.replace("\n", "\\n"));
		}
		return sb.toString();
	}

	private String escape(String string) {
		return string.replace("\\", "\\\\").replace("\r", "\\r").replace("\n",
				"\\n");
	}
}

/**
 * 
 */
package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class StringMap extends LinkedHashMap<String, String> {
	public static final StringMap EMPTY_PROPS = new StringMap();

	public static StringMap property(String key, String value) {
		StringMap map = new StringMap();
		map.put(key, value);
		return map;
	}

	public static StringMap properties(String... kvs) {
		StringMap map = new StringMap();
		for (int i = 0; i < kvs.length; i += 2) {
			map.put(kvs[i], kvs[i + 1]);
		}
		return map;
	}

	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : entrySet()) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(entry.getValue().replace("\\", "\\\\")
					.replace("=", "\\=").replace("\n", "\\n"));
		}
		return sb.toString();
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
			if (idx != -1) {
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

	public void setBooleanOrRemove(String key, boolean value) {
		if (value) {
			put(key, String.valueOf(true));
		} else {
			remove(key);
		}
	}
}
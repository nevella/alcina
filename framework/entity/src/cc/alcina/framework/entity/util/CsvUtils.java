package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.StringMap;

public class CsvUtils {
	public static List<List<String>> parseCsv(String txt) {
		txt = txt.replaceAll("[\\r\\n]+[\\r\\n]*", "\n");
		txt = txt.replaceAll("\t", ",");
		List<List<String>> results = new ArrayList<List<String>>();
		List<String> row = null;
		int i = 0;
		int colCount = 0;
		while (i < txt.length()) {
			if (row == null) {
				row = new ArrayList<String>();
				results.add(row);
			}
			char c = txt.charAt(i);
			int end = -1;
			String value = null;
			if (c == '"') {
				end = i + 1;
				while (true) {
					end = txt.indexOf('"', end);
					if (end < txt.length() - 1 && txt.charAt(end + 1) == '"') {
						end += 2;
					} else {
						break;
					}
				}
				value = txt.substring(i + 1, end).replace("\"\"", "\"");
				i = end + 1;
			}
			int i1 = txt.indexOf(',', i);
			int i2 = txt.indexOf('\n', i);
			if (i1 > -1 && i2 > -1) {
				end = Math.min(i1, i2);
			} else {
				end = Math.max(i1, i2);
			}
			if (end == -1) {
				end = txt.length();
			}
			value = value != null ? value : txt.substring(i, end);
			row.add(value);
			if (end == i2) {
				if (colCount == 0) {
					colCount = row.size();
				} else {
					while (row.size() < colCount) {
						row.add("");
					}
				}
				row = null;
			}
			i = end + 1;
		}
		if (row != null) {
			while (row.size() < colCount) {
				row.add("");
			}
		}
		return results;
	}

	static Pattern wrapInQuotesPattern = Pattern.compile("[ ,a-zA-Z]");

	public static String asCsvRow(Collection values) {
		int i = 0;
		StringBuilder sb = new StringBuilder();
		for (Object object : values) {
			String value = object.toString();
			if (i++ > 0) {
				sb.append(",");
			}
			sb.append(wrapInQuotesPattern.matcher(value).find()
					? "\"" + value + "\"" : value);
		}
		return sb.toString();
	}

	public static StringBuilder headerValuesToCsv(List<String> headers,
			List<List<String>> values) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < headers.size(); i++) {
			String header = headers.get(i);
			headers.set(i, header.replace(" ", "_"));
		}
		sb.append(asCsvRow(headers));
		sb.append("\n");
		for (int j = 0; j < values.size(); j++) {
			List<String> row = values.get(j);
			for (int i = 0; i < row.size(); i++) {
				String cell = row.get(i);
				row.set(i, cell == null ? "" : cell.replace("\n", "\\n"));
			}
			sb.append(asCsvRow(row));
			sb.append("\n");
		}
		return sb;
	}

	public static class CsvRow {
		private int rowIdx;

		private CsvCols csvCols;

		public CsvRow(CsvCols csvCols, int idx) {
			this.csvCols = csvCols;
			this.rowIdx = idx;
		}

		public String get(String key) {
			return csvCols.grid.get(rowIdx).get(getColumnIndex(key));
		}

		public String set(String key, String value) {
			ArrayList<String> list = (ArrayList<String>) csvCols.grid
					.get(rowIdx);
			int columnIndex = getColumnIndex(key);
			for (; list.size() <= columnIndex;) {
				list.add("");
			}
			return list.set(columnIndex, value);
		}

		private int getColumnIndex(String key) {
			Integer index = csvCols.colLookup.get(key);
			if (index != null) {
				return index;
			}
			index = csvCols.colLcLookup.get(key.toLowerCase());
			if (index != null) {
				return index;
			}
			return -1;
		}

		public boolean has(String key) {
			return get(key).length() > 0;
		}

		public Map<String, String> map() {
			StringMap map = new StringMap();
			for (String header : csvCols.headers()) {
				map.put(header, get(header));
			}
			return map;
		}

		public boolean containsKey(String key) {
			return getColumnIndex(key) != -1;
		}

		@Override
		public String toString() {
			return map().entrySet().stream().map(Object::toString)
					.collect(Collectors.joining("\n"));
		}
	}

	public static class CsvCols implements Iterable<CsvRow>, Iterator<CsvRow> {
		Map<String, Integer> colLookup = new LinkedHashMap<>();

		Map<String, Integer> colLcLookup = new LinkedHashMap<>();

		public Map<String, CsvRow> rowLookup(String columnHeader) {
			Map<String, CsvRow> result = new LinkedHashMap<>();
			while (hasNext()) {
				CsvRow row = next();
				result.put(row.get(columnHeader), row);
			}
			return result;
		}

		int idx = 0;

		private List<List<String>> grid;

		public List<String> headers() {
			return colLookup.keySet().stream().collect(Collectors.toList());
		}

		public CsvCols(String csv) {
			this(parseCsv(csv));
		}

		public CsvCols(List<List<String>> grid) {
			this.grid = grid;
			grid.get(0)
					.forEach(s -> colLookup.put(
							s.trim().replace("\"", "").replace("\ufeff", ""),
							idx++));
			colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
			idx = 1;
		}

		@Override
		public Iterator<CsvRow> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return idx < grid.size();
		}

		@Override
		public CsvRow next() {
			return new CsvRow(this, idx++);
		}

		public void addColumn(String string) {
			colLookup.put(string, colLookup.size());
			colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
		}

		public String toCsv() {
			return headerValuesToCsv(
					colLookup.keySet().stream().collect(Collectors.toList()),
					grid.subList(1, grid.size())).toString();
		}
	}
}

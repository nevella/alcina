package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import cc.alcina.framework.entity.util.CsvUtils.CsvCols.CsvRow;

public class CsvUtils {
	public static List<List<String>> parseCsv(String txt) {
		txt = txt.replaceAll("[\\r\\n]+[\\r\\n]*", "\n");
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
					if (end < txt.length() && txt.charAt(end + 1) == '"') {
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
			sb.append(wrapInQuotesPattern.matcher(value).find() ? "\"" + value
					+ "\"" : value);
		}
		return sb.toString();
	}

	public static class CsvCols implements Iterable<CsvRow>, Iterator<CsvRow> {
		Map<String, Integer> colLookup = new LinkedHashMap<>();

		int idx = 0;

		private List<List<String>> grid;

		public CsvCols(List<List<String>> grid) {
			this.grid = grid;
			grid.get(0).forEach(s -> colLookup.put(s.trim(), idx++));
			idx = 1;
		}

		public class CsvRow {
			private int rowIdx;

			public CsvRow(int idx) {
				this.rowIdx = idx;
			}

			public String get(String key) {
				return grid.get(rowIdx).get(colLookup.get(key));
			}
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
			return new CsvRow(idx++);
		}
	}
}

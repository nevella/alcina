package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class CsvUtils {
	static Pattern wrapInQuotesPattern = Pattern.compile("[,\"\\\\]");

	public static String asCsvRow(Collection values) {
		return asOutputRow(values, false);
	}

	private static String asOutputRow(Collection values, boolean tsv) {
		int i = 0;
		StringBuilder sb = new StringBuilder();
		for (Object object : values) {
			String value = object == null ? "" : object.toString();
			if (i++ > 0) {
				sb.append(tsv ? "\t" : ",");
			}
			value = value.replace("\n", "\\n");
			value = value.replace("\"", "\"\"");
			sb.append(wrapInQuotesPattern.matcher(value).find()
					? "\"" + value + "\""
					: value);
		}
		return sb.toString();
	}

	public static StringBuilder headerValuesToCsv(List<String> headers,
			List<List<String>> values) {
		return headerValuesToCsv(headers, values, false);
	}

	public static StringBuilder headerValuesToCsv(List<String> headers,
			List<List<String>> values, boolean quoteHeaders) {
		return headerValuesToOutput(headers, values, quoteHeaders, false);
	}

	static StringBuilder headerValuesToOutput(List<String> headers,
			List<List<String>> values, boolean quoteHeaders, boolean tsv) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < headers.size(); i++) {
			String header = headers.get(i);
			if (quoteHeaders) {
			} else {
				headers.set(i, header.replace(" ", "_"));
			}
		}
		sb.append(asOutputRow(headers, tsv));
		sb.append("\n");
		for (int j = 0; j < values.size(); j++) {
			List<String> row = values.get(j);
			sb.append(asOutputRow(row, tsv));
			sb.append("\n");
		}
		return sb;
	}

	public static List<List<String>> parseCsv(String txt) {
		return parseCsv(txt, false);
	}

	public static List<List<String>> parseCsv(String txt, boolean tsv) {
		txt = txt.replaceAll("[\\r\\n]+[\\r\\n]*", "\n");
		if (tsv) {
			StringBuilder sb = new StringBuilder();
			int lineIdx = 0;
			for (String line : txt.split("\n")) {
				if (lineIdx++ > 0) {
					sb.append('\n');
				}
				int cellIdx = 0;
				for (String cell : line.split("\t")) {
					if (cellIdx++ > 0) {
						sb.append(',');
					}
					if (cell.matches("\".*\"")) {
						sb.append(cell);
					} else {
						sb.append('"');
						sb.append(cell);
						sb.append('"');
					}
				}
			}
			txt = sb.toString();
		}
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
					int close = txt.indexOf('"', end);
					if (close == -1) {
						break;
					}
					end = close;
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
}

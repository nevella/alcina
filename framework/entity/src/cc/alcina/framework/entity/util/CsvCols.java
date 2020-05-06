package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;

public class CsvCols
		implements Iterable<CsvCols.CsvRow>, Iterator<CsvCols.CsvRow> {
	public static CsvCols parse(File file) {
		return new CsvCols(ResourceUtilities.read(file));
	}

	Map<String, Integer> colLookup = new LinkedHashMap<>();

	Map<String, Integer> colLcLookup = new LinkedHashMap<>();

	int idx = 0;

	List<List<String>> grid;

	public CsvCols(List<List<String>> grid) {
		this.grid = grid;
		if (grid.size() == 0) {
			grid.add(new ArrayList<>());
		}
		grid.get(0).forEach(s -> colLookup
				.put(s.trim().replace("\"", "").replace("\ufeff", ""), idx++));
		colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
		reset();
	}

	public CsvCols(String csv) {
		this(CsvUtils.parseCsv(csv));
	}

	public void addColumn(String string) {
		colLookup.put(string, colLookup.size());
		colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
	}

	public CsvCols.CsvRow addRow() {
		grid.add(new ArrayList<String>());
		return next();
	}

	@Override
	public boolean hasNext() {
		return idx < grid.size();
	}

	public List<String> headers() {
		return colLookup.keySet().stream().collect(Collectors.toList());
	}

	@Override
	public Iterator<CsvCols.CsvRow> iterator() {
		return this;
	}

	@Override
	public CsvCols.CsvRow next() {
		return new CsvCols.CsvRow(this, idx++);
	}

	public void preserveColumns(String string) {
		List<String> colNames = Arrays.asList(string.split(","));
		List<String> keys = colLookup.keySet().stream()
				.collect(Collectors.toList());
		Collections.reverse(keys);
		;
		for (String k : keys) {
			if (!colNames.contains(k)) {
				int idx = colLookup.get(k);
				grid.forEach(row -> row.remove(idx));
			}
		}
		colLookup.keySet().removeIf(k -> !colNames.contains(k));
	}

	public void reset() {
		idx = 1;
	}

	public Map<String, CsvCols.CsvRow> rowLookup(String columnHeader) {
		Map<String, CsvCols.CsvRow> result = new LinkedHashMap<>();
		reset();
		while (hasNext()) {
			CsvCols.CsvRow row = next();
			result.put(row.get(columnHeader), row);
		}
		return result;
	}

	public int size() {
		return grid.size() - 1;
	}

	public Stream<CsvCols.CsvRow> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	public String toCsv() {
		return toCsv(false);
	}

	public String toCsv(boolean quoteHeaders) {
		return CsvUtils.headerValuesToCsv(
				colLookup.keySet().stream().collect(Collectors.toList()),
				grid.subList(1, grid.size()), quoteHeaders).toString();
	}

	public static class CsvRow {
		private int rowIdx;

		private CsvCols csvCols;

		public CsvRow(CsvCols csvCols, int idx) {
			this.csvCols = csvCols;
			this.rowIdx = idx;
		}

		public boolean containsKey(String key) {
			return getColumnIndex(key) != -1;
		}

		public String get(String key) {
			return csvCols.grid.get(rowIdx).get(getColumnIndex(key));
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

		public String set(String key, Object value) {
			ArrayList<String> list = (ArrayList<String>) csvCols.grid
					.get(rowIdx);
			int columnIndex = getColumnIndex(key);
			for (; list.size() <= columnIndex;) {
				list.add("");
			}
			return list.set(columnIndex,
					value == null ? null : String.valueOf(value));
		}

		@Override
		public String toString() {
			return map().entrySet().stream().map(Object::toString)
					.collect(Collectors.joining("\n"));
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
	}
}
package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionUtil;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;

/**
 * <p>
 * A fairly rich model of a csv (or tsv) document, with various input/output
 * methods
 * 
 * <p>
 * To parse csv/tsv text, call {@link #parseCsv(String)} or
 * {@link #parseTsv(String)}
 * 
 * <p>
 * To convert a collection of beans to a CSV,
 */
public class Csv implements Iterable<Csv.Row>, Iterator<Csv.Row> {
	public static class Row {
		private int rowIdx;

		private Csv csv;

		public Row(Csv csv, int idx) {
			this.csv = csv;
			this.rowIdx = idx;
		}

		public boolean containsKey(String key) {
			return getColumnIndex(key) != -1;
		}

		public String get(Enum key) {
			return get(Ax.friendly(key));
		}

		public String get(String key) {
			return csv.grid.get(rowIdx).get(getColumnIndex(key));
		}

		public boolean getBoolean(String key) {
			return Boolean.parseBoolean(get(key));
		}

		public long getLong(String key) {
			String s = get(key);
			return Ax.isBlank(s) ? -1 : Long.parseLong(s);
		}

		public int getRowIdx() {
			return this.rowIdx;
		}

		public boolean has(String key) {
			return get(key).length() > 0;
		}

		public Map<String, String> map() {
			StringMap map = new StringMap();
			for (String header : csv.headers()) {
				map.put(header, get(header));
			}
			return map;
		}

		public Stream<String> values() {
			return map().values().stream();
		}

		public void set(Enum e, Object value) {
			set(Ax.friendly(e), value);
		}

		public String set(String key, Object value) {
			ArrayList<String> list = (ArrayList<String>) csv.grid.get(rowIdx);
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
			Integer index = csv.colLookup.get(key);
			if (index != null) {
				return index;
			}
			index = csv.colLcLookup.get(key.toLowerCase());
			if (index != null) {
				return index;
			}
			index = csv.colLcLookup.get(key.toLowerCase().replace(' ', '_'));
			if (index != null) {
				return index;
			}
			return -1;
		}
	}

	/**
	 * Output a collection of beans as a separated-value string
	 */
	public static class FromCollection {
		Collection<?> collection;

		boolean quoteHeaders;

		boolean tsv;

		public FromCollection(Collection<?> collection) {
			this.collection = collection;
		}

		public FromCollection withQuoteHeaders(boolean quoteHeaders) {
			this.quoteHeaders = quoteHeaders;
			return this;
		}

		public FromCollection withTsv(boolean tsv) {
			this.tsv = tsv;
			return this;
		}

		public Csv toCsv() {
			Csv csv = new Csv("");
			Class<?> commonType = CollectionUtil.getCommonType(collection);
			List<Property> properties = Reflections.at(commonType).properties()
					.stream().filter(Property::isReadable).toList();
			properties.stream().map(Property::getName).forEach(csv::addColumn);
			collection.forEach(elem -> {
				Row row = csv.addRow();
				properties.stream()
						.forEach(p -> row.set(p.getName(), p.get(elem)));
			});
			return csv;
		}

		public String toOutputString() {
			return toCsv().toOutputString(quoteHeaders, tsv);
		}
	}

	public static Csv parse(File file) {
		return new Csv(Io.read().file(file).asString());
	}

	public static Csv parse(String xsv, boolean tsv) {
		return new Csv(CsvUtils.parseCsv(xsv, tsv));
	}

	public static Csv parseCsv(String csv) {
		return new Csv(csv);
	}

	public static Csv blankWithKeys(Class<? extends Enum> columnType) {
		Csv csv = new Csv(List.of());
		Arrays.stream(columnType.getEnumConstants()).forEach(csv::addColumn);
		return csv;
	}

	public static Csv parseTsv(String tsv) {
		return new Csv(CsvUtils.parseCsv(tsv, true));
	}

	public static FromCollection fromCollection(Collection<?> collection) {
		return new FromCollection(collection);
	}

	Map<String, Integer> colLookup = new LinkedHashMap<>();

	Map<String, Integer> colLcLookup = new LinkedHashMap<>();

	int idx = 0;

	List<List<String>> grid;

	public Csv(List<List<String>> gridParam) {
		this.grid = new ArrayList<>(gridParam);
		if (grid.size() == 0) {
			grid.add(new ArrayList<>());
		}
		grid.get(0).forEach(s -> colLookup
				.put(s.trim().replace("\"", "").replace("\ufeff", ""), idx++));
		colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
		reset();
	}

	public Csv(String csv) {
		this(CsvUtils.parseCsv(csv));
	}

	public void addColumn(Enum e) {
		addColumn(Ax.friendly(e));
	}

	public void addColumn(String string) {
		colLookup.put(string, colLookup.size());
		colLookup.forEach((k, v) -> colLcLookup.put(k.toLowerCase(), v));
		grid.forEach(list -> list.add(""));
	}

	public Csv.Row addRow() {
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
	public Iterator<Csv.Row> iterator() {
		return this;
	}

	@Override
	public Csv.Row next() {
		return new Csv.Row(this, idx++);
	}

	public void preserveColumns(String string) {
		List<String> colNames = Arrays.asList(string.split(","));
		List<String> keys = colLookup.keySet().stream()
				.collect(Collectors.toList());
		Collections.reverse(keys);
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

	public Map<String, Csv.Row> rowLookup(String columnHeader) {
		Map<String, Csv.Row> result = new LinkedHashMap<>();
		reset();
		while (hasNext()) {
			Csv.Row row = next();
			result.put(row.get(columnHeader), row);
		}
		return result;
	}

	public int size() {
		return grid.size() - 1;
	}

	public Stream<Csv.Row> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	public String toCsvString() {
		return toCsvString(false);
	}

	public String toCsvString(boolean quoteHeaders) {
		return toOutputString(quoteHeaders, false);
	}

	public String toTsvString(boolean quoteHeaders) {
		return toOutputString(quoteHeaders, true);
	}

	public void write(String path) {
		Io.write().string(toCsvString()).toPath(path);
	}

	String toOutputString(boolean quoteHeaders, boolean tsv) {
		return CsvUtils
				.headerValuesToOutput(
						colLookup.keySet().stream()
								.collect(Collectors.toList()),
						grid.subList(1, grid.size()), quoteHeaders, tsv)
				.toString();
	}
}
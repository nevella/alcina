package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class ReportUtils {
	public static void dumpTable(UnsortedMultikeyMap<String> values,
			List<String> columnNames) {
		dumpTable(values, columnNames, false, false);
	}

	public static void dumpTable(UnsortedMultikeyMap<String> values,
			List<String> columnNames, boolean stringKeyedColumns,
			boolean invertAxes) {
		if (invertAxes) {
			int rowCount = values.keySet().size();
			values = transformValues(values, columnNames, stringKeyedColumns);
			columnNames = new ArrayList<String>();
			columnNames.add("Key");
			for (int i = 0; i < rowCount; i++) {
				columnNames.add(String.valueOf(i + 1));
			}
			stringKeyedColumns = false;
		}
		Map<Integer, Integer> colWidthMap = new HashMap<Integer, Integer>();
		int rows = values.keySet().size();
		int cols = columnNames.size();
		for (int col = 0; col < cols; col++) {
			colWidthMap.put(col, 5);
		}
		for (Object row : values.keySet()) {
			for (int col = 0; col < cols; col++) {
				String value = getValue(values, row, col, columnNames,
						stringKeyedColumns);
				int max = Math.max(CommonUtils.iv(colWidthMap.get(col)),
						value.length());
				max = Math.max(max, columnNames.get(col).length());
				colWidthMap.put(col, max);
			}
		}
		if (cols > 1) {
			for (int col = 0; col < cols; col++) {
				System.out.format(" %-" + colWidthMap.get(col) + "s  |",
						columnNames.get(col));
			}
			System.out.println();
			for (int col = 0; col < cols; col++) {
				System.out.format(CommonUtils.padStringLeft("----",
						colWidthMap.get(col) + 6, "-"));
			}
		}
		System.out.println();
		for (Object row : values.keySet()) {
			if (cols == 1) {
				String value = getValue(values, row, 0, columnNames,
						stringKeyedColumns);
				System.out.println(value);
			} else {
				for (int col = 0; col < cols; col++) {
					String value = getValue(values, row, col, columnNames,
							stringKeyedColumns);
					System.out.format(" %-" + colWidthMap.get(col) + "s  |",
							value);
				}
				System.out.println();
			}
		}
	}

	private static UnsortedMultikeyMap<String> transformValues(
			UnsortedMultikeyMap<String> values, List<String> columnNames,
			boolean stringKeyedColumns) {
		UnsortedMultikeyMap<String> result = new UnsortedMultikeyMap<String>();
		Map<String, Integer> colIndexLookup = new LinkedHashMap<String, Integer>();
		for (String colName : columnNames) {
			colIndexLookup.put(colName, colIndexLookup.size());
		}
		for (String colName : columnNames) {
			Integer colIndex = colIndexLookup.get(colName);
			Object colKey = stringKeyedColumns ? colName : colIndex;
			int transColIdx=0;
			result.put(colIndex, transColIdx++, colName);
			for (Object rowKey : values.keySet()) {
				result.put(colIndex, transColIdx++, values.get(rowKey, colName));
			}
		}
		return result;
	}

	private static String getValue(UnsortedMultikeyMap<String> values,
			Object row, int col, List<String> columnNames,
			boolean stringKeyedColumns) {
		Object key2 = stringKeyedColumns ? columnNames.get(col) : col;
		String value = CommonUtils.nullToEmpty(values.get(row, key2));
		return value;
	}
}

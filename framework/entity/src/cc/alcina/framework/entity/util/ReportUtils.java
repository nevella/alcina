package cc.alcina.framework.entity.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class ReportUtils {
	public static void dumpTable(UnsortedMultikeyMap<String> values,
			List<String> columnNames) {
		Map<Integer, Integer> colWidthMap = new HashMap<Integer, Integer>();
		int rows = values.keySet().size();
		int cols = columnNames.size();
		for (int col = 0; col < cols; col++) {
			colWidthMap.put(col, 5);
		}
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				int max = Math.max(CommonUtils.iv(colWidthMap.get(col)), values
						.get(row, col).length());
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
		for (int row = 0; row < rows; row++) {
			if (cols == 1) {
				System.out.println(values.get(row, 0));
			} else {
				for (int col = 0; col < cols; col++) {
					System.out.format(" %-" + colWidthMap.get(col) + "s  |",
							values.get(row, col));
				}
				System.out.println();
			}
		}
	}
}

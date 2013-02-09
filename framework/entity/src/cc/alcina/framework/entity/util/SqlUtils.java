package cc.alcina.framework.entity.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;

public class SqlUtils {
	public static final String CONTEXT_LOG_EVERY_X_RECORDS = SqlUtils.class
			.getName() + ".CONTEXT_LOG_EVERY_X_RECORDS";

	public static Map<Long, Long> toIdMap(Statement stmt, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		System.out.println("Query: " + sql);
		ResultSet rs = stmt.executeQuery(sql);
		Map<Long, Long> result = toIdMap(rs, fn1, fn2);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	private static Map<Long, Long> toIdMap(ResultSet rs, String fn1, String fn2)
			throws SQLException {
		int log = CommonUtils.iv(LooseContext
				.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Map<Long, Long> result = new TreeMap<Long, Long>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getLong(fn2));
			if (log > 0 && result.size() % log == 0) {
				System.out.println(result.size() + "...");
			}
		}
		return result;
	}

	public static Set<Long> toIdList(Statement stmt, String sql,
			String fieldName) throws SQLException {
		MetricLogging.get().start("query");
		System.out.println("Query: " + sql);
		ResultSet rs = stmt.executeQuery(sql);
		Set<Long> result = toIdList(rs, fieldName);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	private static Set<Long> toIdList(ResultSet rs, String fieldName)
			throws SQLException {
		int log = CommonUtils.iv(LooseContext
				.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Set<Long> result = new TreeSet<Long>();
		while (rs.next()) {
			result.add(rs.getLong(fieldName));
			if (log > 0 && result.size() % log == 0) {
				System.out.println(result.size() + "...");
			}
		}
		return result;
	}

	public static void dumpResultSet(ResultSet rs) throws SQLException {
		LookupMapToMap<String> values = new LookupMapToMap<String>(2);
		int row = 0;
		List<String> columnNames = new ArrayList<String>();
		while (rs.next()) {
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				String value = CommonUtils.nullToEmpty(rs.getString(i));
				int j = i - 1;
				if (j >= columnNames.size()) {
					columnNames.add(rs.getMetaData().getColumnName(i));
				}
				values.put(row, j, value);
			}
			row++;
		}
		dumpTable(values, columnNames);
	}

	private static void dumpTable(LookupMapToMap<String> values,
			List<String> columnNames) {
		Map<Integer, Integer> colWidthMap = new HashMap<Integer, Integer>();
		int rows = values.keySet().size();
		int cols = 0;
		for (int row = 0; row < rows; row++) {
			cols = ((Map) values.get(row)).size();
			for (int col = 0; col < cols; col++) {
				int max = Math.max(CommonUtils.iv(colWidthMap.get(col)), values
						.get(row, col).length());
				max = Math.max(max, columnNames.get(col).length());
				colWidthMap.put(col, max);
			}
		}
		for (int col = 0; col < cols; col++) {
			System.out.format(" %-" + colWidthMap.get(col) + "s  |",
					columnNames.get(col));
		}
		System.out.println();
		for (int col = 0; col < cols; col++) {
			System.out.format(CommonUtils.padStringLeft("----",
					colWidthMap.get(col) + 4, "-"));
		}
		System.out.println();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				System.out.format(" %-" + colWidthMap.get(col) + "s  |",
						values.get(row, col));
			}
			System.out.println();
		}
	}
}

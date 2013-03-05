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
		return toIdList(stmt, sql, fieldName, true);
	}

	public static Set<Long> toIdList(Statement stmt, String sql,
			String fieldName, boolean dumpQuerySql) throws SQLException {
		MetricLogging.get().start("query");
		if (dumpQuerySql) {
			System.out.println(sql);
		}
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
		dumpResultSet(rs, new HashMap<String, SqlUtils.ColumnFormatter>());
	}

	public static interface ColumnFormatter {
		public String format(ResultSet rs, int columnIndex) throws SQLException;
	}

	public static void dumpResultSet(ResultSet rs,
			Map<String, ColumnFormatter> formatters) throws SQLException {
		LookupMapToMap<String> values = new LookupMapToMap<String>(2);
		int row = 0;
		List<String> columnNames = new ArrayList<String>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			columnNames.add(rs.getMetaData().getColumnName(i));
		}
		while (rs.next()) {
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				String columnName = columnNames.get(i - 1);
				String value = null;
				if (formatters.containsKey(columnName)) {
					value = formatters.get(columnName).format(rs, i);
				} else {
					value = CommonUtils.nullToEmpty(rs.getString(i));
				}
				int j = i - 1;
				values.put(row, j, value);
			}
			row++;
		}
		ReportUtils.dumpTable(values, columnNames);
		if (row == 0) {
			System.out.println("No rows returned");
		}
	}
}

package cc.alcina.framework.entity.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ReportUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;

public class SqlUtils {
	public static final String CONTEXT_LOG_EVERY_X_RECORDS = SqlUtils.class
			.getName() + ".CONTEXT_LOG_EVERY_X_RECORDS";

	public static void closeConnection(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dumpResultSet(ResultSet rs) throws SQLException {
		dumpResultSet(rs, new HashMap<String, SqlUtils.ColumnFormatter>());
	}

	public static void dumpResultSet(ResultSet rs,
			Map<String, ColumnFormatter> formatters) throws SQLException {
		List<String> columnNames = getColumnNames(rs);
		UnsortedMultikeyMap<String> values = getValues(rs, formatters,
				columnNames, null);
		ReportUtils.dumpTable(values, columnNames);
		if (values.keySet().isEmpty()) {
			System.out.println("No rows returned");
		}
	}

	public static <T> List<T> getMapped(Statement stmt, String sql,
			ThrowingFunction<ResultSet, T> mapper) {
		try {
			ResultSet rs = stmt.executeQuery(sql);
			List<T> result = new ArrayList<>();
			while (rs.next()) {
				result.add(mapper.apply(rs));
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T getValue(Statement stmt, String sql, Class clazz) {
		try {
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			if (clazz == String.class) {
				return (T) rs.getString(1);
			} else if (clazz == Long.class) {
				return (T) Long.valueOf(rs.getLong(1));
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static UnsortedMultikeyMap<String> getValues(ResultSet rs,
			Map<String, ColumnFormatter> formatters, List<String> columnNames,
			String keyField) throws SQLException {
		return getValues(rs, formatters, columnNames, keyField, false);
	}

	public static UnsortedMultikeyMap<String> getValues(ResultSet rs,
			Map<String, ColumnFormatter> formatters, List<String> columnNames,
			String keyField, boolean multiple) throws SQLException {
		columnNames = columnNames == null ? getColumnNames(rs) : columnNames;
		int row = 0;
		UnsortedMultikeyMap<String> values = new UnsortedMultikeyMap<String>(
				multiple ? 3 : 2);
		while (rs.next()) {
			Object key = keyField == null ? row : rs.getString(keyField);
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				String columnName = columnNames.get(i - 1);
				String value = null;
				if (formatters != null && formatters.containsKey(columnName)) {
					value = formatters.get(columnName).format(rs, i);
				} else {
					value = CommonUtils.nullToEmpty(rs.getString(i));
				}
				int j = i - 1;
				if (multiple) {
					values.put(key, keyField == null ? j : columnName, value,
							value);
				} else {
					values.put(key, keyField == null ? j : columnName, value);
				}
			}
			row++;
		}
		return values;
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

	public static Map<Long, Long> toIdMap(Statement stmt, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = stmt.executeQuery(sql);
		Map<Long, Long> result = toIdMap(rs, fn1, fn2);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static Map<Long, String> toStringMap(Statement stmt, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = stmt.executeQuery(sql);
		Map<Long, String> result = new TreeMap<Long, String>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getString(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static StringMap toStringStringMap(Statement stmt, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = stmt.executeQuery(sql);
		StringMap result = new StringMap();
		while (rs.next()) {
			result.put(rs.getString(fn1), rs.getString(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static Map<Long, Timestamp> toTimestampMap(Statement stmt,
			String sql, String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = stmt.executeQuery(sql);
		Map<Long, Timestamp> result = new TreeMap<Long, Timestamp>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getTimestamp(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	private static void maybeLogQuery(String sql) {
		System.out.println(
				"Query: " + CommonUtils.trimToWsChars(sql, 2000, true));
	}

	private static Set<Long> toIdList(ResultSet rs, String fieldName)
			throws SQLException {
		int log = CommonUtils
				.iv(LooseContext.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Set<Long> result = new TreeSet<Long>();
		while (rs.next()) {
			result.add(rs.getLong(fieldName));
			if (log > 0 && result.size() % log == 0) {
				System.out.println(result.size() + "...");
			}
		}
		return result;
	}

	private static Map<Long, Long> toIdMap(ResultSet rs, String fn1, String fn2)
			throws SQLException {
		int log = CommonUtils
				.iv(LooseContext.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Map<Long, Long> result = new TreeMap<Long, Long>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getLong(fn2));
			if (log > 0 && result.size() % log == 0) {
				System.out.println(result.size() + "...");
			}
		}
		return result;
	}

	static List<String> getColumnNames(ResultSet rs) throws SQLException {
		List<String> columnNames = new ArrayList<String>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			columnNames.add(rs.getMetaData().getColumnName(i));
		}
		return columnNames;
	}

	public static interface ColumnFormatter {
		public String format(ResultSet rs, int columnIndex) throws SQLException;
	}
}

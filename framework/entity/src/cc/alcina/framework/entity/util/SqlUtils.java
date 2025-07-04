package cc.alcina.framework.entity.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.CommonUtils;
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

	public static int execute(Statement statement, String sql) {
		try {
			MetricLogging.get().start("query");
			maybeLogQuery(sql);
			return statement.executeUpdate(sql);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			MetricLogging.get().end("query");
		}
	}

	public static ResultSet executeQuery(Statement statement, String sql) {
		try {
			MetricLogging.get().start("query");
			maybeLogQuery(sql);
			return statement.executeQuery(sql);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			MetricLogging.get().end("query");
		}
	}

	static List<String> getColumnNames(ResultSet rs) throws SQLException {
		List<String> columnNames = new ArrayList<String>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			columnNames.add(rs.getMetaData().getColumnName(i));
		}
		return columnNames;
	}

	public static <T> List<T> getMapped(Statement statement, String sql,
			ThrowingFunction<ResultSet, T> mapper) {
		try {
			MetricLogging.get().start("query");
			maybeLogQuery(sql);
			ResultSet rs = (statement instanceof PreparedStatement)
					? ((PreparedStatement) statement).executeQuery()
					: statement.executeQuery(sql);
			MetricLogging.get().end("query");
			List<T> result = new ArrayList<>();
			while (rs.next()) {
				result.add(mapper.apply(rs));
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T getValue(Statement statement, String sql, Class clazz) {
		try {
			ResultSet rs = statement.executeQuery(sql);
			if (rs.next()) {
				if (clazz == String.class) {
					return (T) rs.getString(1);
				} else if (clazz == Long.class || clazz == long.class) {
					return (T) Long.valueOf(rs.getLong(1));
				} else if (clazz == Integer.class || clazz == int.class) {
					return (T) Integer.valueOf(rs.getInt(1));
				} else if (clazz == Timestamp.class) {
					return (T) rs.getTimestamp(1);
				} else {
					throw new UnsupportedOperationException();
				}
			}
			if (clazz == long.class) {
				return (T) Long.valueOf(0L);
			} else if (clazz == int.class) {
				return (T) Integer.valueOf(0);
			} else if (clazz == String.class) {
				return null;
			} else if (clazz == Long.class) {
				return null;
			} else if (clazz == Integer.class) {
				return null;
			} else if (clazz == Timestamp.class) {
				return null;
			} else {
				throw new UnsupportedOperationException();
			}
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

	public static Map<Long, Long> toIdMap(Statement statement, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = statement.executeQuery(sql);
		Map<Long, Long> result = toIdMap(rs, fn1, fn2);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static Set<Long> toIdSet(Statement statement, String sql,
			String fieldName) throws SQLException {
		return toIdSet(statement, sql, fieldName, true);
	}

	public static Set<Long> toIdSet(Statement statement, String sql,
			String fieldName, boolean dumpQuerySql) throws SQLException {
		MetricLogging.get().start("query");
		if (dumpQuerySql) {
			System.out.println(sql);
		}
		ResultSet rs = statement.executeQuery(sql);
		Set<Long> result = toIdList(rs, fieldName);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static Map<Long, String> toStringMap(Statement statement, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = statement.executeQuery(sql);
		Map<Long, String> result = new TreeMap<Long, String>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getString(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static StringMap toStringStringMap(Statement statement, String sql,
			String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = statement.executeQuery(sql);
		StringMap result = new StringMap();
		while (rs.next()) {
			result.put(rs.getString(fn1), rs.getString(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static Map<Long, Timestamp> toTimestampMap(Statement statement,
			String sql, String fn1, String fn2) throws SQLException {
		MetricLogging.get().start("query");
		maybeLogQuery(sql);
		ResultSet rs = statement.executeQuery(sql);
		Map<Long, Timestamp> result = new TreeMap<Long, Timestamp>();
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getTimestamp(fn2));
		}
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	public static interface ColumnFormatter {
		public String format(ResultSet rs, int columnIndex) throws SQLException;
	}
}

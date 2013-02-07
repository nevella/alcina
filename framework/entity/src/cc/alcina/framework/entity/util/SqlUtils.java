package cc.alcina.framework.entity.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;

public class SqlUtils {
	public static final String CONTEXT_LOG_EVERY_X_RECORDS=SqlUtils.class.getName()+".CONTEXT_LOG_EVERY_X_RECORDS";
	public static Map<Long, Long> toIdMap(Statement stmt, String sql, String fn1,
			String fn2) throws SQLException {
		MetricLogging.get().start("query");
		System.out.println("Query: " + sql);
		ResultSet rs = stmt.executeQuery(sql);
		Map<Long, Long> result = toIdMap(rs, fn1, fn2);
		rs.close();
		MetricLogging.get().end("query");
		return result;
	}

	private static Map<Long, Long> toIdMap(ResultSet rs, String fn1, String fn2) throws SQLException {
		int log=CommonUtils.iv(LooseContext.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Map<Long, Long> result = new TreeMap<Long, Long>();
		
		while (rs.next()) {
			result.put(rs.getLong(fn1), rs.getLong(fn2));
			if(log>0&&result.size()%log==0){
				System.out.println(result.size()+"...");
			}
		}
		return result;
	}

	public static Set<Long> toIdList(Statement stmt, String sql, String fieldName)
			throws SQLException {
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
		int log=CommonUtils.iv(LooseContext.getInteger(CONTEXT_LOG_EVERY_X_RECORDS));
		Set<Long> result = new TreeSet<Long>();
		while (rs.next()) {
			result.add(rs.getLong(fieldName));
			if(log>0&&result.size()%log==0){
				System.out.println(result.size()+"...");
			}
		}
		return result;
	}
}

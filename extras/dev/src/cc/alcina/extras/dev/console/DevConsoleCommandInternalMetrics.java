package cc.alcina.extras.dev.console;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.extras.dev.console.DevConsoleCommandTransforms.CmdListTransforms.CmdListTransformsFilter;
import cc.alcina.extras.dev.console.DevConsoleCommandTransforms.DateTimeFormatter;
import cc.alcina.extras.dev.console.DevConsoleCommandTransforms.TrimmedStringFormatter;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.console.FilterArgvParam;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetric;
import cc.alcina.framework.entity.projection.EntityUtils;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.entity.util.SqlUtils.ColumnFormatter;
import cc.alcina.framework.entity.util.SynchronizedSimpleDateFormat;

public class DevConsoleCommandInternalMetrics {
	public static class CmdListMetrics extends DevConsoleCommand {
		boolean foundDteId;

		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "ims" };
		}

		@Override
		public String getDescription() {
			return "list internal metrics with filters";
		}

		@Override
		public String getUsage() {
			return "im {params or none for usage}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				printFullUsage();
				return "";
			}
			console.clear();
			FilterArgvParam p = new FilterArgvParam(argv, "limit");
			argv = p.argv;
			int limit = Integer.parseInt(p.valueOrDefault("9999"));
			p = new FilterArgvParam(argv, "ids");
			List<Long> ids = TransformManager
					.idListToLongs(p.valueOrDefault(""));
			p = new FilterArgvParam(argv, "host");
			String host = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "call");
			String call = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "minId");
			long minId = Long.parseLong(p.valueOrDefault("0"));
			p = new FilterArgvParam(argv, "maxId");
			long maxId = Long.parseLong(
					p.valueOrDefault(String.valueOf(Integer.MAX_VALUE)));
			p = new FilterArgvParam(argv, "nearDate");
			String nearDate = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "format");
			Format format = Format.valueOf(p.valueOrDefault("list"));
			Connection conn = getConn();
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistenceExTransaction();
			String tableName = "internalmetric";
			String sql = "select * from internalmetric where id != -1 AND %s order by id desc";
			List<String> filters = new ArrayList<>();
			if (ids.size() > 0) {
				filters.add(Ax.format("id in %s",
						EntityUtils.longsToIdClause(ids)));
			}
			if (host.length() > 0) {
				filters.add(Ax.format("hostname ilike '%%s%'", host));
			}
			if (call.length() > 0) {
				filters.add(Ax.format("callname ilike '%%s%'", call));
			}
			if (minId != 0) {
				filters.add(Ax.format("id >= %s", minId));
			}
			if (maxId != 0) {
				filters.add(Ax.format("id <= %s", maxId));
			}
			if (nearDate.length() > 0) {
				DateFormat dbDateFormat = new SynchronizedSimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				Date near = dbDateFormat.parse(nearDate);
				Date start = new Date(
						near.getTime() - 2 * TimeConstants.ONE_MINUTE_MS);
				filters.add(Ax.format("starttime >= '%s'",
						dbDateFormat.format(start)));
				filters.add(Ax.format("endtime <= '%s'",
						dbDateFormat.format(near)));
			}
			sql = Ax.format(sql,
					filters.stream().collect(Collectors.joining(" AND ")));
			PreparedStatement ps = conn.prepareStatement(sql);
			System.out.println(console.breakAndPad(1, 80, sql, 0));
			switch (format) {
			case list: {
				ResultSet rs = ps.executeQuery();
				Map<String, ColumnFormatter> formatters = new HashMap<String, SqlUtils.ColumnFormatter>();
				formatters.put("startdate", new DateTimeFormatter());
				formatters.put("enddate", new DateTimeFormatter());
				formatters.put("slicejson", new TrimmedStringFormatter(50));
				formatters.put("obfuscatedargs",
						new TrimmedStringFormatter(50));
				SqlUtils.dumpResultSet(rs, formatters);
				rs.close();
			}
				break;
			case dump: {
				List<InternalMetric> metrics = SqlUtils.getMapped(ps, sql,
						rs -> {
							InternalMetric metric = CommonPersistenceProvider
									.get().getCommonPersistenceExTransaction()
									.getNewImplementationInstance(
											InternalMetric.class);
							metric.setCallName(rs.getString("callname"));
							metric.setHostName(rs.getString("hostname"));
							metric.setEndTime(rs.getTimestamp("endTime"));
							metric.setStartTime(rs.getTimestamp("startTime"));
							metric.setId(rs.getLong("id"));
							metric.setLockType(rs.getString("locktype"));
							metric.setObfuscatedArgs(
									rs.getString("obfuscatedargs"));
							metric.setSliceJson(rs.getString("slicejson"));
							metric.setSliceCount(rs.getInt("slicecount"));
							metric.setThreadName(rs.getString("threadname"));
							return metric;
						});
				for (InternalMetric internalMetric : metrics) {
					Ax.out(GraphProjection.fieldwiseToString(internalMetric,
							false, false, 30, "obfuscatedArgs", "sliceJson",
							"versionNumber", "localId"));
					Ax.out("-----------\n%s\n\n---------\n%s\n",
							internalMetric.getObfuscatedArgs(),
							internalMetric.getSliceJson());
				}
			}
				break;
			default:
				throw new UnsupportedOperationException();
			}
			ps.close();
			return "";
		}

		private void printFullUsage() {
			System.out.format("im {usage} \n", DevConsoleFilter
					.describeFilters(CmdListTransformsFilter.class));
		}

		enum Format {
			list, dump
		}
	}
}

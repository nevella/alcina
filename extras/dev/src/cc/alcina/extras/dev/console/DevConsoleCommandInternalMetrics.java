package cc.alcina.extras.dev.console;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;

import cc.alcina.extras.dev.console.DevConsoleCommandTransforms.DateTimeFormatter;
import cc.alcina.extras.dev.console.DevConsoleCommandTransforms.TrimmedStringFormatter;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.console.FilterArgvParam;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.cache.DomainStoreWaitStats.DomainStoreWaitOnLockStat;
import cc.alcina.framework.entity.persistence.metric.InternalMetric;
import cc.alcina.framework.entity.persistence.metric.ThreadHistory;
import cc.alcina.framework.entity.persistence.metric.ThreadInfoSer;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.entity.util.SqlUtils.ColumnFormatter;
import cc.alcina.framework.entity.util.SynchronizedSimpleDateFormat;

public class DevConsoleCommandInternalMetrics {
	public static class CmdDrillMetric extends DevConsoleCommand {
		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "imd" };
		}

		@Override
		public String getDescription() {
			return "drill internal metric";
		}

		@Override
		public String getUsage() {
			return "imd <id> (true) :: second arg indicates dump callee args";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				Ax.out("id required");
				return "";
			}
			List<String> args = new ArrayList<>();
			args.add("ids");
			args.add(argv[0]);
			args.add("format");
			args.add("dump");
			if (argv.length == 2) {
				args.add("args");
				args.add("true");
			} else {
				args.add("ignoreables");
				args.add("true");
			}
			String[] suvargv = (String[]) args.toArray(new String[args.size()]);
			return runSubcommand(new CmdListMetrics(), suvargv);
		}
	}

	public static class CmdListMetrics extends DevConsoleCommand {
		private boolean ignoreables;

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
			return "ims filters:{host|args|call|threadName|age|nearDate} modifiers:{tz|format|ignoreables|}";
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
			int limit = Integer.parseInt(p.valueOrDefault("5"));
			p = new FilterArgvParam(argv, "ids");
			List<Long> ids = TransformManager
					.idListToLongs(p.valueOrDefault(""));
			p = new FilterArgvParam(argv, "host");
			String host = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "args");
			boolean outputArgs = Boolean
					.parseBoolean(p.valueOrDefault("false"));
			p = new FilterArgvParam(argv, "ignoreables");
			ignoreables = Boolean.parseBoolean(p.valueOrDefault("false"));
			p = new FilterArgvParam(argv, "call");
			String call = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "minId");
			long minId = Long.parseLong(p.valueOrDefault("0"));
			p = new FilterArgvParam(argv, "maxId");
			long maxId = Long.parseLong(
					p.valueOrDefault(String.valueOf(Integer.MAX_VALUE)));
			p = new FilterArgvParam(argv, "nearDate");
			String nearDate = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "threadName");
			String threadName = p.valueOrDefault("");
			p = new FilterArgvParam(argv, "age");
			long age = Long.parseLong(p.valueOrDefault(p.valueOrDefault("0")));
			p = new FilterArgvParam(argv, "duration");
			long duration = Long
					.parseLong(p.valueOrDefault(p.valueOrDefault("0")));
			p = new FilterArgvParam(argv, "tz");
			String tz = p.valueOrDefault("AEST");
			p = new FilterArgvParam(argv, "format");
			Format format = Format.valueOf(p.valueOrDefault("list"));
			p = new FilterArgvParam(argv, "order");
			String order = p.valueOrDefault("updatetime");
			Connection conn = getConn();
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistenceExTransaction();
			String tableName = "internalmetric";
			String sql = "select %s from internalmetric where id != -1 and updatetime is not null "
					+ "AND %s order by %s desc limit %s";
			List<String> filters = new ArrayList<>();
			if (ids.size() > 0) {
				filters.add(Ax.format("id in %s",
						EntityPersistenceHelper.toInClause(ids)));
			}
			if (host.length() > 0) {
				filters.add(Ax.format("hostname ilike '%%s%'", host));
			}
			if (call.length() > 0) {
				filters.add(Ax.format("callname ilike '%%s%'", call));
			}
			if (threadName.length() > 0) {
				filters.add(Ax.format("threadName ilike '%%s%'", threadName));
			}
			if (minId != 0) {
				filters.add(Ax.format("id >= %s", minId));
			}
			if (maxId != 0) {
				filters.add(Ax.format("id <= %s", maxId));
			}
			if (age != 0) {
				filters.add(Ax.format(
						"extract 	(epoch from now() at time zone '%s' -starttime)<%s",
						tz, age));
			}
			if (duration != 0) {
				filters.add(Ax.format(
						"extract    (epoch from (CASE when endtime is null then now() ELSE endtime END)-starttime)*1000>%s",
						duration));
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
			String fields = "*";
			switch (format) {
			case list: {
				fields = "id,starttime,endtime," + "extract ('epoch' from "
						+ "(CASE when endtime is null then now() ELSE endtime END)-starttime)*1000 as duration,"
						+ "hostname,callname,threadname,updatetime,optlock,slicecount,locktype";
			}
				break;
			}
			sql = Ax.format(sql, fields,
					filters.stream().collect(Collectors.joining(" AND ")),
					order, limit);
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
							InternalMetric metric = AlcinaPersistentEntityImpl
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
					dumpMetric(internalMetric, outputArgs);
				}
			}
				break;
			default:
				throw new UnsupportedOperationException();
			}
			ps.close();
			return "";
		}

		private long adjustForTz(long time, Date relTo) {
			if (time == 0) {
				return 0;
			}
			long diff = time - relTo.getTime();
			if (diff < 0) {
				diff = -(diff % TimeConstants.ONE_HOUR_MS);
				if (diff > TimeConstants.ONE_HOUR_MS - diff) {
					diff = TimeConstants.ONE_HOUR_MS - diff;
				}
			}
			return diff;
		}

		private void dumpMetric(InternalMetric internalMetric,
				boolean outputArgs) {
			Ax.out(GraphProjection.fieldwiseToString(internalMetric, false,
					false, 30, "obfuscatedArgs", "sliceJson", "versionNumber",
					"localId"));
			String args = internalMetric.getObfuscatedArgs();
			try {
				Matcher matcher = Pattern
						.compile("(?s)((?:.+?)Parameters:)(.+)").matcher(args);
				matcher.matches();
				args = matcher.group(1)
						+ new JSONArray(matcher.group(2)).toString(2);
			} catch (Exception e) {
				// iignore
			}
			if (outputArgs) {
				Ax.out("-----------\n%s\n", args);
			}
			ThreadHistory history = internalMetric.getThreadHistory();
			CachingMap<Long, WaitStatLockTimeTuple> waitStatTimes = new CachingMap<>(
					WaitStatLockTimeTuple::new);
			history.elements.forEach(thhe -> {
				ThreadInfoSer threadInfo = thhe.threadInfo;
				if (ignoreables) {
					String joined = CommonUtils
							.joinWithNewlines(threadInfo.stackTrace);
					if (threadInfo.lock != null) {
						joined = Ax.format("Waiting for lock: %s\n",
								threadInfo.lock, joined);
					}
					if (CmdAnalyseStackTrace.ignoreableStackTrace(joined)) {
						return;
					}
				}
				Ax.out("Elapsed: %s", adjustForTz(thhe.date.getTime(),
						internalMetric.getStartTime()));
				if (thhe.domainCacheLockTime != 0
						|| thhe.domainCacheWaitTime != 0) {
					Ax.out("Domain cache:\n\tActive time: %s\n\tWait time: %s\n\tLock state: %s",
							adjustForTz(thhe.domainCacheLockTime, thhe.date),
							adjustForTz(thhe.domainCacheWaitTime, thhe.date),
							thhe.lockState);
				}
				if (threadInfo.lockedMonitors.size() > 0) {
					Ax.out("Locked monitors:\n\t%s", CommonUtils
							.joinWithNewlineTab(threadInfo.lockedMonitors));
				}
				if (threadInfo.lockedSynchronizers.size() > 0) {
					Ax.out("Locked synchronizers:\n\t%s",
							CommonUtils.joinWithNewlineTab(
									threadInfo.lockedSynchronizers));
				}
				thhe.waitStats.waitingOnLockStats
						.forEach(stat -> waitStatTimes.get(stat.bestId())
								.add(stat, thhe.domainCacheWaitTime));
				if (threadInfo.lock != null) {
					Ax.out("Waiting for lock: %s", threadInfo.lock);
				}
				Ax.out("Trace:\n\t%s", CommonUtils.joinWithNewlineTab(
						filterTrace(threadInfo.stackTrace)));
			});
			Ax.out("-----------\nContended locks:\n%s\n%s",
					WaitStatLockTimeTuple.toHeader(),
					waitStatTimes.values().stream()
							.sorted(Comparator.comparing(
									WaitStatLockTimeTuple::sliceCount))
							.map(Object::toString)
							.collect(Collectors.joining("\n")));
			if (outputArgs) {
				Ax.out("-----------\n%s\n", args);
			}
		}

		private boolean elide(FilteredTrace last, StackTraceElement element,
				ElideState elideState) {
			if (last == null) {
				return false;
			}
			StackTraceElement lastElement = last.element;
			if (lastElement.getClassName()
					.equals("com.google.gwt.user.server.rpc.RPC")) {
				return true;
			}
			if (lastElement.getClassName()
					.equals("au.com.barnet.jade.server.JadeRemoteServiceImpl")
					&& element.getClassName().matches(
							"sun.reflect.NativeMethodAccessorImpl|sun.reflect.DelegatingMethodAccessorImpl|java.lang.reflect.Method")) {
				return true;
			}
			if (lastElement.getClassName().matches(
					".*(CollectionProjection|GraphProjection|CacheProjection).*")
					&& element.getClassName().matches(
							".*(CollectionProjection|GraphProjection|CacheProjection).*")) {
				return true;
			}
			if (lastElement.getClassName().equals(
					"org.jboss.as.ee.component.ManagedReferenceMethodInterceptor")) {
				elideState.inJbossEjbCall = true;
			}
			if (element.getClassName().equals(
					"org.jboss.as.ee.component.ProxyInvocationHandler")) {
				elideState.inJbossEjbCall = false;
			}
			return elideState.inJbossEjbCall;
		}

		private List<FilteredTrace>
				filterTrace(List<StackTraceElement> stackTrace) {
			List<FilteredTrace> result = new ArrayList<>();
			int index = 0;
			boolean elided = false;
			ElideState elideState = new ElideState();
			for (StackTraceElement element : stackTrace) {
				if (elide(CommonUtils.last(result), element, elideState)) {
					elided = true;
				} else {
					FilteredTrace trace = new FilteredTrace();
					result.add(trace);
					trace.element = element;
					trace.frameIndex = index;
					trace.elided = elided;
					elided = false;
				}
				index++;
			}
			return result;
		}

		private void printFullUsage() {
			Ax.out(getUsage());
			// System.out.format("im {} \n", DevConsoleFilter
			// .describeFilters(CmdListTransformsFilter.class));
		}

		class ElideState {
			boolean inJbossEjbCall = false;
		}

		static class FilteredTrace {
			public boolean elided;

			public int frameIndex;

			public StackTraceElement element;

			@Override
			public String toString() {
				return String.format("%s%5s: %s", elided ? "..." : "   ",
						frameIndex, element);
			}
		}

		enum Format {
			list, dump
		}

		static class WaitStatLockTimeTuple {
			private static final String template = "%-30s %12s %4s %8s";

			static String toHeader() {
				return String.format(template, "Thread name", "id", "size",
						"min.time");
			}

			List<DomainStoreWaitOnLockStat> stats = new ArrayList<>();

			long id;

			long cumulativeTime;

			long lastDomainCacheWaitTime;

			WaitStatLockTimeTuple(long id) {
				this.id = id;
			}

			public void add(DomainStoreWaitOnLockStat stat,
					long domainCacheWaitTime) {
				stats.add(stat);
				if (domainCacheWaitTime > lastDomainCacheWaitTime) {
					cumulativeTime -= lastDomainCacheWaitTime;
				}
				cumulativeTime += domainCacheWaitTime;
				lastDomainCacheWaitTime = domainCacheWaitTime;
			}

			@Override
			public String toString() {
				DomainStoreWaitOnLockStat stat = stats.get(0);
				return String.format(template, stat.threadName, id,
						stats.size(), cumulativeTime);
			}

			int sliceCount() {
				return stats.size();
			}
		}
	}
}

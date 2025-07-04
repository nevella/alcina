package cc.alcina.extras.dev.console;

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.Table;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandlerShort;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.console.FilterArgvFlag;
import cc.alcina.framework.entity.console.FilterArgvParam;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.entity.util.SqlUtils.ColumnFormatter;

public class DevConsoleCommandTransforms {
	static boolean classRefsEnsured;

	public static void ensureClassRefs(Connection conn) throws Exception {
		if (classRefsEnsured) {
			return;
		}
		System.out.println("getting classrefs...");
		Statement ps = conn.createStatement();
		ResultSet rs = ps.executeQuery("select * from classref");
		while (rs.next()) {
			long id = rs.getLong("id");
			String cn = rs.getString("refclassname");
			Class clazz = PersistentImpl.getImplementation(ClassRef.class);
			ClassRef cr = (ClassRef) clazz.getDeclaredConstructor()
					.newInstance();
			cr.setId(id);
			cr.setRefClassName(cn);
			try {
				cr.setRefClass(Class.forName(cn));
			} catch (Exception e) {
				e.printStackTrace();
			}
			ClassRef.add(Collections.singleton(cr));
		}
		ps.close();
		System.out.println("getting classrefs...got");
		classRefsEnsured = true;
	}

	static class ClassRefNameFormatter implements ColumnFormatter {
		@Override
		public String format(ResultSet rs, int columnIndex)
				throws SQLException {
			long objRefId = rs.getLong(columnIndex);
			ClassRef forId = ClassRef.forId(objRefId);
			if (forId == null) {
				return "unknown";
			}
			return forId.getRefClass() == null ? "<deleted class>"
					: forId.getRefClass().getSimpleName();
		}
	}

	public static class CmdListClientInstances extends DevConsoleCommand {
		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "trcl" };
		}

		@Override
		public String getDescription() {
			return "list client instances with filters";
		}

		@Override
		public String getUsage() {
			return "trcl {id} {user_id|user_name} {days}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String sql = "select ci.*, u.username  "
					+ "from client_instance ci "
					+ "inner join authenticationsession aus on ci.authenticationsession_id=aus.id "
					+ "inner join users u on aus.user_id=u.id "
					+ "where ci.id != -1 %s order by ci.id desc";
			String arg0 = argv[0];
			String arg1 = argv.length < 2 ? "0" : argv[1];
			String filter = "";
			filter += arg0.equals("0") ? ""
					: String.format(" and ci.id=%s ", arg0);
			if (!arg1.equals("0")) {
				filter += arg1.matches("\\d+")
						? String.format(" and u.id=%s ", arg1)
						: String.format(" and u.username='%s' ", arg1);
			}
			String arg2 = argv.length < 3 ? filter.isEmpty() ? "7" : "0"
					: argv[2];
			filter += arg2.equals("0") ? ""
					: String.format("  and age(ci.hellodate)<'%s days'  ",
							arg2);
			Connection conn = getConn();
			sql = String.format(sql, filter);
			System.out.println(console.breakAndPad(1, 80, sql, 0));
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			SqlUtils.dumpResultSet(rs);
			return "";
		}
	}

	public static class CmdListTransforms extends DevConsoleCommand {
		boolean foundDteId;

		public static class Result {
			public List<DomainTransformEvent> transforms;

			public long firstDtrId;

			public Date firstEventDate;
		}

		public Result result = new Result();

		public boolean muteLogging;

		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "trt" };
		}

		@Override
		public String getDescription() {
			return "list transforms with filters";
		}

		@Override
		public String getUsage() {
			return "trt {params or none for usage}";
		}

		private void printFullUsage() {
			System.out.format(
					"trt <-r:=rq ids only> <-wdtr: with dtr prefilter> <-t: as transforms> {[%s] value}+\n",
					DevConsoleFilter
							.describeFilters(CmdListTransformsFilter.class));
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				printFullUsage();
				return "";
			}
			FilterArgvParam p = new FilterArgvParam(argv, "limit");
			argv = p.argv;
			int limit = Integer.parseInt(p.valueOrDefault("9999"));
			FilterArgvFlag f = new FilterArgvFlag(argv, "-r");
			boolean rqIdsOnly = f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-t");
			boolean outputTransforms = f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-v");
			boolean valuesOnly = f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-objIds");
			boolean objIds = f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-rr");
			boolean forceGetRqIds = f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-wdtr");
			// seems that dtr query generally slows things
			boolean noDtrQuery = !f.contains;
			argv = f.argv;
			f = new FilterArgvFlag(argv, "-oldCi");
			// seems that dtr query generally slows things
			boolean oldCi = f.contains;
			argv = f.argv;
			Connection conn = getConn();
			ensureClassRefs(conn);
			Class<? extends DomainTransformRequestPersistent> clazz = PersistentImpl
					.getImplementation(DomainTransformRequestPersistent.class);
			String dtrName = clazz.getAnnotation(Table.class).name();
			Class<? extends DomainTransformEventPersistent> class1 = PersistentImpl
					.getImplementation(DomainTransformEventPersistent.class);
			String dteName = class1.getAnnotation(Table.class).name();
			String authClause = "client_instance ci "
					+ "inner join authenticationsession aus on ci.authenticationsession_id=aus.id "
					+ "inner join users u on aus.user_id=u.id ";
			String authClauseOld = "client_instance ci "
					+ "inner join users u on ci.user_id=u.id ";
			if (oldCi) {
				authClause = authClauseOld;
			}
			String sql1 = "select dtr.id as id from " + authClause
					+ " inner join %s dtr on dtr.clientinstance_id=ci.id "
					+ "where %s order by dtr.id desc";
			String sql2 = "select ci.id as cli_id, u.username,  "
					+ "  dtr.id as dtr_id,dtr.requestid as dtr_rid, dte.id as dte_id, "
					+ " dte.objectclassref_id as dte_objref, dte.objectid as object_id, "
					+ "dte.propertyname as propertyname, "
					+ " dte.newstringvalue as newstringvalue,dte.transformtype as transformtype, "
					+ " dte.valueid,"
					+ " dte.servercommitdate as servercommitdate,"
					+ " dte.valueclassref_id, "
					+ "dte.utcDate as utcdate, dte.objectlocalid,dtr.tag as tag "
					+ "from " + authClause
					+ " inner join %s dtr on dtr.clientinstance_id=ci.id "
					+ " inner join %s dte on dte.domaintransformrequestpersistent_id = dtr.id"
					+ " where %s %s limit %s";
			Set<Long> ids = null;
			String orderClause = limit < 100 ? ""
					: outputTransforms ? "order by dte.id"
							: "order by dte.id desc";
			Predicate<String> dteIdFilter = new Predicate<String>() {
				@Override
				public boolean test(String o) {
					if (o.contains("dte.id")) {
						foundDteId = true;
					}
					return o.contains("dte.id");
				}
			};
			Predicate<String> dteFilter = new Predicate<String>() {
				@Override
				public boolean test(String o) {
					return o.contains("dte.");
				}
			};
			String filter = DevConsoleFilter.getFilters(
					CmdListTransformsFilter.class, argv, dteIdFilter);
			if (!noDtrQuery && (!foundDteId || forceGetRqIds)) {
				filter = DevConsoleFilter.getFilters(
						CmdListTransformsFilter.class, argv,
						dteFilter.negate());
				sql1 = String.format(sql1, dtrName, filter);
				Statement ps = conn.createStatement();
				if (!muteLogging) {
					System.out.println(console.breakAndPad(1, 80, sql1, 0));
				}
				ids = SqlUtils.toIdSet(ps, sql1, "id", false);
				ps.close();
				List<String> args = new ArrayList<String>(Arrays.asList(argv));
				args.add("dtr");
				args.add(CommonUtils.join(ids, ", "));
				argv = args.toArray(new String[args.size()]);
			}
			if (rqIdsOnly) {
				System.out.format("Matched request ids: \n%s\n\n",
						CommonUtils.join(ids, ", "));
			} else {
				filter = DevConsoleFilter
						.getFilters(CmdListTransformsFilter.class, argv);
				sql2 = String.format(sql2, dtrName, dteName, filter,
						orderClause, limit);
				PreparedStatement ps = conn.prepareStatement(sql2);
				if (!muteLogging) {
					System.out.println(console.breakAndPad(1, 80, sql2, 0));
				}
				ResultSet rs = ps.executeQuery();
				if (outputTransforms) {
					RsrowToDteConverter transformer = new RsrowToDteConverter(
							true);
					result.transforms = transformer.convert(rs);
					result.firstDtrId = transformer.firstDtrId;
					result.firstEventDate = transformer.firstEventDate;
					if (!muteLogging) {
						String outPath = "/tmp/transforms.txt";
						Io.write().string(result.transforms.toString())
								.toPath(outPath);
						Ax.out("wrote %s transforms to \n\t%s",
								result.transforms.size(), outPath);
					}
				} else if (valuesOnly) {
					while (rs.next()) {
						System.out.format("%s | %s\n", rs.getLong("object_id"),
								rs.getString("newstringvalue"));
					}
				} else if (objIds) {
					while (rs.next()) {
						System.out.format("%s\n", rs.getLong("object_id"));
					}
				} else {
					Map<String, ColumnFormatter> formatters = new HashMap<String, SqlUtils.ColumnFormatter>();
					formatters.put("dte_objref", new ClassRefNameFormatter());
					formatters.put("servercommitdate", new DateTimeFormatter());
					formatters.put("utcdate", new DateTimeFormatter());
					formatters.put("transformtype",
							new EnumFormatter(TransformType.class));
					formatters.put("newstringvalue",
							new TrimmedStringFormatter(30));
					console.startRecordingSysout(false);
					SqlUtils.dumpResultSet(rs, formatters);
					String sysout = console.endRecordingSysout();
					String outPath = "/tmp/transforms-table.txt";
					Io.write().string(sysout).toPath(outPath);
				}
				rs.close();
				ps.close();
			}
			return "";
		}

		@Registration(CmdListTransformsFilter.class)
		public abstract static class CmdListTransformsFilter
				extends DevConsoleFilter {
		}

		public static class CmdListTransformsFilterClass
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(final String arg1) {
				Set<ClassRef> refs = ClassRef.all();
				Predicate<ClassRef> classNameFilter = new Predicate<ClassRef>() {
					Pattern namePattern = Pattern.compile(arg1,
							Pattern.CASE_INSENSITIVE);

					@Override
					public boolean test(ClassRef o) {
						return namePattern.matcher(o.getRefClassName()).find();
					}
				};
				List<ClassRef> filteredRefs = refs.stream()
						.filter(classNameFilter).collect(Collectors.toList());
				return String.format("dte.objectclassref_id in %s",
						EntityPersistenceHelper.toInClause(filteredRefs));
			}

			@Override
			public String getKey() {
				return "class";
			}
		}

		public static class CmdListTransformsFilterClientInstance
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = CommonUtils.isNullOrEmpty(value) ? "-1" : value;
				if (value.contains("/")) {
					return Ax.format("ci.id=%s AND dtr.requestId=%s",
							value.split("/")[0], value.split("/")[1]);
				}
				return String.format(
						value.contains(",") ? "ci.id in (%s)" : "ci.id=%s",
						value);
			}

			@Override
			public String getKey() {
				return "ci";
			}

			@Override
			protected boolean hasDefault() {
				return false;
			}
		}

		public static class CmdListTransformsFilterCreationIds
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(final String arg1) {
				return "dte.transformType=0";
			}

			@Override
			public String getKey() {
				return "creationIds";
			}
		}

		public static class CmdListTransformsFilterDays
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("age(ci.hellodate)<'%s days'",
						value == null ? "30" : value);
			}

			@Override
			public String getKey() {
				return "days";
			}

			@Override
			protected boolean hasDefault() {
				return true;
			}
		}

		public static class CmdListTransformsFilterDteId
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = value.isEmpty() ? "-1" : value;
				if (value.contains(">")) {
					return String.format("dte.id %s", value);
				}
				return String.format(
						value.contains(",") ? "dte.id in (%s)" : "dte.id=%s",
						value);
			}

			@Override
			public String getKey() {
				return "dte";
			}
		}

		public static class CmdListTransformsFilterDtrId
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = value.isEmpty() ? "-1" : value;
				return String.format(
						value.contains(",") ? "dtr.id in (%s)" : "dtr.id=%s",
						value);
			}

			@Override
			public String getKey() {
				return "dtr";
			}
		}

		public static class CmdListTransformsFilterDtrId2
				extends CmdListTransformsFilterDtrId {
			@Override
			public String getKey() {
				return "dtrid";
			}
		}

		public static class CmdListTransformsFilterDtrRqId
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = value.isEmpty() ? "-1" : value;
				return String
						.format(value.contains(",") ? "dtr.requestid in (%s)"
								: "dtr.requestid=%s", value);
			}

			@Override
			public String getKey() {
				return "dtr.rqid";
			}
		}

		public static class CmdListTransformsFilterMinDays
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("age(ci.hellodate)>'%s days'",
						value == null ? "3" : value);
			}

			@Override
			public String getKey() {
				return "mindays";
			}

			@Override
			protected boolean hasDefault() {
				return false;
			}
		}

		public static class CmdListTransformsFilterNewStringValue
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String arg1) {
				return String.format("dte.newStringValue ilike '%%%s%%'", arg1);
			}

			@Override
			public String getKey() {
				return "nsv";
			}
		}

		public static class CmdListTransformsFilterTransformType
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(final String arg1) {
				return String.format("dte.transformtype = %s",
						TransformType.valueOf(arg1).ordinal());
			}

			@Override
			public String getKey() {
				return "tt";
			}
		}

		public static class CmdListTransformsFilterUser
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String arg1) {
				return arg1.matches("\\d+") ? String.format("u.id=%s", arg1)
						: String.format("u.username='%s'", arg1);
			}

			@Override
			public String getKey() {
				return "user";
			}
		}

		public static class CmdListTransformsFilterUserNot
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String arg1) {
				return arg1.matches("\\d+") ? String.format("u.id!=%s", arg1)
						: String.format("u.username!='%s'", arg1);
			}

			@Override
			public String getKey() {
				return "usernot";
			}
		}

		public static class CmdListTransformsFilterValueId
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format(value.contains(",") ? "dte.valueid in (%s)"
						: "dte.valueid=%s", value);
			}

			@Override
			public String getKey() {
				return "valueid";
			}
		}

		public static class CmdListTransformsObjectId
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				String template = "dte.objectid=%s";
				if (value.contains(",")) {
					template = "dte.objectid in (%s)";
				}
				if (value.matches("[<>]=.+")) {
					template = "dte.objectid %s";
				}
				return String.format(template, value);
			}

			@Override
			public String getKey() {
				return "objid";
			}
		}

		public static class CmdListTransformsPropertyName
				extends CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("dte.propertyname ='%s'", value);
			}

			@Override
			public String getKey() {
				return "pn";
			}
		}
	}

	static class DateTimeFormatter implements ColumnFormatter {
		@Override
		public String format(ResultSet rs, int columnIndex)
				throws SQLException {
			Timestamp ts = rs.getTimestamp(columnIndex);
			return DateStyle.DATE_TIME_HUMAN.format(ts);
		}
	}

	static class EnumFormatter implements ColumnFormatter {
		private Class<? extends Enum> clazz;

		public EnumFormatter(Class<? extends Enum> clazz) {
			this.clazz = clazz;
		}

		@Override
		public String format(ResultSet rs, int columnIndex)
				throws SQLException {
			int i = rs.getInt(columnIndex);
			return rs.wasNull() ? null : clazz.getEnumConstants()[i].toString();
		}
	}

	public static class LogsToDtrs {
		private static final Pattern NUMERIC_FN_PATTERN = Pattern
				.compile("(\\d+)\\.(?:txt\\.gz|txt)");

		public LogsToDtrs() {
		}

		public Multimap<Long, List<DomainTransformEvent>>
				dtrExpsToCliDteMap(List<File> files) throws Exception {
			Multimap<Long, List<DomainTransformEvent>> result = new Multimap<Long, List<DomainTransformEvent>>();
			List<DeltaApplicationRecord> wrappers = new ArrayList<DeltaApplicationRecord>();
			Collections.sort(files, new LongFnComparator());
			int processedIndex = 0;
			for (; processedIndex < files.size();) {
				File f = files.get(processedIndex++);
				String ser = Io.read().file(f).asString();
				DeltaApplicationRecord wrapper = new DeltaApplicationRecordSerializerImpl()
						.read(ser);
				// no uuid, this is dev code and will never be committed
				// automatically
				DomainTransformRequest rq = DomainTransformRequest
						.fromString(wrapper.getText(), null);
				result.addCollection(wrapper.getClientInstanceId(),
						rq.getEvents());
			}
			return result;
		}

		public Multimap<Long, List<DomainTransformEvent>>
				dtrExpsToCliDteMap(String folderPath) throws Exception {
			List<File> files = new ArrayList<File>(Arrays
					.asList(new File(folderPath).listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							return NUMERIC_FN_PATTERN
									.matcher(pathname.getName()).matches();
						}
					})));
			return dtrExpsToCliDteMap(files);
		}

		public Multimap<Long, List<DomainTransformEvent>>
				logsToDtrs(String logFile, boolean removeDuplicates) {
			Pattern lfPat = Pattern.compile(
					"\\s*(\\d{4}.+?)\\s+\\| transform\\s+\\| (\\d+)\\s+\\|(.+)");
			Multimap<Long, List<DomainTransformEvent>> result = new Multimap<Long, List<DomainTransformEvent>>();
			List<String> strs = CommonUtils.split(logFile, "\n");
			Collections.reverse(strs);
			for (int i = 0; i < strs.size(); i++) {
				String line = strs.get(i);
				if (line.contains("kXZ3")) {
					int j = 3;
				}
				// ignore terminating "|"
				int idx = line.length() - 1;
				if (idx < 1) {
					continue;
				}
				for (; idx > 0 && line.charAt(idx - 1) == ' '; idx--)
					;
				line = line.substring(0, idx);
				Matcher m = lfPat.matcher(line);
				if (m.matches()) {
					long clId = Long.parseLong(m.group(2));
					String transforms = m.group(3);
					transforms = transforms.replace("\\nlc7x--", "\n");
					int shortCheck = transforms.indexOf("str:");
					if (shortCheck == 0 || shortCheck == 1) {
						List<DomainTransformEvent> dtes = new PlaintextProtocolHandlerShort()
								.deserialize(transforms);
						result.addCollection(clId, dtes);
					} else {
						List<DomainTransformEvent> dtes = new PlaintextProtocolHandler()
								.deserialize(transforms);
						result.addCollection(clId, dtes);
					}
				}
			}
			for (List<DomainTransformEvent> dtes : result.values()) {
				Collections.sort(dtes,
						DomainTransformEvent.UTC_DATE_COMPARATOR);
				DomainTransformEvent last = null;
				for (Iterator<DomainTransformEvent> itr = dtes.iterator(); itr
						.hasNext();) {
					DomainTransformEvent current = itr.next();
					if (last != null
							&& last.toString().equals(current.toString())
							&& removeDuplicates) {
						itr.remove();
					}
					last = current;
				}
			}
			return result;
		}

		private static class LongFnComparator implements Comparator<File> {
			@Override
			public int compare(File o1, File o2) {
				Matcher m1 = NUMERIC_FN_PATTERN.matcher(o1.getName());
				m1.find();
				long l1 = Long.parseLong(m1.group(1));
				Matcher m2 = NUMERIC_FN_PATTERN.matcher(o2.getName());
				m2.find();
				long l2 = Long.parseLong(m2.group(1));
				return CommonUtils.compareLongs(l1, l2);
			}
		}
	}

	public static class RsrowToDteConverter {
		private boolean modColNames;

		public RsrowToDteConverter(boolean modColNames) {
			this.modColNames = modColNames;
		}

		long firstDtrId;

		Date firstEventDate;

		public List<DomainTransformEvent> convert(ResultSet rs)
				throws Exception {
			List<DomainTransformEvent> dtes = new ArrayList<DomainTransformEvent>();
			while (rs.next()) {
				DomainTransformEvent dte = TransformManager
						.createTransformEvent();
				dtes.add(dte);
				if (firstDtrId == 0) {
					firstDtrId = rs.getLong("dtr_id");
					firstEventDate = rs.getTimestamp("servercommitdate");
				}
				dte.setNewStringValue(rs.getString("newStringValue"));
				dte.setObjectClassRef(ClassRef.forId(rs.getLong(
						modColNames ? "dte_objref" : "objectclassref_id")));
				dte.setPropertyName(rs.getString("propertyname"));
				dte.setUtcDate(rs.getTimestamp("utcdate"));
				dte.setObjectId(
						rs.getLong(modColNames ? "object_id" : "objectId"));
				dte.setObjectLocalId(rs.getLong("objectlocalid"));
				dte.setValueId(rs.getLong("valueid"));
				dte.setValueClassRef(
						ClassRef.forId(rs.getLong("valueclassref_id")));
				int i = rs.getInt("transformtype");
				TransformType tt = rs.wasNull() ? null
						: TransformType.class.getEnumConstants()[i];
				dte.setTransformType(tt);
			}
			return dtes;
		}
	}

	static class TrimmedStringFormatter implements ColumnFormatter {
		private int length;

		public TrimmedStringFormatter(int length) {
			this.length = length;
		}

		@Override
		public String format(ResultSet rs, int columnIndex)
				throws SQLException {
			return CommonUtils.trimToWsChars(CommonUtils
					.nullToEmpty(rs.getString(columnIndex)).replace("\n", " "),
					length);
		}
	}
}

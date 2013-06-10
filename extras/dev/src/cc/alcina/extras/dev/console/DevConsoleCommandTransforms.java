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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Table;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRSimpleSerialSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.entity.util.SqlUtils.ColumnFormatter;

public class DevConsoleCommandTransforms {
	public static class LogsToDtrs{
		public LogsToDtrs() {
		}
		
		public Multimap<Long, List<DomainTransformEvent>> logsToDtrs(String logFile){
			Pattern lfPat=Pattern.compile("\\s*(\\d{4}.+?)\\s+\\| transform\\s+\\| (\\d+)\\s+\\|(.+)");
			Multimap<Long, List<DomainTransformEvent>> result=new Multimap<Long, List<DomainTransformEvent>>();
			String[] split = logFile.split("\n");
			List<String> strs=new ArrayList<String>(Arrays.asList(split));
			Collections.reverse(strs);
			for(String line:strs){
				Matcher m = lfPat.matcher(line);
				if(m.matches()){
					long clId=Long.parseLong(m.group(2));
					String transforms=m.group(3);
					transforms=transforms.replace("\\nlc7x--", "\n");
					List<DomainTransformEvent> dtes = new PlaintextProtocolHandler().deserialize(transforms);
					result.addCollection(clId, dtes);
				}
			}
			for(List<DomainTransformEvent> dtes:result.values()){
				Collections.sort(dtes,DomainTransformEvent.UTC_DATE_COMPARATOR);
				DomainTransformEvent last=null;
				for(Iterator<DomainTransformEvent> itr=dtes.iterator();itr.hasNext();){
					DomainTransformEvent current = itr.next();
					if(last!=null&&last.toString().equals(current.toString())){
						itr.remove();
					}
					last=current;
				}
			}
			return result;
		}

		public Multimap<Long, List<DomainTransformEvent>> dtrExpsToCliDteMap(
				String folderPath) throws Exception {
			Multimap<Long, List<DomainTransformEvent>> result=new Multimap<Long, List<DomainTransformEvent>>();
			List<DTRSimpleSerialWrapper> wrappers = new ArrayList<DTRSimpleSerialWrapper>();
			List<File> files = new ArrayList<File>(Arrays.asList(new File(folderPath)
					.listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							return NUMERIC_FN_PATTERN.matcher(
									pathname.getName()).matches();
						}
					})));
			Collections.sort(files, new LongFnComparator());
			int processedIndex = 0;
			for (; processedIndex < files.size();) {
				File f = files.get(processedIndex++);
				String ser = ResourceUtilities.readFileToStringGz(f);
				DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialSerializer()
						.read(ser);
				DomainTransformRequest rq = new DomainTransformRequest();
				rq.fromString(wrapper.getText());
				result.addCollection(wrapper.getClientInstanceId(), rq.getEvents());
			}
			return result;
		}
		private static final Pattern NUMERIC_FN_PATTERN = Pattern
				.compile("(\\d+)\\.(?:txt\\.gz|txt)");
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
					+ "inner join users u on ci.user_id=u.id "
					+ "where ci.id != -1 %s order by ci.id desc";
			String arg0 = argv[0];
			String arg1 = argv.length < 2 ? "0" : argv[1];
			String arg2 = argv.length < 3 ? "7" : argv[2];
			String filter = "";
			filter += arg0.equals("0") ? "" : String.format(" and ci.id=%s ",
					arg0);
			if (!arg1.equals("0")) {
				filter += arg1.matches("\\d+") ? String.format(" and u.id=%s ",
						arg1) : String.format(" and u.username='%s' ", arg1);
			}
			filter += arg2.equals("0") ? "" : String.format(
					"  and age(ci.hellodate)<'%s days'  ", arg2);
			Connection conn = getConn();
			sql = String.format(sql, filter);
			System.out.println(console.breakAndPad(1, 80, sql, 0));
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			SqlUtils.dumpResultSet(rs);
			return "";
		}
	}

	public static class CmdListClientLogRecords extends DevConsoleCommand {
		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "trl" };
		}

		@Override
		public String getDescription() {
			return "list client log records";
		}

		@Override
		public String getUsage() {
			return "trl {params or none for usage}";
		}

		@RegistryLocation(registryPoint = CmdListClientLogRecordsFilter.class)
		public abstract static class CmdListClientLogRecordsFilter extends
				DevConsoleFilter {
		}

		public static class CmdListClientLogRecordsFilterClientInstance extends
				CmdListClientLogRecordsFilter {
			@Override
			public String getFilter(String value) {
				value = CommonUtils.isNullOrEmpty(value) ? "-1" : value;
				return String.format(value.contains(",") ? "ci.id in (%s)"
						: "ci.id=%s", value);
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

		public static class CmdListClientLogRecordsFilterUserId extends
				CmdListClientLogRecordsFilter {
			@Override
			public String getFilter(String value) {
				value = CommonUtils.isNullOrEmpty(value) ? "-1" : value;
				return String.format(value.contains(",") ? "u.id in (%s)"
						: "u.id=%s", value);
			}

			@Override
			public String getKey() {
				return "us";
			}

			@Override
			protected boolean hasDefault() {
				return false;
			}
		}

		public static class CmdListClientLogRecordsFilterMessage extends
				CmdListClientLogRecordsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("clr.message ilike '%%%s%%'", value);
			}

			@Override
			public String getKey() {
				return "mes";
			}

			@Override
			protected boolean hasDefault() {
				return false;
			}
		}

		public static class CmdListClientLogRecordsFilterTopic extends
				CmdListClientLogRecordsFilter {
			@Override
			public String getFilter(String value) {
				StringMap keys = StringMap
						.fromPropertyString("s=stat\nc=click\nh=history\nm=message\nt=transform\nz=metric");
				String notClause = "";
				if (value.startsWith("!")) {
					notClause = "not";
				}
				List<String> strs = new ArrayList<String>();
				for (String key : keys.keySet()) {
					if (value.contains(key)) {
						strs.add(keys.get(key));
					}
				}
				return String.format("%s clr.topic in %s", notClause,
						EntityUtils.stringListToClause(strs));
			}

			@Override
			public String getKey() {
				return "top";
			}

			@Override
			protected boolean hasDefault() {
				return false;
			}
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				return "Usage: trl {-t: trim message}{-l: single-line message}{-m: message only} ({ci|top|mes|us} value)+ - top is topic {!}{s|m|c|t}";
			}
			FilterArgvResult f = new FilterArgvResult(argv, "-t");
			FilterArgvResult f2 = new FilterArgvResult(f.argv, "-l");
			FilterArgvResult f3 = new FilterArgvResult(f2.argv, "-m");
			String sql = "select %s%s from "
					+ "clientlogrecord clr inner join "
					+ " client_instance ci on clr.clientinstanceid = ci.id "
					+ " inner join users u " + "on ci.user_id=u.id " + "where "
					+ " %s order by clr.id desc";
			String metaSelect = f3.contains ? "" : "clr.time, clr.topic, ci.id, ";
			String messageSelect = f.contains ? "substr(replace(clr.message,'\\n','\\\\n'),0,80)"
					: f2.contains ? "replace(clr.message,'\\n','\\\\nlc7x--')"
							: "clr.message";
			argv = f3.argv;
			String filter = DevConsoleFilter.getFilters(
					CmdListClientLogRecordsFilter.class, argv, null);
			Connection conn = getConn();
			sql = String.format(sql, metaSelect, messageSelect, filter);
			System.out.println(console.breakAndPad(1, 80, sql, 0));
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			SqlUtils.dumpResultSet(rs);
			return "";
		}
	}

	public static class CmdListTransforms extends DevConsoleCommand {
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

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				printFullUsage();
				return "";
			}
			Connection conn = getConn();
			ensureClassRefs(conn);
			CommonPersistenceLocal cpl = EntityLayerLocator.get()
					.commonPersistenceProvider()
					.getCommonPersistenceExTransaction();
			Class<? extends DomainTransformRequestPersistent> clazz = cpl
					.getImplementation(DomainTransformRequestPersistent.class);
			String dtrName = clazz.getAnnotation(Table.class).name();
			Class<? extends DomainTransformEventPersistent> class1 = cpl
					.getImplementation(DomainTransformEventPersistent.class);
			String dteName = class1.getAnnotation(Table.class).name();
			String sql1 = "select dtr.id as id" + " from client_instance ci "
					+ "inner join users u on ci.user_id=u.id "
					+ " inner join %s dtr on dtr.clientinstance_id=ci.id "
					+ "where %s order by dtr.id desc";
			String sql2 = "select ci.id as cli_id, u.username,  "
					+ "  dtr.id as dtr_id, dte.id as dte_id, "
					+ " dte.objectclassref_id as dte_objref, dte.objectid as object_id, "
					+ "dte.propertyname as propertyname, "
					+ " dte.newstringvalue as newstringvalue,dte.transformtype as transformtype, "
					+ " dte.valueid,"
					+ " dte.servercommitdate as servercommitdate, dte.objectlocalid "
					+ "from client_instance ci "
					+ "inner join users u on ci.user_id=u.id "
					+ " inner join %s dtr on dtr.clientinstance_id=ci.id "
					+ " inner join %s dte on dte.domaintransformrequestpersistent_id = dtr.id"
					+ " where %s order by dte.id desc";
			{
				CollectionFilter<String> allowFilter = new CollectionFilter<String>() {
					@Override
					public boolean allow(String o) {
						return !o.contains("dte.");
					}
				};
				String filter = DevConsoleFilter.getFilters(
						CmdListTransformsFilter.class, argv, allowFilter);
				sql1 = String.format(sql1, dtrName, filter);
				Statement ps = conn.createStatement();
				System.out.println(console.breakAndPad(1, 80, sql1, 0));
				Set<Long> ids = SqlUtils.toIdList(ps, sql1, "id", false);
				ps.close();
				List<String> args = new ArrayList<String>(Arrays.asList(argv));
				args.add("dtr");
				args.add(CommonUtils.join(ids, ", "));
				argv = (String[]) args.toArray(new String[args.size()]);
			}
			{
				String filter = DevConsoleFilter.getFilters(
						CmdListTransformsFilter.class, argv);
				sql2 = String.format(sql2, dtrName, dteName, filter);
				PreparedStatement ps = conn.prepareStatement(sql2);
				System.out.println(console.breakAndPad(1, 80, sql2, 0));
				ResultSet rs = ps.executeQuery();
				Map<String, ColumnFormatter> formatters = new HashMap<String, SqlUtils.ColumnFormatter>();
				formatters.put("dte_objref", new ClassRefNameFormatter());
				formatters.put("servercommitdate", new DateTimeFormatter());
				formatters.put("transformtype", new EnumFormatter(
						TransformType.class));
				formatters
						.put("newstringvalue", new TrimmedStringFormatter(30));
				SqlUtils.dumpResultSet(rs, formatters);
				rs.close();
				ps.close();
			}
			return "";
		}

		static boolean classRefsEnsured;

		private void ensureClassRefs(Connection conn) throws Exception {
			if (classRefsEnsured) {
				return;
			}
			System.out.println("getting classrefs...");
			Statement ps = conn.createStatement();
			ResultSet rs = ps.executeQuery("select * from classref");
			while (rs.next()) {
				long id = rs.getLong("id");
				String cn = rs.getString("refclassname");
				Class clazz = Registry.get().lookupSingle(
						AlcinaPersistentEntityImpl.class, ClassRef.class);
				ClassRef cr = (ClassRef) clazz.newInstance();
				cr.setId(id);
				cr.setRefClassName(cn);
				cr.setRefClass(Class.forName(cn));
				ClassRef.add(Collections.singleton(cr));
			}
			ps.close();
			classRefsEnsured = true;
		}

		private void printFullUsage() {
			System.out
					.println("trt {[days|user|ci|class|dtr|pn|objid|valueid|nsv] value}+");
		}

		@RegistryLocation(registryPoint = CmdListTransformsFilter.class)
		public abstract static class CmdListTransformsFilter extends
				DevConsoleFilter {
		}

		public static class CmdListTransformsFilterClientInstance extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = CommonUtils.isNullOrEmpty(value) ? "-1" : value;
				return String.format(value.contains(",") ? "ci.id in (%s)"
						: "ci.id=%s", value);
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

		public static class CmdListTransformsFilterCreationIds extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(final String arg1) {
				return "dte.transformType=0";
			}

			@Override
			public String getKey() {
				return "creationIds";
			}
		}

		public static class CmdListTransformsFilterClass extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(final String arg1) {
				Set<ClassRef> refs = ClassRef.all();
				CollectionFilter<ClassRef> classNameFilter = new CollectionFilter<ClassRef>() {
					Pattern namePattern = Pattern.compile(arg1,
							Pattern.CASE_INSENSITIVE);

					@Override
					public boolean allow(ClassRef o) {
						return namePattern.matcher(o.getRefClassName()).find();
					}
				};
				List<ClassRef> filteredRefs = CollectionFilters.filter(refs,
						classNameFilter);
				return String.format("dte.objectclassref_id in %s",
						EntityUtils.hasIdsToIdClause(filteredRefs));
			}

			@Override
			public String getKey() {
				return "class";
			}
		}

		public static class CmdListTransformsFilterDays extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("age(ci.hellodate)<'%s days'",
						value == null ? "3" : value);
			}

			@Override
			public String getKey() {
				return "days";
			}

			protected boolean hasDefault() {
				return true;
			}
		}

		public static class CmdListTransformsFilterDtrId extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = value.isEmpty() ? "-1" : value;
				return String.format(value.contains(",") ? "dtr.id in (%s)"
						: "dtr.id=%s", value);
			}

			@Override
			public String getKey() {
				return "dtr";
			}
		}

		public static class CmdListTransformsFilterDteId extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				value = value.isEmpty() ? "-1" : value;
				return String.format(value.contains(",") ? "dte.id in (%s)"
						: "dte.id=%s", value);
			}

			@Override
			public String getKey() {
				return "dte";
			}
		}

		public static class CmdListTransformsFilterUser extends
				CmdListTransformsFilter {
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

		public static class CmdListTransformsFilterNewStringValue extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String arg1) {
				return String.format("dte.newStringValue ilike '%%%s%%'", arg1);
			}

			@Override
			public String getKey() {
				return "nsv";
			}
		}

		public static class CmdListTransformsFilterValueId extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format(
						value.contains(",") ? "dte.valueid in (%s)"
								: "dte.valueid=%s", value);
			}

			@Override
			public String getKey() {
				return "valueid";
			}
		}

		public static class CmdListTransformsObjectId extends
				CmdListTransformsFilter {
			@Override
			public String getFilter(String value) {
				return String.format("dte.objectid =%s", value);
			}

			@Override
			public String getKey() {
				return "objid";
			}
		}

		public static class CmdListTransformsPropertyName extends
				CmdListTransformsFilter {
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

	public static class CmdListUsers extends DevConsoleCommand {
		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "trus" };
		}

		@Override
		public String getDescription() {
			return "list users with filters";
		}

		@Override
		public String getUsage() {
			return "trus {user_id|username substring}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String sql = "select  u.id, u.username  " + "from users u  "
					+ "where u.id != -1 %s order by u.id desc";
			String arg0 = argv[0];
			String filter = "";
			filter += arg0.matches("\\d+") ? String.format(" and u.id=%s ",
					arg0) : String.format(" and u.username ilike '%%%s%%' ",
					arg0);
			Connection conn = getConn();
			sql = String.format(sql, filter);
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			SqlUtils.dumpResultSet(rs);
			return String.format("set id to '%s'", console.props.idOrSet);
		}
	}

	public abstract static class DevConsoleFilter {
		public static String getFilters(
				Class<? extends DevConsoleFilter> registryPoint, String[] argv) {
			return getFilters(registryPoint, argv, null);
		}

		public static String getFilters(
				Class<? extends DevConsoleFilter> registryPoint, String[] argv,
				CollectionFilter<String> allowFilter) {
			List<String> filters = new ArrayList<String>();
			List<? extends DevConsoleFilter> impls = Registry
					.impls(registryPoint);
			StringMap kv = new StringMap();
			for (int i = 0; i < argv.length; i += 2) {
				kv.put(argv[i], argv[i + 1]);
			}
			for (DevConsoleFilter impl : impls) {
				if (kv.containsKey(impl.getKey()) || impl.hasDefault()) {
					String filterString = impl.getFilter(kv.get(impl.getKey()));
					if (allowFilter == null || allowFilter.allow(filterString)) {
						filters.add(filterString);
					}
				}
			}
			return CommonUtils.join(filters, " and ");
		}

		public abstract String getFilter(String value);

		public abstract String getKey();

		protected boolean hasDefault() {
			return false;
		}
	}

	static class ClassRefNameFormatter implements ColumnFormatter {
		@Override
		public String format(ResultSet rs, int columnIndex) throws SQLException {
			long objRefId = rs.getLong(columnIndex);
			return ClassRef.forId(objRefId).getRefClass().getSimpleName();
		}
	}

	static class DateTimeFormatter implements ColumnFormatter {
		@Override
		public String format(ResultSet rs, int columnIndex) throws SQLException {
			Timestamp ts = rs.getTimestamp(columnIndex);
			return CommonUtils.formatDate(ts, DateStyle.AU_DATE_TIME_HUMAN);
		}
	}

	static class EnumFormatter implements ColumnFormatter {
		private Class<? extends Enum> clazz;

		public EnumFormatter(Class<? extends Enum> clazz) {
			this.clazz = clazz;
		}

		@Override
		public String format(ResultSet rs, int columnIndex) throws SQLException {
			int i = rs.getInt(columnIndex);
			return rs.wasNull() ? null : clazz.getEnumConstants()[i].toString();
		}
	}

	static class TrimmedStringFormatter implements ColumnFormatter {
		private int length;

		public TrimmedStringFormatter(int length) {
			this.length = length;
		}

		@Override
		public String format(ResultSet rs, int columnIndex) throws SQLException {
			return CommonUtils.trimToWsChars(
					CommonUtils.nullToEmpty(rs.getString(columnIndex)), length);
		}
	}
}

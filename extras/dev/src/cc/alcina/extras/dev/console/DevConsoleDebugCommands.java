package cc.alcina.extras.dev.console;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang.StringEscapeUtils;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.extras.dev.console.DevConsoleDebugCommands.CmdDrillClientException.DevConsoleDebugPaths;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.CollectionFilters.ConverterFilter;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.ReplayInstruction;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SortedMultimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.console.FilterArgvFlag;
import cc.alcina.framework.entity.console.FilterArgvParam;
import cc.alcina.framework.entity.projection.EntityUtils;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.entity.util.StreamBuffer;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import nl.bitwalker.useragentutils.Browser;
import nl.bitwalker.useragentutils.RenderingEngine;
import nl.bitwalker.useragentutils.UserAgent;

public class DevConsoleDebugCommands {
	public static final String USER_AGENT = "User agent: ";

	public static final Pattern UA_PATTERN = Pattern
			.compile(USER_AGENT + "(.+)");

	static DevConsoleDebugPeer getPeer() {
		return Registry.impl(DevConsoleDebugPeer.class);
	}

	public static class CmdCountClientExceptions extends DevConsoleCommand {
		@Override
		public boolean clsBeforeRun() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "dxc" };
		}

		@Override
		public String getDescription() {
			return "Count and format exceptions\n(filter"
					+ " by {key:=key|all|pub,d,c,r,parser,t,m,j} and/or {user_id} ";
		}

		@Override
		public String getUsage() {
			return "dxc  {-c key} {-u userid} {-d days}"
					+ " {-i min-id} {-r regex} {-rn not-regex} {-fu id - list(client)users with similar exceptions to id}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			List<ILogRecord> logRecords = console.state.logRecords;
			for (ILogRecord o : logRecords) {
				o.setText(CommonUtils.nullToEmpty(o.getText()));
			}
			if (CommonUtils.isNullOrEmpty(logRecords)) {
				runSubcommand(new CmdGetExceptionLogs(), null);
				logRecords = console.state.logRecords;
			}
			Set<String> affectedUserNames = new LinkedHashSet<String>();
			Pattern clientP = Pattern.compile(
					"(?:cc\\.alcina\\.framework\\.common\\.client\\.csobjects\\.WebException:|RPC exception:)(.+)",
					Pattern.MULTILINE | Pattern.DOTALL);
			Pattern p = clientP;
			FilterArgvParam filterArgvResult = new FilterArgvParam(argv, "-c");
			String componentKey = filterArgvResult.value;
			argv = filterArgvResult.argv;
			if (componentKey != null) {
				p = Pattern.compile("(.*)", Pattern.MULTILINE | Pattern.DOTALL);
			}
			if (componentKey != null) {
				logRecords = filterByComponent(logRecords, componentKey);
			}
			CollectionFilter<ILogRecord> customFilter = new CollectionFilter<ILogRecord>() {
				Pattern p = Pattern.compile(
						"(Citables initialization failed|CitablesSingletonHolder|Citable\\.root)",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

				@Override
				public boolean allow(ILogRecord o) {
					boolean matches = p.matcher(o.getText()).find();
					return !matches;
				}
			};
			logRecords = CollectionFilters.filter(logRecords, customFilter);
			filterArgvResult = new FilterArgvParam(argv, "-u");
			argv = filterArgvResult.argv;
			if (filterArgvResult.value != null) {
				long userId = Long.parseLong(filterArgvResult.value);
				if (userId != 0) {
					logRecords = CollectionFilters.filterByProperty(logRecords,
							ILogRecord.USER_ID, userId);
				}
			}
			filterArgvResult = new FilterArgvParam(argv, "-r");
			argv = filterArgvResult.argv;
			if (filterArgvResult.value != null) {
				final Pattern rp = Pattern.compile(filterArgvResult.value,
						Pattern.CASE_INSENSITIVE);
				CollectionFilter<ILogRecord> containsTextFilter = new CollectionFilter<ILogRecord>() {
					@Override
					public boolean allow(ILogRecord o) {
						return rp.matcher(o.getText()).find();
					}
				};
				logRecords = CollectionFilters.filter(logRecords,
						containsTextFilter);
			}
			filterArgvResult = new FilterArgvParam(argv, "-rn");
			argv = filterArgvResult.argv;
			if (filterArgvResult.value != null) {
				final Pattern rp = Pattern.compile(filterArgvResult.value,
						Pattern.CASE_INSENSITIVE);
				CollectionFilter<ILogRecord> containsTextFilter = new CollectionFilter<ILogRecord>() {
					@Override
					public boolean allow(ILogRecord o) {
						return !rp.matcher(o.getText()).find();
					}
				};
				logRecords = CollectionFilters.filter(logRecords,
						containsTextFilter);
			}
			CountingMap<String> byType = new CountingMap<String>();
			filterArgvResult = new FilterArgvParam(argv, "-d");
			argv = filterArgvResult.argv;
			if (filterArgvResult.value != null) {
				int days = Integer.parseInt(filterArgvResult.value);
				if (days != 0) {
					final Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					c.add(Calendar.DATE, -days);
					CollectionFilter<ILogRecord> dayRecencyFilter = new CollectionFilter<ILogRecord>() {
						@Override
						public boolean allow(ILogRecord o) {
							return c.getTime().compareTo(o.getCreatedOn()) <= 0;
						}
					};
					logRecords = CollectionFilters.filter(logRecords,
							dayRecencyFilter);
				}
			}
			filterArgvResult = new FilterArgvParam(argv, "-i");
			argv = filterArgvResult.argv;
			if (filterArgvResult.value != null) {
				final int minId = Integer.parseInt(filterArgvResult.value);
				CollectionFilter<ILogRecord> minIdFilter = new CollectionFilter<ILogRecord>() {
					@Override
					public boolean allow(ILogRecord o) {
						return o.getId() > minId;
					}
				};
				logRecords = CollectionFilters.filter(logRecords, minIdFilter);
			}
			filterArgvResult = new FilterArgvParam(argv, "-fu");
			argv = filterArgvResult.argv;
			List<Long> similarToIds = new ArrayList<Long>();
			if (filterArgvResult.value != null) {
				similarToIds = TransformManager
						.idListToLongs(filterArgvResult.value);
			}
			final Multimap<String, List<Long>> textIdLookup = new Multimap<String, List<Long>>();
			String pad = "\n" + CommonUtils.padStringLeft("", 20, " ");
			Map<Long, ILogRecord> idLkp = new LinkedHashMap<Long, ILogRecord>();
			for (ILogRecord l : logRecords) {
				byType.add(l.getComponentKey());
			}
			for (ILogRecord l : logRecords) {
				String str = l.getText();
				Matcher m = p.matcher(str);
				if (m.find()) {
					String exText = CommonUtils
							.trimToWsChars(m.group(1), 250, true)
							.replace("\n", pad);
					textIdLookup.add(exText, l.getId());
					idLkp.put(l.getId(), l);
				}
			}
			Set<Entry<Integer, List<String>>> entrySet = new CountingMap(
					textIdLookup).reverseMap(true).entrySet();
			for (Entry<Integer, List<String>> entry : entrySet) {
				String o = "";
				List<String> keys = entry.getValue();
				for (String v : entry.getValue()) {
					List<Long> ids = textIdLookup.get(v);
					Collections.sort(ids);
				}
				Collections.sort(keys, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						List<Long> ids = textIdLookup.get(o1);
						long id1 = CommonUtils.last(ids);
						ids = textIdLookup.get(o2);
						long id2 = CommonUtils.last(ids);
						return (int) (id2 - id1);
					}
				});
				for (String v : entry.getValue()) {
					List<Long> ids = textIdLookup.get(v);
					String allIds = ids.size() == 1 ? ""
							: String.format("[%s]",
									CommonUtils.join(ids, ", "));
					if (similarToIds.isEmpty()) {
						Set<String> userNames = new LinkedHashSet<String>();
						for (Long id : ids) {
							ILogRecord record = idLkp.get(id);
							userNames.add(console.state
									.getUser(record.getUserId()).getUserName());
						}
						o += String.format("%-20s%s\n%-20s%s\t%s\n",
								CommonUtils.last(ids), v, "", userNames,
								allIds);
					} else {
						Set intersection = CommonUtils.intersection(ids,
								similarToIds);
						if (intersection.size() > 0) {
							for (Long id : ids) {
								ILogRecord record = idLkp.get(id);
								o += String.format(
										"%-20s %-20s  %-20s  %-20s\n",
										intersection.iterator().next(), id,
										record.getUserId(), console.state
												.getUser(record.getUserId()));
								affectedUserNames.add(console.state
										.getUser(record.getUserId())
										.getUserName());
							}
						}
					}
				}
				System.out.println(
						String.format("Count: %s\n%s\n", entry.getKey(), o));
			}
			if (affectedUserNames.size() > 0) {
				System.out.format("\n\n%s\n\n",
						CommonUtils.join(affectedUserNames, ", "));
			}
			Ax.out("Record count: %s", logRecords.size());
			if (byType.size() > 1) {
				SortedMultimap<Integer, List<String>> reverseMap = byType
						.reverseMap(true);
				String template = "%-50s  %s\n";
				System.out.format(template, "Type", "Count");
				System.out.println(CommonUtils.padStringLeft("", 66, "-"));
				for (Entry<Integer, List<String>> types : reverseMap
						.entrySet()) {
					for (String type : types.getValue()) {
						if (type.contains("EXCEPTION")) {
							System.out.format(template, type, types.getKey());
						}
					}
				}
			}
			return "";
		}

		private List<ILogRecord> filterByComponent(List<ILogRecord> logRecords,
				String componentKey) {
			return getPeer().filterByComponent(logRecords, componentKey);
		}
	}

	public static class CmdCountExceptionsByUser extends DevConsoleCommand {
		private static final int DEFAULT_COUNT_EXCEPTIONS_IN_LAST_X_DAYS = 30;

		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "dxu" };
		}

		@Override
		public String getDescription() {
			return "Count exceptions by user, last x days";
		}

		@Override
		public String getUsage() {
			return "dxu {x}:";
		}

		@Override
		public String run(String[] argv) throws Exception {
			Connection conn = getConn();
			int days = getIntArg(argv, 0,
					DEFAULT_COUNT_EXCEPTIONS_IN_LAST_X_DAYS);
			String ckFilter = new CmdGetExceptionLogs()
					.getExceptionIgnoreClause();
			String sql = String.format(
					"select u.username,u.id, count(l.id) as errct from logging l inner join users u on l.user_id=u.id"
							+ " where age(created_on)<'%s days' and "
							+ " not (l.component_key in %s)"
							+ "group by u.username,u.id order by count(l.id) desc;",
					days, ckFilter);
			System.out.format(
					"Exceptions in last %s days, by user\n---------------------\n\n",
					days);
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			SqlUtils.dumpResultSet(rs);
			return String.format("Counted exceptions for last %s days - \n"
					+ " remember to cache all exceptions for that period  "
					+ "before drilldown/count by running \n\tdxg %s\n\n ", days,
					days);
		}
	}

	public static class CmdDrillClientException extends DevConsoleCommand {
		static ILogRecord lastRecord;

		private File module;

		private File symbol;

		private boolean getJs;

		private File jsfDir;

		private String fnOverride;

		List<File> jsFiles = new ArrayList<File>();

		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public boolean clsBeforeRun() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "dxd" };
		}

		@Override
		public String getDescription() {
			return "Drill down to exception - run with zero params for usage";
		}

		@Override
		public String getUsage() {
			return "dxd  {id} <-js> <-fn fname> <-o outputformat>";
		}

		public List<ClientLogRecord>
				parseSerializedLogRecords(String serializedLogRecords) {
			Converter<String, ClientLogRecords> converter = new Converter<String, ClientLogRecord.ClientLogRecords>() {
				@Override
				public ClientLogRecords convert(String original) {
					try {
						return new AlcinaBeanSerializerS()
								.deserialize(original);
					} catch (Exception e) {
						System.out.format(
								"problem deserializing clientlogrecord:\n%s\n",
								original);
						e.printStackTrace();
						if (ResourceUtilities.getBoolean(
								CommonRemoteServiceServlet.class,
								"throwLogClientRecordExceptions")) {
							throw new WrappedRuntimeException(e);
						}
						return null;
					}
				}
			};
			List<String> lines = Arrays
					.asList(serializedLogRecords.split("\n"));
			List<ClientLogRecords> records = CollectionFilters.convert(lines,
					converter);
			while (records.remove(null)) {
			}
			List<ClientLogRecord> clrs = new ArrayList<ClientLogRecord>();
			for (ClientLogRecords clientLogRecords : records) {
				clrs.addAll(clientLogRecords.getLogRecords());
			}
			return clrs;
		}

		@Override
		public void printUsage() {
			System.out.println(getUsage());
		}

		@Override
		public String run(String[] argv) throws Exception {
			try {
				LooseContext.pushWithTrue(DevConsole.CONTEXT_NO_TRUNCATE);
				console.clear();
				return run0(argv);
			} finally {
				LooseContext.pop();
				console.scrollToTopAtEnd();
			}
		}

		private void deObfStacktrace(String text, String mn, Browser browser)
				throws Exception {
			Pattern symbolMapPattern = Pattern.compile("(.+?),(.+)");
			Pattern wkp = Pattern
					.compile("Unknown\\.(.+?)\\(Unknown Source\\)");
			Pattern wkp2 = Pattern.compile("Unknown\\.(.+?)\\(.+");
			Pattern ie6p = wkp;
			// Pattern ffp = Pattern.compile("(?:stack: )(.+?)\\(.+");
			Pattern ffp2 = Pattern.compile("([^@(]+)(?:\\([^@]*\\))?@.+");
			Pattern sf6 = Pattern.compile("Unknown\\.anonymous\\((.+?)@.+");
			Pattern ie10 = Pattern
					.compile(".* at (.+?) \\(Unknown script code.+");
			Pattern wkStr = Pattern.compile("(?:stack: )?(.+?)@https://jade.+");
			Pattern[] trialsFF = { ffp2, wkp2 };
			Pattern[] trialsNonFF = { wkStr, sf6, wkp, wkp2, ie10 };
			Pattern[] trials = browser
					.getRenderingEngine() == RenderingEngine.GECKO ? trialsFF
							: trialsNonFF;
			String symbolMapContents = ResourceUtilities
					.readFileToString(symbol);
			String moduleContents = ResourceUtilities.readFileToString(module);
			Matcher symbolMapMatcher = symbolMapPattern
					.matcher(symbolMapContents);
			// Matcher m2 = stp.matcher(stf);
			LinkedHashMap<String, String> km = new LinkedHashMap<String, String>();
			LinkedHashMap<String, String> stm = new LinkedHashMap<String, String>();
			boolean ffStack = browser
					.getRenderingEngine() == RenderingEngine.GECKO;// text.contains("stack:
																	// ");
			String regex = ffStack && text.contains("stack:")
					? "stack:(.+)-----\n"
					: text.contains("compatible; MSIE 10.0")
							? String.format("%s(.+)%s", "", USER_AGENT)
							: String.format("%s(.+)%s", "-----\n", USER_AGENT);
			Pattern stackTraceExtract = Pattern.compile(regex,
					Pattern.MULTILINE | Pattern.DOTALL);
			Matcher stem = stackTraceExtract.matcher(text);
			stem.find();
			String obfStacktrace = stem.group(1);
			while (symbolMapMatcher.find()) {
				km.put(symbolMapMatcher.group(1), symbolMapMatcher.group(2));
			}
			CountingMap<Pattern> counter = new CountingMap<Pattern>();
			for (Pattern trial : trials) {
				Matcher m = trial.matcher(obfStacktrace);
				while (m.find()) {
					if (!m.group()
							.equals("Unknown.anonymous(Unknown Source)")) {
						counter.add(trial);
					}
				}
			}
			if (counter.isEmpty()) {
				return;
			}
			Pattern max = counter.max();
			Matcher m2 = max.matcher(obfStacktrace);
			int ctr = 0;
			String firstFrame = "";
			Pattern lnp = Pattern.compile("^\\s+Line: (\\d+)",
					Pattern.MULTILINE);
			Matcher lnm = lnp.matcher(text);
			int lineNumber = 0;
			String[] moduleLines = null;
			System.out.println("\n\nDeobfuscated stacktrace:\n\n");
			if (lnm.find()) {
				try {
					lineNumber = Integer.parseInt(lnm.group(1));
					moduleLines = moduleContents.split("\n");
					Pattern moduleLineFuncPat = Pattern
							.compile("function (.+?)\\(");
					Matcher m3 = moduleLineFuncPat
							.matcher(moduleLines[lineNumber - 1]);
					if (m3.find()) {
						String fn = m3.group(1).trim();
						String unobffn = km.get(fn);
						System.out.format("\tLine number %s:\n\t%s\n\n",
								lineNumber,
								(unobffn == null ? "***" + m3.group()
										: unobffn));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("**invliad line number, rpperply");
					lineNumber = 0;
				}
			}
			int last = 0;
			boolean breakForFn = false;
			while (m2.find()) {
				if (m2.start() - last > 10) {
					System.out.println("\n-----\n");
				}
				last = m2.end();
				String fn = m2.group(1).trim();
				fn = fnOverride == null ? fn : fnOverride;
				String unobffn = km.get(fn);
				if (fnOverride != null && unobffn != null) {
					breakForFn = true;
				}
				System.out.println("\t"
						+ (unobffn == null ? "***" + m2.group() : unobffn));
				if (ctr == 0) {
					String js = null;
					StringMap defJsLines = new StringMap();
					if (lineNumber != 0) {
						js = moduleLines[lineNumber - 1];
					} else {
						Pattern modP = Pattern
								.compile(String.format("function %s\\(.+", fn));
						int indexOf = moduleContents
								.indexOf(String.format("function %s(", fn));
						Matcher modM = modP.matcher(moduleContents);
						if (modM.find()) {
							js = modM.group();
						}
						if (getJs) {
							defJsLines.putAll(getDefJsFunctionLines(fn));
						}
					}
					if (js != null) {
						defJsLines.put("initial-module", js);
					}
					if (lineNumber != 0) {
						if (getJs) {
							defJsLines.putAll(getDefJsLines(lineNumber));
						}
					}
					if (defJsLines.size() > 0) {
						ScriptEngineManager manager = new ScriptEngineManager();
						ScriptEngine engine = manager
								.getEngineByName("JavaScript");
						for (Entry<String, String> jsLine : defJsLines
								.entrySet()) {
							List<String> result = new ArrayList<String>();
							engine.put("result", result);
							String script = ResourceUtilities
									.readStreamToString(
											getClass().getResourceAsStream(
													"beautify.js"));
							// String eval = String.format(
							// "%s\nvar js='%s';result.add(js_beautify(js));",
							// script,
							// StringEscapeUtils.escapeJavaScript(
							// CommonUtils.trimToWsChars(
							// jsLine.getValue(), 500)));
							// engine.eval(eval);
							// firstFrame += String.format(
							// "\n%s\n============\n%s\n", jsLine.getKey(),
							// result.get(0));
							firstFrame += String.format(
									"\n%s\n============\n%s\n", jsLine.getKey(),
									StringEscapeUtils.escapeJavaScript(
											CommonUtils.trimToWsChars(
													jsLine.getValue(), 500)));
						}
					}
				}
				ctr++;
				if (breakForFn) {
					fnOverride = null;
					break;
				}
			}
			String whichFrame = lineNumber == 0 ? "First frame:"
					: String.format("Exception line (%s):", lineNumber);
			System.out.format("\n" + whichFrame + "\n\n\t%s\n\n",
					firstFrame.replace("\\n", "\n").replace("\n", "\n\t"));
		}

		private void dumpHistory(String serializedLogRecords) {
			List<ClientLogRecord> clrs = parseSerializedLogRecords(
					serializedLogRecords);
			Converter<ClientLogRecord, String> recordsConverter = new Converter<ClientLogRecord, String>() {
				@Override
				public String convert(ClientLogRecord record) {
					return String.format("%-30s | %-30s | %s", record.getTime(),
							record.getTopic(), console.breakAndPad(0, 100,
									record.getMessage(), 66));
				}
			};
			System.out.println(String.format("%-30s | %-30s | %s", "Time",
					"Topic", "Event"));
			Collections.reverse(clrs);
			System.out.println(CommonUtils.join(
					CollectionFilters.convert(clrs, recordsConverter), "\n"));
		}

		private boolean ensureModuleAndSymbolMap(String mn) throws Exception {
			DevConsoleDebugPaths paths = getPeer().getPaths(console.props);
			File devFolder = console.devHelper.getDevFolder();
			File gwtSymbols = SEUtilities.getChildFile(devFolder,
					"gwt-symbols");
			gwtSymbols.mkdir();
			module = new File(
					String.format("%s/%s.cache.js", gwtSymbols.getPath(), mn));
			symbol = new File(
					String.format("%s/%s.symbolMap", gwtSymbols.getPath(), mn));
			if (!module.exists()) {
				importViaRsync(String.format("%s%s/%s.cache.js",
						paths.remoteRoot, paths.modPath, mn), module);
			}
			if (getJs) {
				jsfDir = new File(String.format("%s/deferredjs/%s",
						gwtSymbols.getPath(), mn));
				jsfDir.mkdirs();
				if (jsfDir.listFiles().length != 9) {
					importViaRsync(
							String.format("%s%s/deferredjs/%s/*",
									paths.remoteRoot, paths.modPath, mn),
							jsfDir);
				}
			}
			if (!symbol.exists()) {
				importViaRsync(String.format("%s%s/%s.symbolMap",
						paths.remoteRoot, paths.symPath, mn), symbol);
			}
			return module.exists() && symbol.exists();
		}

		private String extractReplay(String serializedLogRecords) {
			List<ClientLogRecord> clrs = parseSerializedLogRecords(
					serializedLogRecords);
			ConverterFilter<ClientLogRecord, ReplayInstruction> converterFilter = new ConverterFilter<ClientLogRecord, ReplayInstruction>() {
				@Override
				public boolean allowPostConvert(ReplayInstruction c) {
					return c != null;
				}

				@Override
				public boolean allowPreConvert(ClientLogRecord t) {
					return true;
				}

				@Override
				public ReplayInstruction convert(ClientLogRecord original) {
					return ReplayInstruction.fromClientLogRecord(original);
				}
			};
			List<ReplayInstruction> ris = CollectionFilters
					.convertAndFilter(clrs, converterFilter);
			return CommonUtils.join(ris, "\n");
		}

		private StringMap getDefJsFunctionLines(String fn) throws Exception {
			StringMap jsLines = new StringMap();
			String rgxfn = fn.replace("$", "\\$");
			Pattern modP = Pattern
					.compile(String.format("function %s\\(.+", rgxfn));
			for (File f : jsfDir.listFiles()) {
				String fileContents = ResourceUtilities.readFileToString(f);
				int indexOf = fileContents
						.indexOf(String.format("function %s(", fn));
				Matcher modM = modP.matcher(fileContents);
				if (modM.find()) {
					jsLines.put(f.getName(), modM.group());
				}
			}
			return jsLines;
		}

		private StringMap getDefJsLines(int lineNumber) throws Exception {
			StringMap jsLines = new StringMap();
			for (File f : jsfDir.listFiles()) {
				String[] lines = ResourceUtilities.readFileToString(f)
						.split("\n");
				if (lines.length > lineNumber) {
					jsLines.put(f.getName(), lines[lineNumber]);
				}
			}
			return jsLines;
		}

		private void importViaRsync(String from, File to) throws Exception {
			System.out.format("rsync %s -> %s\n", from, to.getPath());
			ProcessBuilder pb = new ProcessBuilder("/usr/bin/rsync", "-avz",
					"--progress", from, to.getPath());
			Process proc = pb.start();
			StreamBuffer errorGobbler = new StreamBuffer(proc.getErrorStream(),
					"ERROR");
			StreamBuffer outputGobbler = new StreamBuffer(proc.getInputStream(),
					"OUTPUT");
			errorGobbler.start();
			outputGobbler.start();
			proc.waitFor();
		}

		String run0(String[] argv) throws Exception {
			if (argv.length == 0) {
				printUsage();
				return null;
			}
			ILogRecord record = null;
			String id = argv[0];
			long recId = 0;
			if (id.matches("\\d+")) {
				recId = Long.parseLong(argv[0]);
				record = console.state.logRecordById(recId);
				if (record == null) {
					if (lastRecord != null && lastRecord.getId() == recId) {
						record = lastRecord;
					}
				}
				if (record == null) {
					List<ILogRecord> logRecords = new ArrayList<ILogRecord>();
					Connection conn = getConn();
					String sql = "select l.*,u.username "
							+ "from logging l left outer join users u on l.user_id=u.id "
							+ "where l.id=? ";
					PreparedStatement ps = conn.prepareStatement(sql);
					ps.setLong(1, recId);
					ResultSet rs = ps.executeQuery();
					CmdGetExceptionLogs subCmd = new CmdGetExceptionLogs();
					subCmd.setEnvironment(console);
					subCmd.addLogRecords(rs, logRecords);
					record = CommonUtils.first(logRecords);
					ps.close();
				}
			} else {
				record = Registry.impl(ILogRecord.class);
				record.setText(ResourceUtilities.readFileToString(id));
			}
			lastRecord = record;
			FilterArgvParam param = new FilterArgvParam(argv, "-o");
			argv = param.argv;
			String format = param.valueOrDefault("all");
			boolean all = format.equals("all");
			boolean replay = format.equals("replay");
			Pattern clientLogPattern = Pattern
					.compile("\nSession History:\n----\n(.+)", Pattern.DOTALL);
			Pattern clientLogPattern2 = Pattern.compile("(\\{\"cn\":.+)",
					Pattern.DOTALL);
			String text = record.getText();
			Matcher clientLogMatcher = clientLogPattern.matcher(text);
			if (clientLogMatcher.find()) {
				text = text.substring(0, clientLogMatcher.start());
			}
			text = text.replace("\n", "\n\t");
			System.out.format(
					"-------\nId:\t%s\nUser:\t%s (%s)\nCmp:\t%s\nDate:\t%s\nHost:\t%s\nText:\t%s\n",
					record.getId(), console.state.getUser(record.getUserId()),
					record.getUserId(), record.getComponentKey(),
					record.getCreatedOn(), record.getHost(),
					replay ? "" : text);
			Matcher uam = UA_PATTERN.matcher(record.getText());
			Pattern moduleNamePattern = Pattern
					.compile("Permutation name: (\\S+)");
			Matcher m = moduleNamePattern.matcher(record.getText());
			String recordText = record.getText();
			FilterArgvFlag f = new FilterArgvFlag(argv, "-js");
			argv = f.argv;
			getJs = f.contains;
			FilterArgvParam p = new FilterArgvParam(argv, "-fn");
			if (p.value != null) {
				argv = p.argv;
				fnOverride = p.value;
			}
			if (m.find()) {
				uam.find();
				String userAgentString = uam.group(1);
				UserAgent userAgent = UserAgent
						.parseUserAgentString(userAgentString);
				Browser browser = userAgent.getBrowser();
				String mn = m.group(1);
				if (!mn.equals("HostedMode")
						&& !text.contains("fillInStackTrace") && all
						&& ensureModuleAndSymbolMap(mn)) {
					deObfStacktrace(text, mn, browser);
				} else {
					if (all) {
						System.out.println("Unable to import module/symbol");
					}
				}
			}
			clientLogMatcher = clientLogPattern.matcher(record.getText());
			Matcher clientLogMatcher2 = clientLogPattern2
					.matcher(record.getText());
			if (!clientLogMatcher.find() && clientLogMatcher2.find()) {
				clientLogMatcher = clientLogMatcher2;
			}
			clientLogMatcher.reset();
			if (clientLogMatcher.find()) {
				if (all) {
					dumpHistory(clientLogMatcher.group(1));
				} else if (replay) {
					String replayStr = extractReplay(clientLogMatcher.group(1));
					System.out.format("\n\n%s\n\n", replayStr);
					console.setClipboardContents(replayStr);
				}
			}
			return String.format("Drilldown - %s", recId);
		}

		public static class DevConsoleDebugPaths {
			public String symPath;

			public String modPath;

			public String remoteRoot;
		}
	}

	public static class CmdGetExceptionLogs extends DevConsoleCommand {
		private static final int DEFAULT_GET_EXCEPTIONS_IN_LAST_X_DAYS = 2;

		String regexFilter = null;

		public void addLogRecords(ResultSet rs, List<ILogRecord> logRecords)
				throws SQLException {
			Optional<Pattern> ignorePattern = Optional.ofNullable(
					regexFilter != null ? Pattern.compile(regexFilter) : null);
			while (rs.next()) {
				IUser u = console.state.ensureUser(rs.getLong("user_id"),
						rs.getString("username"));
				ILogRecord l = Registry.impl(ILogRecord.class);
				l.setId(rs.getLong("id"));
				l.setComponentKey(rs.getString("component_key"));
				l.setUserId(rs.getLong("user_id"));
				l.setText(rs.getString("text"));
				l.setHost(rs.getString("host"));
				l.setCreatedOn(
						new Date(rs.getTimestamp("created_on").getTime()));
				if (ignorePattern.isPresent()
						&& ignorePattern.get().matcher(l.getText()).find()) {
				} else {
					logRecords.add(l);
				}
			}
		}

		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "dxg" };
		}

		@Override
		public String getDescription() {
			return "Download meaningful Jade exceptions for the last x days";
		}

		@Override
		public String getUsage() {
			return "dxg {days} <-a --all types> <-gtid id : log record id gt specified>";
		}

		@Override
		public String run(String[] argv) throws Exception {
			Connection conn = getConn();
			int days = getIntArg(argv, 0,
					DEFAULT_GET_EXCEPTIONS_IN_LAST_X_DAYS);
			String ckFilter = getExceptionIgnoreClause();
			FilterArgvFlag filterArgvResult = new FilterArgvFlag(argv, "-a");
			argv = filterArgvResult.argv;
			String exceptionFilter = filterArgvResult.contains ? ""
					: "and l.component_key ='CLIENT_EXCEPTION' ";
			filterArgvResult = new FilterArgvFlag(argv, "-ex");
			argv = filterArgvResult.argv;
			FilterArgvParam filterArgvParam = new FilterArgvParam(argv,
					"-ignore");
			regexFilter = filterArgvParam.value;
			argv = filterArgvParam.argv;
			List<String> exceptions = Arrays.asList(
					"UNEXPECTED_SERVLET_EXCEPTION", "ALERT_GENERATION_FAILURE",
					"JOB_FAILURE_EXCEPTION", "CLUSTER_EXCEPTION",
					"CLIENT_EXCEPTION", "RPC_EXCEPTION",
					"CLIENT_EXCEPTION_IE_CRUD", "PUBLICATION_EXCEPTION",
					"TRANSFORM_EXCEPTION", "PARSER_EXCEPTION");
			exceptionFilter = filterArgvResult.contains
					? String.format("and l.component_key in %s",
							EntityUtils.stringListToClause(exceptions))
					: exceptionFilter;
			filterArgvParam = new FilterArgvParam(argv, "-extypes");
			String customExceptionFilter = filterArgvParam.value;
			if (customExceptionFilter != null) {
				exceptions = Arrays.stream(customExceptionFilter.split(","))
						.collect(Collectors.toList());
				exceptionFilter = String.format("and l.component_key in %s",
						EntityUtils.stringListToClause(exceptions));
			}
			argv = filterArgvParam.argv;
			filterArgvParam = new FilterArgvParam(argv, "-gtid");
			argv = filterArgvParam.argv;
			String gtOnlyFilter = filterArgvParam.value == null ? ""
					: String.format("and l.id>=%s ", filterArgvParam.value);
			String sql = String.format("select l.*,u.username "
					+ "from logging l inner join users u on l.user_id=u.id "
					+ "where l.created_on>? %s %s and "
					+ " not (l.component_key in %s)" + " order by l.id ",
					exceptionFilter, gtOnlyFilter, ckFilter);
			PreparedStatement ps = conn.prepareStatement(sql);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -days);
			Date d = c.getTime();
			ps.setDate(1, new java.sql.Date(d.getTime()));
			ResultSet rs = ps.executeQuery();
			console.state.logRecords = new ArrayList<ILogRecord>();
			List<ILogRecord> logRecords = console.state.logRecords;
			addLogRecords(rs, logRecords);
			console.serializeState();
			return String.format("retrieved %s log records", logRecords.size());
		}

		protected String getExceptionIgnoreClause() {
			return getPeer().getExceptionIgnoreClause();
		}
	}

	@RegistryLocation(registryPoint = DevConsoleDebugPeer.class, implementationType = ImplementationType.INSTANCE)
	public static abstract class DevConsoleDebugPeer {
		public abstract List<ILogRecord> filterByComponent(
				List<ILogRecord> logRecords, String componentKey);

		public abstract String getExceptionIgnoreClause();

		public abstract DevConsoleDebugPaths
				getPaths(DevConsoleProperties props);
	}
}

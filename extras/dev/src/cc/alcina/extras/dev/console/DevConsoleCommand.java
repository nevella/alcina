package cc.alcina.extras.dev.console;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import cc.alcina.extras.dev.console.DevConsoleProperties.SetPropInfo;
import cc.alcina.extras.dev.console.DevConsoleStrings.DevConsoleString;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.KeyValueMapper.StringKeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.ReportUtils;
import cc.alcina.framework.entity.util.ShellUtils;

@RegistryLocation(registryPoint = DevConsoleCommand.class)
public abstract class DevConsoleCommand<C extends DevConsole> {
	protected C console;

	public Logger logger;

	public List<Long> failed;

	private Connection conn;

	private boolean cancelled;

	public void cancel() {
		cancelled = true;
	}
	public void checkCancelled() {
		if (cancelled) {
			throw new CancelledException("Action cancelled");
		}
	}

	public void format(String format, Object... args) {
		String out = String.format(format, args);
		System.out.print(out);
		commandOutputBuffer.append(out);
	}

	private StringBuilder commandOutputBuffer = new StringBuilder();

	public String dumpCommandOutputBuffer() {
		return commandOutputBuffer.toString();
	}

	protected void println(String string) {
		System.out.println(string);
		commandOutputBuffer.append(string);
		commandOutputBuffer.append("\n");
	};

	public static class FilterArgvResult {
		public boolean contains;

		public String[] argv;

		public FilterArgvResult(String[] argv, String flag) {
			List<String> strs = new ArrayList<String>(Arrays.asList(argv));
			this.contains = strs.remove(flag);
			this.argv = (String[]) strs.toArray(new String[strs.size()]);
		}
	}

	public boolean canUseProductionConn() {
		return false;
	}

	public abstract String[] getCommandIds();

	public abstract String getDescription();

	public abstract String getUsage();

	public abstract String run(String[] argv) throws Exception;

	public void setEnvironment(C console) {
		this.console = console;
		this.logger = console.logger;
	}

	public void configure() {
	}

	public Connection getConn() throws Exception {
		if (conn == null) {
			if (console.props.connection_useProduction
					&& !canUseProductionConn()) {
				throw new Exception(String.format("Cmd %s is local only",
						getClass().getSimpleName()));
			}
			Class.forName("org.postgresql.Driver");
			String connStr = console.props.connection_useProduction ? console.props.connection_production
					: console.props.connection_local;
			String[] parts = connStr.split(",");
			try {
				conn = DriverManager
						.getConnection(parts[0], parts[1], parts[2]);
			} catch (Exception e) {
				if (console.props.connection_useProduction
						&& !console.props.connectionProductionTunnelCmd
								.isEmpty()) {
					ShellUtils
							.runShell(console.props.connectionProductionTunnelCmd);
					for (int i = 1; i < 15; i++) {
						try {
							System.out.format("opening tunnel ... %s ...\n", i);
							conn = DriverManager.getConnection(parts[0],
									parts[1], parts[2]);
							return conn;
						} catch (Exception e1) {
							try {
								Thread.sleep(1000);
							} catch (Exception e2) {
								e2.printStackTrace();
							}
						}
					}
					conn = DriverManager.getConnection(parts[0], parts[1],
							parts[2]);
				} else {
					throw e;
				}
			}
		}
		return conn;
	}

	protected List<String> getIds() throws IOException {
		String idOrSet = console.props.idOrSet;
		try {
			long id = Long.parseLong(idOrSet);
			return Collections.singletonList(idOrSet);
		} catch (NumberFormatException e) {
			String idList = ResourceUtilities.readFileToString(String.format(
					"%s/%s.txt", console.setsFolder.getPath(),
					console.props.idOrSet));
			List<String> idlStr = new ArrayList<String>();
			List<Long> idListL = TransformManager.idListToLongs(idList);
			for (Long id : idListL) {
				idlStr.add(id.toString());
			}
			return idlStr;
		}
	}

	protected int getIntArg(String[] argv, int argIndex, int defaultValue) {
		return argv.length <= argIndex ? defaultValue : Integer
				.parseInt(argv[argIndex]);
	}
	protected String getStringArg(String[] argv, int argIndex, String defaultValue) {
		return argv.length <= argIndex ? defaultValue : argv[argIndex];
	}

	protected String getSitename(long id) throws Exception {
		Statement stmt = getConn().createStatement();
		ResultSet rs = stmt
				.executeQuery("select o.siteName from article a inner join overview o"
						+ " on a.overview_id=o.id where a.id=" + id);
		rs.next();
		String string = rs.getString(1);
		stmt.close();
		return string;
	}

	protected String runSubcommand(DevConsoleCommand sub, String[] argv)
			throws Exception {
		console.prepareCommand(sub);
		return sub.run(argv == null ? new String[0] : argv);
	}

	protected String simpleParserName(Object parser) {
		return parser.getClass().getSimpleName().replace("Article", "")
				.replace("Parser", "").replace("Marker", "");
	}

	public static class CmdBreak extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "brk" };
		}

		@Override
		public String getDescription() {
			return "Cancel all running jobs";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public String run(String[] argv) throws Exception {
			List<DevConsoleCommand> runningJobs = console.runningJobs;
			for (DevConsoleCommand cmd : runningJobs) {
				cmd.cancel();
			}
			return "cancel message sent";
		}
	}

	public static class CmdFind extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "f" };
		}

		@Override
		public String getDescription() {
			return "Find text in main console";
		}

		@Override
		public String getUsage() {
			return "f <text|last text>";
		}

		@Override
		public String run(String[] argv) throws Exception {
			console.find(argv.length == 0 ? null : argv[0]);
			return null;
		}

		@Override
		public boolean silent() {
			return true;
		}
	}

	public static class CmdClearBuffer extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "cls" };
		}

		@Override
		public String getDescription() {
			return "Clear the console screen and scroll buffer";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public String run(String[] argv) throws Exception {
			console.clear();
			return "";
		}
	}

	public static class CmdExecRunnable extends DevConsoleCommand {
		private DevConsoleRunnable r;

		@Override
		public String[] getCommandIds() {
			return new String[] { "x" };
		}

		@Override
		public String getDescription() {
			return "Run the specified task - invoke with no arguments to list available";
		}

		public String getUsage() {
			return "x {runnableClassname}";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return true;
		}
		
		@Override
		public boolean canUseProductionConn() {
			return r.canUseProductionConn();
		}
		@Override
		public String run(String[] argv) throws Exception {
			List<Class> classes = Registry.get().lookup(
					DevConsoleRunnable.class);
			String runnableName = argv.length == 0 ? "" : argv[0];
			for (Class clazz : classes) {
				if (clazz.getSimpleName().equals(runnableName)) {
					r = (DevConsoleRunnable) clazz
							.newInstance();
					r.console = console;
					r.command = this;
					r.value = argv.length == 1 ? null : argv[1];
					r.actionLogger = console.logger;
					r.argv=argv;
					try {
						LooseContext.pushWithKey(
								DevConsoleRunnable.CONTEXT_ACTION_RESULT, "");
						r.run();
						String msg = LooseContext
								.getString(DevConsoleRunnable.CONTEXT_ACTION_RESULT);
						msg = msg.isEmpty() ? msg : "\n\t" + msg;
						return String.format("'%s' was run%s", runnableName,
								msg);
					} finally {
						LooseContext.pop();
					}
				}
			}
			listRunnables(classes);
			return String.format("no runnable with classname '%s' found",
					runnableName);
		}

		static void listRunnables(List<Class> classes)
				throws InstantiationException, IllegalAccessException {
			SortedMap<String, Class> map = CollectionFilters.sortedMap(classes,
					new ClassSimpleNameMapper());
			System.out.format("%-45s%-20s\n", "Available runnables:", "Tags");
			for (Entry<String, Class> entry : map.entrySet()) {
				String[] tags = ((DevConsoleRunnable) entry.getValue()
						.newInstance()).tagStrings();
				System.out.format("%-45s%-20s\n", entry.getKey(), CommonUtils
						.join(tags, ", ").toLowerCase());
			}
		}
	}

	public static class CmdListRunnables extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "xl" };
		}

		@Override
		public String getDescription() {
			return "List runnables matching tag substrings";
		}

		public String getUsage() {
			return "xl {tag substring,...}";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return false;
		}

		@Override
		public String run(final String[] argv) throws Exception {
			List<Class> classes = Registry.get().lookup(
					DevConsoleRunnable.class);
			CollectionFilter<Class> filter = new CollectionFilter<Class>() {
				@Override
				public boolean allow(Class o) {
					try {
						String[] tags = ((DevConsoleRunnable) o.newInstance())
								.tagStrings();
						for (String tag : tags) {
							for (String arg : argv) {
								if (tag.toLowerCase().startsWith(
										arg.toLowerCase())) {
									return true;
								}
							}
						}
						return false;
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			};
			CmdExecRunnable.listRunnables(CollectionFilters.filter(classes,
					filter));
			return "";
		}
	}

	public static class CmdHelp extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "h" };
		}

		@Override
		public String getDescription() {
			return "show help (for specifc cmd if 'cmd' set)";
		}

		@Override
		public String getUsage() {
			return "h {cmd}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			List<String> keys = new ArrayList<String>(
					console.commandsById.keySet());
			Collections.sort(keys);
			System.out.format("%-20s%-50s%s\n", "Command", "Usage",
					"Description");
			System.out.println(CommonUtils.padStringLeft("", 100, "-"));
			Set<DevConsoleCommand> seen = new LinkedHashSet<DevConsoleCommand>();
			String descPad = "\n" + CommonUtils.padStringLeft("", 73, " ");
			for (String k : keys) {
				Map<String, DevConsoleCommand> commandsById = console.commandsById;
				DevConsoleCommand cmd2 = commandsById.get(k);
				if (seen.contains(cmd2)) {
					continue;
				}
				seen.add(cmd2);
				String desc = cmd2.getDescription();
				desc = desc.replace("\n", descPad);
				System.out.format("%-20s%-50s%s\n",
						CommonUtils.join(cmd2.getCommandIds(), ", "),
						cmd2.getUsage(), desc);
			}
			return null;
		}
	}

	public static class CmdQuit extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "q" };
		}

		@Override
		public String getDescription() {
			return "Quit the console";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public String run(String[] argv) throws Exception {
			System.exit(0);
			return "";
		}
	}

	public static class CmdReset extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "reset" };
		}

		@Override
		public String getDescription() {
			return "Reload metadata parser xml, reload citables cache and jade-server.properties";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public String run(String[] argv) throws Exception {
			console.resetObjects();
			return "Lookups reset";
		}
	}

	public static class CmdSetId extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "i" };
		}

		@Override
		public String getDescription() {
			return "set current working set - either a numeric id, or an idList name";
		}

		@Override
		public String getUsage() {
			return "i [id|idListName]";
		}

		@Override
		public String run(String[] argv) throws Exception {
			console.props.idOrSet = argv[0];
			console.saveConfig();
			return String.format("set id to '%s'", console.props.idOrSet);
		}
	}

	public static class CmdSearchHistory extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "sh" };
		}

		@Override
		public String getDescription() {
			return "search history for matching cmd(s)";
		}

		@Override
		public String getUsage() {
			return "sh [cmd substring]";
		}

		@Override
		public String run(String[] argv) throws Exception {
			List<String> matches = new ArrayList<String>(
					console.history.getMatches(argv[0]));
			Collections.reverse(matches);
			CollectionFilter<String> filter = new CollectionFilter<String>() {
				@Override
				public boolean allow(String o) {
					return !o.startsWith("sh ");
				}
			};
			CollectionFilters.filterInPlace(matches, filter);
			if (matches.size() > 1) {
				System.out.format("Matches:\n-------\n%s\n\n",
						CommonUtils.join(matches, "\n"));
			}
			if (matches.size() > 0) {
				console.setCommandLineText(CommonUtils.last(matches.iterator()));
			}
			return "";
		}
	}

	public static class CmdRsync extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "rsync" };
		}

		@Override
		public String getDescription() {
			return "get or put a file";
		}

		@Override
		public String getUsage() {
			return "rsync  [get|put] local remote";
		}

		private void importViaRsync(String arg1, String remotePort,
				String from, String to) throws Exception {
			String[] cmdAndArgs = new String[] { "/usr/bin/rsync", "-avz",
					"--progress", arg1, remotePort, from, to };
			ShellUtils.runProcessCatchOutputAndWait(cmdAndArgs);
		}

		@Override
		public String run(String[] argv) throws Exception {
			String homeDir = (System.getenv("USERPROFILE") != null) ? System
					.getenv("USERPROFILE") : System.getProperty("user.home");
			String localPath = SEUtilities.combinePaths(homeDir + "/", argv[1]);
			String remotePath = String.format("%s:%s", console.props.remoteSsh,
					SEUtilities.combinePaths(console.props.remoteHomeDir + "/",
							argv[2]));
			String remotePortStr = String.format(
					"/usr/bin/ssh -o StrictHostKeychecking=no -p %s",
					console.props.remoteSshPort);
			boolean put = argv[0].equals("put");
			String f1 = put ? localPath : remotePath;
			String f2 = put ? remotePath : localPath;
			importViaRsync("--rsh", remotePortStr, f1, f2);
			return String.format("%s -> %s", f1, f2);
		}
	}

	public static class CmdInterpolatePostgresParameterisedQuery extends
			DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "pgpi" };
		}

		@Override
		/**
		 * LOG:  execute S_1: BEGIN
		LOG:  execute <unnamed>: select distinct article2_.id as id521_, article2_.OPTLOCK as OPTLOCK521_, article2_.before_ as before3_521_, article2_.checkedBy_id as checkedBy23_521_, article2_.checkedOn as checkedOn521_, article2_.detachedFromAutomaticUpdate as detached5_521_, article2_.disabled as disabled521_, article2_.documentTitle as document7_521_, article2_.effectiveDate as effectiv8_521_, article2_.ignoreForAlerts as ignoreFo9_521_, article2_.incomingCitationCount as incomin10_521_, article2_.incomingCitationRank as incomin11_521_, article2_.listOfAuthorities as listOfA12_521_, article2_.mediumNeutralCitationSortKey as mediumN13_521_, article2_.mediumNeutralId as mediumN14_521_, article2_.modificationDate as modific15_521_, article2_.outgoingCitationCount as outgoin16_521_, article2_.outgoingCitationRank as outgoin17_521_, article2_.overview_id as overview18_521_, article2_.restrictedToGroup_id as restric24_521_, article2_.restrictedToUser_id as restric25_521_, article2_.retrievedOn as retriev19_521_, article2_.standardCitationSortKey as standar20_521_, article2_.structuralPages as structu21_521_, article2_.url as url521_ from documentoverlaynode documentov0_ inner join articledocumentoverlay articledoc1_ on documentov0_.ownerDocument_id=articledoc1_.id inner join public.article article2_ on articledoc1_.article_id=article2_.id inner join citation citation3_ on documentov0_.citation_id=citation3_.id where citation3_.id=$1 limit $2
		DETAIL:  parameters: $1 = '57051626', $2 = '2'
		 * @see DevConsoleCommand#getDescription()
		 */
		public String getDescription() {
			return "interpolate pg parameters into a query (see the command's source)";
		}

		@Override
		public String getUsage() {
			return "pgpi (will prompt for text, or copy from clipboard)";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String pg = console
					.getMultilineInput("Enter the pg text, or blank for clipboard: ");
			pg = pg.isEmpty() ? console.getClipboardContents() : pg;
			System.out.format("Inserting into query:\n%s\n\n",
					console.padLeft(pg, 1, 0));
			Pattern p1 = Pattern
					.compile("LOG:  execute <unnamed>: (.+)\nDETAIL:  parameters: (.+)");
			Pattern p3 = Pattern.compile("(\\$\\d+) = ('.+?')");
			Matcher m1 = p1.matcher(pg);
			m1.find();
			String query = m1.group(1);
			String params = m1.group(2);
			Matcher m3 = p3.matcher(params);
			StringMap pvs = new StringMap();
			while (m3.find()) {
				pvs.put(m3.group(1), m3.group(2));
			}
			ArrayList<String> keys = new ArrayList<String>(pvs.keySet());
			Collections.reverse(keys);
			for (String key : keys) {
				query = query.replace(key, pvs.get(key));
			}
			System.out.println(query);
			console.setClipboardContents(query);
			System.out.println("\n");
			return "";
		}
	}

	public static class CmdSetLogLevel extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "ll" };
		}

		@Override
		public String getDescription() {
			return "Set log level to debug/info";
		}

		@Override
		public String getUsage() {
			return "ll {d|i}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv[0].equals("d")) {
				console.props.logLevel = "DEBUG";
			} else if (argv[0].equals("i")) {
				console.props.logLevel = "INFO";
			} else {
				System.err.println("values: i=INFO, d=DEBUG");
				return "";
			}
			console.saveConfig();
			return String.format("log level set to %s", console.props.logLevel);
		}
	}

	public static class CmdSetProp extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "prop" };
		}

		@Override
		public String getDescription() {
			return "list all properties, or set name=value (value can be double-quoted)";
		}

		@Override
		public String getUsage() {
			return "prop {name value}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			Map<String, Field> fieldsByAnnName = CollectionFilters.sortedMap(
					Arrays.asList(console.props.getClass().getFields()),
					new FieldFilter());
			if (argv.length == 0) {
				System.out.println(ResourceUtilities
						.readFileToString(console.consolePropertiesFile));
				dumpProps(fieldsByAnnName);
				return "";
			}
			String key = argv[0];
			String fieldName = null;
			Object value = null;
			if (fieldsByAnnName.containsKey(key)) {
				Field field = fieldsByAnnName.get(key);
				SetPropInfo ann = field.getAnnotation(SetPropInfo.class);
				Class<?> type = field.getType();
				if (type == Boolean.class || type == boolean.class) {
					field.set(console.props, Boolean.valueOf(argv[1]));
				} else if (type == Integer.class || type == int.class) {
					field.set(console.props, Integer.parseInt(argv[1]));
				} else if (type == String.class) {
					field.set(console.props, argv.length == 1 ? null : argv[1]);
				}
				console.saveConfig();
				return String.format("set %s to '%s'", argv[0],
						argv.length == 1 ? null : argv[1]);
			} else {
				System.err.println("Property not found - valid names are:");
				dumpProps(fieldsByAnnName);
			}
			return "";
		}

		private void dumpProps(Map<String, Field> fieldsByAnnName)
				throws Exception {
			System.out.format("%-30s%-50s%s\n", "Property", "Value",
					"Description");
			System.out.println(CommonUtils.padStringLeft("", 100, "-"));
			String descPad = "\n" + CommonUtils.padStringLeft("", 83, " ");
			for (String key : fieldsByAnnName.keySet()) {
				Field field = fieldsByAnnName.get(key);
				SetPropInfo ann = field.getAnnotation(SetPropInfo.class);
				String desc = ann.description();
				desc = desc.replace("\n", descPad);
				System.out.format("%-30s%-50s%s\n", ann.key(),
						field.get(console.props), desc);
			}
		}

		private static class FieldFilter extends StringKeyValueMapper<Field>
				implements CollectionFilter<Field> {
			@Override
			public boolean allow(Field o) {
				return getKey(o) != null;
			}

			@Override
			public String getKey(Field o) {
				SetPropInfo ann = o.getAnnotation(SetPropInfo.class);
				if (ann != null) {
					return ann.key();
				}
				return null;
			}
		}
	}

	public static class CmdDevProfile extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "profile" };
		}

		@Override
		public String getDescription() {
			return "save/restore a named dev profile (serialized obj collection, props, state)";
		}

		@Override
		public String getUsage() {
			return "profile [load|save] name";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length < 2) {
				listProfiles();
				printUsage();
				return "";
			}
			boolean load = argv[0].equals("load");
			String name = argv[1];
			File profile = SEUtilities
					.getChildFile(console.profileFolder, name);
			File profileSer = SEUtilities.getChildFile(profile, "ser");
			File testFolder = console.devHelper.getTestFolder();
			if (load) {
				if (!profile.exists()) {
					System.err.format("Profile '%s' does not exist\n", name);
					listProfiles();
					return "";
				} else {
					SEUtilities.copyFile(profileSer, testFolder);
					SEUtilities.copyFile(SEUtilities.getChildFile(profile,
							console.consolePropertiesFile.getName()),
							console.consolePropertiesFile);
					SEUtilities.copyFile(SEUtilities.getChildFile(profile,
							console.consoleHistoryFile.getName()),
							console.consoleHistoryFile);
					console.loadConfig();
					runSubcommand(new CmdReset(), new String[0]);
				}
			} else {
				SEUtilities.copyFile(testFolder, profileSer);
				SEUtilities.copyFile(console.consolePropertiesFile, SEUtilities
						.getChildFile(profile,
								console.consolePropertiesFile.getName()));
				SEUtilities.copyFile(console.consoleHistoryFile, SEUtilities
						.getChildFile(profile,
								console.consoleHistoryFile.getName()));
			}
			return load ? String
					.format("Loaded config from profile '%s'", name) : String
					.format("Saved config to profile '%s'", name);
		}

		private void listProfiles() {
			File[] files = console.profileFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			});
			System.out.println("Profiles:");
			for (File file : files) {
				System.out.format("\t%s\n", file.getName());
			}
		}
	}

	public enum TestResultFolder {
		CURRENT, OK
	}

	public boolean rerunIfMostRecentOnRestart() {
		return false;
	}

	public void printUsage() {
		System.err.format("Usage: %s\n\n", getUsage());
	}

	public void cleanup() throws SQLException {
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}

	public static class CmdTags extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "t" };
		}

		@Override
		public String getDescription() {
			return "Tagged strings - add, use, tag";
		}

		@Override
		public String getUsage() {
			return "t a|d|l|lt name tags content";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length == 0) {
				printFullUsage();
				return "";
			}
			String cmd = argv[0];
			if (cmd.equals("a")) {
				String name = argv[1];
				List<String> tags = new ArrayList<String>(Arrays.asList(argv[2]
						.split(",")));
				String content = argv[3].replace("\\n", "\n");
				console.strings.remove(name);
				console.strings.add(name, tags, content);
			} else if (cmd.equals("d")) {
				String name = argv[1];
				console.strings.remove(name);
			} else if (cmd.equals("c")) {
				String name = argv[1];
				DevConsoleString cs = console.strings.get(name);
				if (cs != null) {
					console.setClipboardContents(cs.content);
					System.out.format("Copied to clipboard: %s\n\t%s\n",
							cs.name, cs.content);
				}
			} else if (cmd.equals("l")) {
				List<String> tags = new ArrayList<String>(
						Arrays.asList((argv.length < 2 ? "" : argv[1])
								.split(",")));
				List<DevConsoleString> list = console.strings.list(tags);
				LookupMapToMap<String> tableData = new LookupMapToMap<String>(2);
				int r = 0;
				for (DevConsoleString s : list) {
					tableData.put(r, 0, s.name);
					tableData.put(r, 1, CommonUtils.join(s.tags, ","));
					tableData.put(r, 2, s.content.replace("\n", "\\n"));
					r++;
				}
				List<String> columnNames = Arrays.asList(new String[] { "name",
						"tags", "content" });
				ReportUtils.dumpTable(tableData, columnNames);
			} else if (cmd.equals("lt")) {
				ArrayList<String> tags = new ArrayList<String>(
						console.strings.listTags());
				Collections.sort(tags);
				System.out.println(CommonUtils.join(tags, "\n"));
			} else {
				System.err.format("Unknown subcommand - %s\n", cmd);
			}
			return "";
		}

		private void printFullUsage() {
			System.out.println("a: add (name, tags, content)");
			System.out.println("c: copy to clipboard (name)");
			System.out.println("d: delete (name)");
			System.out.println("l: list matching (tags)");
			System.out.println("lt: list tags");
		}
	}

	public boolean silent() {
		return false;
	}
	protected int getIntArg(String[] argv, String argName, int defaultValue) {
		String stringArg = getStringArg(argv, argName, null);
		return stringArg==null?defaultValue:Integer.parseInt(stringArg);
	}
	protected String getStringArg(String[] argv, String argName, String defaultValue) {
		Pattern argMatcher=Pattern.compile(String.format("%s=(.+)",argName));
		for(String arg:argv){
			Matcher m = argMatcher.matcher(arg);
			if(m.matches()){
				return m.group(1);
			}
		}
		return defaultValue;
	}
}
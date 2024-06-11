package cc.alcina.extras.dev.console;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdHelp;
import cc.alcina.extras.dev.console.remote.server.DevConsoleRemote;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.log.AlcinaLogUtils.LogMuter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityBrowser;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Al.Context;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Diff;
import cc.alcina.framework.common.client.util.Diff.Change;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.console.ArgParser;
import cc.alcina.framework.entity.gwt.reflection.impl.JvmReflections;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.BackendTransformQueue;
import cc.alcina.framework.entity.stat.DevStats;
import cc.alcina.framework.entity.stat.StatCategory;
import cc.alcina.framework.entity.stat.StatCategory_Console;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole.InitJaxbServices;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole.InitLightweightServices;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitPostObjectServices;
import cc.alcina.framework.entity.util.AlcinaChildRunnable.AlcinaChildContextRunner;
import cc.alcina.framework.entity.util.BiPrintStream;
import cc.alcina.framework.entity.util.BiPrintStream.NullPrintStream;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.DelegateMapCreatorConcurrentNoNulls;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.Shell.Output;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.servlet.job.JobLogTimer;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.task.TaskPublish;
import cc.alcina.framework.servlet.util.transform.SerializationSignatureListener;

/*
 * Startup speed doc
 * @formatter:off
 *
 * domainstore	prepare-domainstore	initialise-descriptor
									mvcc
				cluster-tr-listener	mark

Command line opts:

- --no-http
- --no-exit
- --no-rerun
- --http-port=<port>
- (command string)
 * @formatter:on

FIXME - console - search for "FIXME - console" in markdown files

 */
@Registration.Singleton
public abstract class DevConsole implements ClipboardOwner {
	private static BiPrintStream out;

	private static BiPrintStream err;

	private static BiPrintStream devErr;

	private static BiPrintStream devOut;

	private static long startupTime;
	// has to happen early, otherwise can never redirect
	static {
		startupTime = System.currentTimeMillis();
		err = new BiPrintStream(new ByteArrayOutputStream());
		devErr = new BiPrintStream(new ByteArrayOutputStream());
		err.s1 = System.err;
		err.s2 = devErr;
		devErr.s1 = new NullPrintStream();
		devErr.s2 = new NullPrintStream();
		out = new BiPrintStream(new ByteArrayOutputStream());
		devOut = new BiPrintStream(new ByteArrayOutputStream());
		out.s1 = System.out;
		out.s2 = devOut;
		devOut.s1 = new NullPrintStream();
		devOut.s2 = new NullPrintStream();
		System.setErr(err);
		System.setOut(out);
		// headless
		System.setProperty("java.awt.headless", "true");
		System.setProperty("awt.toolkit", "sun.awt.HToolkit");
		File devConsoleRegistry = new File(
				"/g/alcina/extras/dev/bin/registry.properties");
		if (!devConsoleRegistry.exists()
				&& devConsoleRegistry.getParentFile().exists()) {
			try {
				devConsoleRegistry.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static final Color RED = new Color(210, 20, 20);

	static final Color GREEN = new Color(0, 174, 127);

	static final Color BLUE = new Color(0, 127, 174);

	static DevConsole instance;

	public static DevConsole getInstance() {
		return instance;
	}

	public static void stdSysOut() {
		System.setErr(err.s1);
		System.setOut(out.s1);
	}

	private boolean initialised;

	protected DevHelper devHelper;

	Map<String, DevConsoleCommand> commandsById = new HashMap<String, DevConsoleCommand>();

	protected DevConsoleProperties props;

	public DevConsoleHistory history;

	public DevConsoleStrings strings;

	private String lastCommand;

	boolean secondHelperInitted = false;

	protected DevConsoleState state;

	File consolePropertiesFile;

	File consoleHistoryFile;

	File consoleStringsFile;

	public File devFolder;

	public File setsFolder;

	File profileFolder;

	public boolean runningLastCommand;

	List<DevConsoleCommand> runningJobs = new ArrayList<DevConsoleCommand>();

	ByteArrayOutputStream recordOut;

	PrintStream oldS2;

	String outDumpFileName = null;

	private DevConsoleRemote remote;

	private boolean headless;

	Stack<Class<? extends DevConsoleCommand>> shells = new Stack<>();

	public ConsoleStatLogProvider logProvider;

	private DevConsoleStyle style = DevConsoleStyle.NORMAL;

	private Set<StatCategory> emitted = new LinkedHashSet<>();

	protected Logger logger;

	protected LaunchConfiguration launchConfiguration;

	CountDownLatch currentCommandLatch;

	LinkedList<DevConsoleRunnable> currentRunnables = new LinkedList<>();

	private boolean noHistory;

	public DevConsole(String[] args) {
		if (args.length == 0) {
			String propertyArgs = System.getProperty("DevConsole.args");
			if (Ax.notBlank(propertyArgs)) {
				args = propertyArgs.split(";");
			}
		}
		File consoleOutputFile = new File(
				Ax.format("/tmp/log/console/%s.log", getLogFilePrefix()));
		consoleOutputFile.delete();
		consoleOutputFile.getParentFile().mkdirs();
		String loggingPropertiesPath = null;
		try {
			consoleOutputFile.createNewFile();
			devOut.s2 = new PrintStream(new FileOutputStream(consoleOutputFile),
					true);
			devErr.s2 = new PrintStream(new FileOutputStream(consoleOutputFile),
					true);
			InputStream s1 = DevConsole.class
					.getResourceAsStream("logging.properties");
			// will be a bufferedinputstream wrapping a fileinputstream
			Field f1 = SEUtilities.getFieldByName(s1.getClass(), "in");
			f1.setAccessible(true);
			FileInputStream s2 = (FileInputStream) f1.get(s1);
			Field f2 = s2.getClass().getDeclaredField("path");
			f2.setAccessible(true);
			loggingPropertiesPath = (String) f2.get(s2);
		} catch (Exception e) {
			Ax.simpleExceptionOut(e);
		}
		System.setProperty("java.util.logging.config.file",
				"/g/alcina/extras/dev/src/cc/alcina/extras/dev/console/logging.properties");
		logger = LoggerFactory.getLogger(getClass());
		shells.push(DevConsoleCommand.class);
		launchConfiguration = new LaunchConfiguration(args);
		DevConsoleRunnable.console = this;
	}

	public void atEndOfDomainStoreLoad() {
		new StatCategory_Console.PostDomainStore().emit();
		new StatCategory_Console().emit();
		if (Configuration.is("logStartupMetrics")) {
			new DevStats().parse(logProvider).dump(true);
		}
		logger.info("\nDomain stores loaded - time from launch {} ms\n",
				System.currentTimeMillis() - startupTime);
		logProvider.startRemote();
		BackendTransformQueue.get().start();
		JobRegistry.get().init();
		JobLogTimer.get().init();
		AlcinaTopics.applicationRestart.add(v -> getInstance().restart());
		DomainStore.topicStoreLoadingComplete.signal();
	}

	public String breakAndPad(int tabCount, int width, String text,
			int padLeftCharCount) {
		StringBuilder sb = new StringBuilder();
		int idx0 = 0;
		for (int idx1 = width; idx1 < text.length(); idx1 += width) {
			for (; idx1 < text.length(); idx1 += 1) {
				char c = text.charAt(idx1);
				if (c == ' ' || c == ',') {
					sb.append(text.substring(idx0, idx1 + 1));
					sb.append("\n");
					idx0 = idx1 + 1;
					break;
				}
			}
		}
		sb.append(text.substring(idx0));
		return padLeft(sb.toString(), tabCount, padLeftCharCount);
	}

	public void clear() {
		remote.addClearEvent();
		if (lastCommand != null) {
			echoCommand(lastCommand);
		}
	}

	public void closePipeHtml() {
		try {
			String val = endRecordingSysout();
			val = val.replaceAll("(https?://\\S+)", "<a href='$1'>$1</a>");
			val = Ax.format("<pre>%s</pre>", val);
			Io.write().string(val).toPath(this.outDumpFileName);
			this.outDumpFileName = null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void createDevHelper();

	private <T> T deserializeProperties(T newInstance, File file)
			throws Exception {
		if (file.exists()) {
			try {
				return (T) new ObjectMapper().enableDefaultTyping()
						.readValue(file, newInstance.getClass());
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
		}
		return newInstance;
	}

	public void disablePathLinks(boolean disable) {
		Runnable r = () -> Configuration.properties.set(
				"MethodHandler_GET_RECORDS.disablePathLinks",
				String.valueOf(disable));
		if (disable) {
			r.run();
		} else {
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(500);
						r.run();
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}.start();
		}
	}

	public void doCommandHistoryDelta(int delta) {
		String cmd = history.getCommand(delta);
		if (!cmd.isEmpty()) {
			setCommandLineText(cmd);
		}
	}

	public String dumpTransforms() {
		System.out.println("\n\n");
		Set<DomainTransformEvent> transforms = devHelper.dumpTransforms();
		System.out.println("\n\n");
		setClipboardContents(transforms.toString());
		File dumpFile = getDevFile("dumpTransforms.txt");
		try {
			Io.write().string(transforms.toString()).toFile(dumpFile);
			Ax.out("Transforms dumped to:\n\t%s", dumpFile.getPath());
			return dumpFile.getPath();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void echoCommand(String commandString) {
		setStyle(DevConsoleStyle.COMMAND);
		Ax.out("\n>%s", commandString);
		setStyle(DevConsoleStyle.NORMAL);
	}

	public void emitIfFirst(StatCategory statCategory) {
		if (emitted.add(statCategory)) {
			statCategory.emit();
		}
	}

	public String endRecordingSysout() {
		out.s2.flush();
		String result = new String(recordOut.toByteArray());
		out.s2 = oldS2;
		return result;
	}

	public abstract void ensureDomainStore() throws Exception;

	protected boolean filterCommand(DevConsoleCommand command) {
		return true;
	}

	/**
	 * Get the String residing on the clipboard.
	 *
	 * @return any text found on the Clipboard; if none found, return an empty
	 *         String.
	 */
	public String getClipboardContents() {
		String result = "";
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit()
					.getSystemClipboard();
			// odd: the Object param of getContents is not currently used
			Transferable contents = clipboard.getContents(null);
			boolean hasTransferableText = (contents != null)
					&& contents.isDataFlavorSupported(DataFlavor.stringFlavor);
			if (hasTransferableText) {
				try {
					result = (String) contents
							.getTransferData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException ex) {
					// highly unlikely since we are using a standard DataFlavor
					System.out.println(ex);
					ex.printStackTrace();
				} catch (IOException ex) {
					System.out.println(ex);
					ex.printStackTrace();
				}
			}
		} catch (HeadlessException e) {
			if (isOsX()) {
				try {
					Output output = new Shell().noLogging().runShell("",
							"pbpaste");
					return output.output;
				} catch (Exception e2) {
					throw new WrappedRuntimeException(e2);
				}
			}
		}
		return result;
	}

	public File getDevFile(String path) {
		File xmlFile = new File(
				Ax.format("%s/%s.xml", devFolder.getPath(), path));
		File jsonFile = new File(
				Ax.format("%s/%s.json", devFolder.getPath(), path));
		if (xmlFile.exists()) {
			if (jsonFile.exists()) {
				xmlFile.delete();
			} else {
				xmlFile.renameTo(jsonFile);
			}
		}
		return jsonFile;
	}

	public DevHelper getDevHelper() {
		return devHelper;
	}

	protected String getLogFilePrefix() {
		return getClass().getSimpleName().toLowerCase();
	}

	public String getMultilineInput(String prompt) {
		return getMultilineInput(prompt, 10, 40);
	}

	public String getMultilineInput(String prompt, int rows, int cols) {
		return getClipboardContents();
	}

	public DevConsoleProperties getProps() {
		return props;
	}

	public DevConsoleState getState() {
		return state;
	}

	public DevConsoleStyle getStyle() {
		return style;
	}

	@SuppressWarnings("resource")
	protected void init() throws Exception {
		instance = this;
		Al.context = Context.console;
		Registry.Internals
				.setDelegateCreator(new DelegateMapCreatorConcurrentNoNulls());
		JvmReflections.configureBootstrapJvmServices();
		Registry.register().singleton(DevConsole.class, this);
		long statStartInit = System.currentTimeMillis();
		JvmReflections.init();
		Reflections.init();
		createDevHelper();
		devHelper.initRegistry();
		LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
		devHelper.doParallelEarlyClassInit();
		devHelper.copyTemplates();
		devHelper.loadConfig();
		devHelper.initLightweightServices();
		long statEndInitLightweightServices = System.currentTimeMillis();
		devHelper.getTestLogger();
		loadCommandMap();
		System.setProperty("awt.useSystemAAFontSettings", "gasp");
		System.setProperty("swing.aatext", "true");
		System.setProperty(
				"com.sun.xml.internal.bind.v2.runtime.JAXBContextImpl.fastBoot",
				"true");
		System.setProperty(
				"com.sun.xml.internal.bind.v2.bytecode.ClassTailor.noOptimize",
				"true");
		// need to be before ui init, cos window height is a preference
		initFiles();
		loadConfig();
		// initJaxb();
		// triggered by first publication
		long statEndInitJaxbServices = System.currentTimeMillis();
		initState();
		remote = DevConsoleRemote.get();
		remote.setOverridePort(launchConfiguration.httpPort);
		remote.setDevConsole(this);
		if (launchConfiguration.noHttpServer) {
			Ax.out("STARTUP\t no-http: not serving console over http");
			this.headless = true;
		} else {
			remote.start();
			this.headless = remote.isHasRemote();
			devOut.s1 = new PrintStream(
					new WriterOutputStream(remote.getOutWriter()));
			devErr.s1 = new PrintStream(
					new WriterOutputStream(remote.getErrWriter()));
		}
		if (!headless) {
			throw new UnsupportedOperationException();
		}
		clear();
		MetricLogging.get().setStart("init-console", statStartInit);
		MetricLogging.get().end("init-console");
		this.logProvider = new ConsoleStatLogProvider();
		new StatCategory_Console.Start().emit(startupTime);
		new InitLightweightServices().emit(statEndInitLightweightServices);
		new InitJaxbServices().emit(statEndInitJaxbServices);
		devHelper.initPostObjectServices();
		devHelper.initLifecycleServices();
		devHelper.initAppDebug();
		devHelper.initTopics();
		new InitPostObjectServices().emit(System.currentTimeMillis());
		new InitConsole().emit(System.currentTimeMillis());
		initialised = true;
		noHistory = launchConfiguration.noHistory;
		if (launchConfiguration.hasCommandString()) {
			performCommand(launchConfiguration.getCommandString());
		} else if (!props.lastCommand.matches("|q|re|restart")
				&& !launchConfiguration.noRerunLastCommand) {
			runningLastCommand = true;
			performCommand(props.lastCommand);
		} else {
			ok("Enter 'h' for help\n\n");
		}
		if (launchConfiguration.noHttpServer
				&& !launchConfiguration.exitAfterCommand) {
			startReadlineCommandLoop();
		}
	}

	public void initClassrefScanner() throws Exception {
		// ClassMetadataCache cache = new CachingClasspathScanner("*", true,
		// false,
		// Logger.getLogger(getClass()), Registry.MARKER_RESOURCE,
		// Arrays.asList(
		// new String[] { "WEB-INF/classes", "WEB-INF/lib" }))
		// .getClasses();
		// ClassrefScanner classrefScanner = new ClassrefScanner();
		// // if (!TransformCommit.isCommitTestTransforms()) {
		// classrefScanner.noPersistence();
		// // }
		// classrefScanner.scan(cache);
		ClassRef.add(
				Domain.stream(PersistentImpl.getImplementation(ClassRef.class))
						.collect(Collectors.toList()));
	}

	void initFiles() {
		devFolder = devHelper.getDevFolder();
		setsFolder = getDevFile("sets");
		setsFolder.mkdirs();
		profileFolder = getDevFile("profiles");
		profileFolder.mkdir();
		consolePropertiesFile = getDevFile("console-properties");
		consoleHistoryFile = getDevFile("console-history");
		consoleStringsFile = getDevFile("console-strings");
	}

	protected final void initState() {
		state = new DevConsoleState();
		try {
			state = getDevHelper().readObject(getState());
		} catch (Exception e) {
			DevConsole.stdSysOut();
			FileNotFoundException fnfe = CommonUtils.extractCauseOfClass(e,
					FileNotFoundException.class);
			if (fnfe == null) {
				e.printStackTrace();
			}
			serializeState();
		}
	}

	protected boolean isConsoleInstanceCommand(DevConsoleCommand c) {
		return false;
	}

	public boolean isHeadless() {
		return this.headless;
	}

	public boolean isInitialised() {
		return this.initialised;
	}

	private boolean isOsX() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("mac") >= 0;
	}

	public boolean isSingleCommand() {
		return launchConfiguration.noHttpServer
				&& launchConfiguration.exitAfterCommand;
	}

	private void loadCommandMap() {
		synchronized (commandsById) {
			commandsById.clear();
			try {
				List<Class<?>> list = Registry.query(DevConsoleCommand.class)
						.untypedRegistrations().collect(Collectors.toList());
				Registry.query(DevConsoleCommand.class).implementations()
						.filter(this::filterCommand).forEach(cmd -> {
							if (cmd.getShellClass() != shells.peek()) {
								return;
							}
							cmd.setEnvironment(this);
							for (String s : cmd.getCommandIds()) {
								commandsById.put(s, cmd);
							}
						});
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public void loadConfig() throws Exception {
		if (consolePropertiesFile.exists()) {
			try {
				props = deserializeProperties(newConsoleProperties(),
						consolePropertiesFile);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
		}
		if (props == null) {
			props = newConsoleProperties();
		}
		if (consoleHistoryFile.exists()) {
			try {
				history = deserializeProperties(new DevConsoleHistory(),
						consoleHistoryFile);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
		}
		if (history == null) {
			history = new DevConsoleHistory();
		}
		if (consoleStringsFile.exists()) {
			try {
				strings = deserializeProperties(new DevConsoleStrings(),
						consoleStringsFile);
			} catch (Exception e) {
				Ax.simpleExceptionOut(e);
			}
		}
		if (strings == null) {
			strings = new DevConsoleStrings();
		}
		saveConfig();
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	protected DevConsoleProperties newConsoleProperties() {
		return new DevConsoleProperties();
	}

	public void ok(String string) {
		setStyle(DevConsoleStyle.OK);
		Ax.out(string);
		setStyle(DevConsoleStyle.NORMAL);
	}

	protected void onAddDomainStore() {
		// EntityLayerLogging.setLevel(
		// AlcinaLogUtils.getMetricLogger(DomainStore.class), Level.WARN);
		DomainStore.stores().writableStore().getPersistenceEvents()
				.addDomainTransformPersistenceListener(
						new SerializationSignatureListener());
	}

	public String padLeft(String str, int tabCount, int charCount) {
		if (tabCount != 0) {
			String pad = CommonUtils.padStringLeft("", charCount, "\t");
			return pad + str.replace("\n", "\n" + pad);
		} else {
			String pad = "\n" + CommonUtils.padStringLeft("", charCount, " ");
			return str.replace("\n", pad);
		}
	}

	public void performCommand(String command) {
		if (command.isEmpty()) {
			command = this.lastCommand;
		}
		emitIfFirst(new StatCategory_Console.InitCommands.Start());
		this.lastCommand = command;
		StreamTokenizer tokenizer = new StreamTokenizer(
				new StringReader(command));
		tokenizer.ordinaryChars('0', '9');
		tokenizer.wordChars('0', '9');
		tokenizer.ordinaryChars('.', '.');
		tokenizer.wordChars('.', '.');
		tokenizer.ordinaryChars('-', '-');
		tokenizer.wordChars('-', '-');
		tokenizer.wordChars('/', '/');
		tokenizer.wordChars('_', '_');
		tokenizer.wordChars('=', '=');
		tokenizer.wordChars(':', ':');
		tokenizer.wordChars('@', '@');
		tokenizer.wordChars(',', ',');
		int token;
		try {
			String cmd = null;
			final List<String> args = new ArrayList<String>();
			while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
				switch (tokenizer.ttype) {
				case StreamTokenizer.TT_WORD:
					if (cmd == null) {
						cmd = tokenizer.sval;
					} else {
						args.add(tokenizer.sval);
					}
					break;
				case '\'':
					args.add(tokenizer.sval);
					break;
				default:
					if (cmd == null) {
						cmd = tokenizer.sval;
					} else {
						args.add(tokenizer.sval);
					}
					break;
				}
			}
			DevConsoleCommand template = null;
			synchronized (commandsById) {
				template = commandsById.get(cmd);
			}
			if (template == null) {
				Ax.err("'%s' is not a command\n", cmd);
				CmdHelp cmdHelp = new CmdHelp();
				cmdHelp.console = this;
				cmdHelp.run(new String[0]);
				return;
			}
			final DevConsoleCommand c = template.getClass()
					.getDeclaredConstructor().newInstance();
			prepareCommand(c);
			for (DevConsoleCommand c2 : runningJobs) {
				if (c2.getClass() == c.getClass()
						&& !c.isAllowParallelExecution()) {
					System.err.format("Command '%s' already running\n",
							c2.getClass().getSimpleName());
					return;
				}
			}
			LooseContextInstance snapshot = LooseContext.getContext()
					.snapshot();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					LooseContext.putSnapshotProperties(snapshot);
					performCommandInThread(args, c, true);
					currentCommandLatch.countDown();
				}
			};
			currentCommandLatch = new CountDownLatch(1);
			new AlcinaChildContextRunner(
					"dev-runner-" + c.getClass().getSimpleName())
							.callNewThread(runnable);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void performCommandInThread(List<String> args,
			DevConsoleCommand c, boolean topLevel) {
		try {
			LooseContext.push();
			PermissionsManager.get().pushUser(DevHelper.getDefaultUser(),
					LoginState.LOGGED_IN);
			runningJobs.add(c);
			if (!noHistory) {
				history.addCommand(lastCommand);
			}
			if (!c.silent()) {
				System.out.format("%s...\n", lastCommand);
			}
			long l1 = System.currentTimeMillis();
			c.configure();
			if (c.clsBeforeRun()) {
				clear();
			}
			String msg = c
					.run((String[]) args.toArray(new String[args.size()]));
			c.cleanup();
			long l2 = System.currentTimeMillis();
			if (msg != null) {
				ok(String.format("  %s - ok - %s ms\n", msg, l2 - l1));
			}
			if (topLevel && !c.ignoreForCommandHistory() && !noHistory) {
				String modCommand = c.rerunIfMostRecentOnRestart() ? lastCommand
						: "";
				if (!Objects.equals(modCommand, props.lastCommand)) {
					props.lastCommand = modCommand;
					serializeObject(props, consolePropertiesFile);
				}
				serializeObject(history, consoleHistoryFile);
			}
		} catch (Exception e) {
			if (!(e instanceof CancelledException)) {
				e.printStackTrace();
			}
		} finally {
			runningLastCommand = false;
			// txs just to allow propertychangelistener removal from user
			Transaction.ensureBegun();
			PermissionsManager.get().popUser();
			Transaction.end();
			LooseContext.pop();
			runningJobs.remove(c);
			if (launchConfiguration.exitAfterCommand) {
				// delay to ensure props etc written?
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		}
	}

	public void pipeOutput(String outDumpFileName) {
		pipeOutput(outDumpFileName, true);
	}

	public void pipeOutput(String outDumpFileName, boolean mute) {
		if (outDumpFileName != null) {
			startRecordingSysout(mute);
			this.outDumpFileName = outDumpFileName;
		} else {
			try {
				Io.write().string(endRecordingSysout())
						.toPath(this.outDumpFileName);
				this.outDumpFileName = null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public void popSubshell() {
		shells.pop();
		loadCommandMap();
	}

	public void prepareCommand(DevConsoleCommand cmd) {
		cmd.setEnvironment(this);
	}

	public void printDiff(boolean ignoreEqualLength, boolean ignoreInsertions,
			boolean ignoreLatterSubstring,
			boolean ignoreWhitespaceAndPunctuation, File f1, String s1, File f2,
			String s2) {
		String[] split1 = splitFile(s1);
		String[] split2 = splitFile(s2);
		Diff diff = new Diff(split1, split2);
		Change change = diff.diff(Diff.forwardScript);
		while (change != null) {
			if (ignoreEqualLength && change.deleted == change.inserted) {
			} else if (ignoreInsertions && change.deleted <= change.inserted) {
			} else {
				boolean ignore = false;
				if (ignoreLatterSubstring) {
					if (change.deleted == change.inserted) {
						ignore = true;
						for (int i = 0; i < change.deleted; i++) {
							if (split1[change.line0 + i]
									.contains(split2[change.line0 + i])) {
							} else {
								ignore = false;
								break;
							}
						}
					}
				}
				if (ignoreWhitespaceAndPunctuation && change.deleted == 1
						&& change.inserted == 1) {
					if (Ax.ntrim(split1[change.line0])
							.replaceAll("[ ,.~:-]", "")
							.equals(Ax.ntrim(split2[change.line1])
									.replaceAll("[ ,.~:-]", ""))) {
						ignore = true;
					}
				}
				if (!ignore) {
					System.out.format("(%s, %s): -%s, +%s\n", change.line0 + 1,
							change.line1 + 1, change.deleted, change.inserted);
					for (int i = 0; i < change.deleted; i++) {
						System.out.format("\t---%-8s: %s\n",
								change.line0 + i + 1, CommonUtils.hangingIndent(
										split1[change.line0 + i], true, 2));
					}
					for (int i = 0; i < change.inserted; i++) {
						System.out.format("\t+++%-8s: %s\n",
								change.line1 + i + 1, CommonUtils.hangingIndent(
										split2[change.line1 + i], true, 2));
					}
				}
			}
			change = change.link;
		}
		System.out.println();
		System.out.format("\nopendiff \"%s\" \"%s\"\n\n", f1.getPath(),
				f2.getPath());
	}

	public void printDiff(boolean ignoreEqualLength, boolean ignoreInsertions,
			boolean ignoreLatterSubstring, File f1, String s1, File f2,
			String s2) {
		printDiff(ignoreEqualLength, ignoreInsertions, ignoreLatterSubstring,
				false, f1, s1, f2, s2);
	}

	public void publish(ContentRequestBase request) {
		TaskPublish task = new TaskPublish();
		task.setPublicationRequest(request);
		Job job = task.perform();
		job.domain().ensurePopulated();
		Ax.out(job.getTaskSerialized());
	}

	public void pushSubshell(Class<? extends DevConsoleCommand> clazz) {
		shells.push(clazz);
		loadCommandMap();
	}

	public void restart() {
		String command = props.restartCommand;
		command = "echo \"{ \\\"command\\\": \\\"workbench.action.debug.restart\\\" }\" | websocat ws://127.0.0.1:3710";
		if (Ax.isBlank(command)) {
			Ax.err("Property 'restartCommand' not set");
		} else {
			try {
				new Shell().runBashScript(command).throwOnException();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public void saveConfig() throws Exception {
		serializeObject(props, consolePropertiesFile);
		serializeObject(history, consoleHistoryFile);
		serializeObject(strings, consoleStringsFile);
	}

	private void serializeObject(Object object, File file) {
		new Thread(Ax.format("console-serialize-%s", file.getName())) {
			@Override
			public void run() {
				try {
					new ObjectMapper().enableDefaultTyping()
							.writerWithDefaultPrettyPrinter()
							.writeValue(file, object);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}.start();
	}

	public void serializeState() {
		devHelper.writeObject(state);
	}

	/**
	 * Place a String on the clipboard, and make this class the owner of the
	 * Clipboard's contents.
	 */
	public void setClipboardContents(String aString) {
		// try {
		// StringSelection stringSelection = new StringSelection(aString);
		// Clipboard clipboard = Toolkit.getDefaultToolkit()
		// .getSystemClipboard();
		// clipboard.setContents(stringSelection, this);
		// } catch (HeadlessException e) {
		if (isOsX()) {
			try {
				String path = "/tmp/pbcopy.txt";
				Io.write().string(aString).toPath(path);
				new Shell().runBashScript(Ax.format("pbcopy < %s", path));
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		}
		// }
	}

	public void setCommandLineText(String text) {
		remote.addSetCommandLineEvent(text);
	}

	public void setConsoleOuputMuted(boolean muted) {
		out.setMuted(muted);
		err.setMuted(muted);
	}

	public void setNextCommand(String cmd) {
		props.lastCommand = cmd;
		try {
			saveConfig();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void setStyle(DevConsoleStyle style) {
		this.style = style;
	}

	protected String[] splitFile(String str) {
		return str.split("\n");
	}

	private void startReadlineCommandLoop() {
		Console c = System.console();
		if (c == null) {
			System.err.println("No console.");
			System.exit(1);
		}
		while (true) {
			String command = c.readLine("%s>",
					Ax.cssify(getClass().getSimpleName()));
			performCommand(command);
			try {
				currentCommandLatch.await();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	public void startRecordingSysout(boolean mute) {
		oldS2 = out.s2;
		PrintStream s2 = out.s2;
		recordOut = new ByteArrayOutputStream();
		PrintStream outStream = new PrintStream(recordOut);
		if (mute) {
			out.s2 = outStream;
			ByteArrayOutputStream nullOut = new ByteArrayOutputStream();
			out.s1 = new PrintStream(nullOut);
		} else {
			BiPrintStream s2repl = new BiPrintStream(
					new ByteArrayOutputStream());
			s2repl.s1 = s2;
			s2repl.s2 = outStream;
			out.s2 = s2repl;
		}
	}

	public enum DevConsoleStyle {
		NORMAL, OK, ERR, COMMAND
	}

	static class LaunchConfiguration {
		boolean noHttpServer;

		boolean noRerunLastCommand;

		private ArgParser parser;

		boolean exitAfterCommand;

		private Integer httpPort;

		boolean noHistory;

		public LaunchConfiguration(String[] argv) {
			parser = new ArgParser(argv);
			noHttpServer = parser.hasAndRemove("--no-http");
			noRerunLastCommand = parser.hasAndRemove("--no-rerun");
			this.httpPort = parser.getAndRemove("--http-port=").intValue()
					.orElse(null);
			exitAfterCommand = !parser.hasAndRemove("--no-exit")
					&& hasCommandString() && httpPort == null;
			noHistory = parser.hasAndRemove("--no-history");
			if (exitAfterCommand) {
				noHttpServer = true;
			}
		}

		String getCommandString() {
			return parser.asCommandString();
		}

		public boolean hasCommandString() {
			return parser.asCommandString().length() > 0;
		}
	}

	@Registration.Singleton(
		value = LogMuter.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class LogMuter_DevConsole extends LogMuter {
		@Override
		public void muteAllLogging(boolean muteAll) {
			instance.setConsoleOuputMuted(muteAll);
		}
	}

	@Registration.Singleton(
		value = EntityBrowser.class,
		priority = Registration.Priority.PREFERRED_LIBRARY)
	public static class EntityBrowser_DevConsole implements EntityBrowser {
		@Override
		public void browse(Entity entity) {
			try {
				int port = Configuration.getInt(DevConsoleRemote.class, "port");
				String strUrl = Ax.format(
						"http://127.0.0.1:%s/entity#traversal/layers.index=1:layers.sort-selected-first.present=true:layers-1.index=2:layers-1.sort-selected-first.present=true:layers-2.index=3:layers-2.sort-selected-first.present=true:paths.segmentPath=domain.%s.%s",
						port, Domain.resolveEntityClass(entity.getClass())
								.getSimpleName().toLowerCase(),
						entity.getId());
				Shell.exec("open %s", strUrl);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}
}

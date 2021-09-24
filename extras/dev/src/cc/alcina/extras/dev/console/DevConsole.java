package cc.alcina.extras.dev.console;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdHelp;
import cc.alcina.extras.dev.console.DevHelper.ConsolePrompter;
import cc.alcina.extras.dev.console.DevHelper.StringPrompter;
import cc.alcina.extras.dev.console.remote.server.DevConsoleRemote;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Diff;
import cc.alcina.framework.common.client.util.Diff.Change;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.WrappedObject;
import cc.alcina.framework.entity.persistence.WrappedObject.WrappedObjectHelper;
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
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.entity.util.AlcinaChildRunnable.AlcinaChildContextRunner;
import cc.alcina.framework.entity.util.BiPrintStream;
import cc.alcina.framework.entity.util.BiPrintStream.NullPrintStream;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.DelegateMapCreatorConcurrentNoNulls;
import cc.alcina.framework.entity.util.ShellWrapper;
import cc.alcina.framework.entity.util.ShellWrapper.ShellOutputTuple;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.util.transform.SerializationSignatureListener;

/*
 * Startup speed doc
 * @formatter:off
 * 
 * domainstore	prepare-domainstore	initialise-descriptor
									mvcc
				cluster-tr-listener	mark
 * @formatter:on
 */
@RegistryLocation(registryPoint = DevConsole.class, implementationType = ImplementationType.SINGLETON)
public abstract class DevConsole<P extends DevConsoleProperties, D extends DevHelper, S extends DevConsoleState>
		implements ClipboardOwner {
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

	public D devHelper;

	Map<String, DevConsoleCommand> commandsById = new HashMap<String, DevConsoleCommand>();

	public P props;

	public DevConsoleHistory history;

	public DevConsoleStrings strings;

	private String lastCommand;

	boolean secondHelperInitted = false;

	public S state;

	File consolePropertiesFile;

	File consoleHistoryFile;

	File consoleStringsFile;

	public File devFolder;

	public File setsFolder;

	File profileFolder;

	public boolean runningLastCommand;

	List<DevConsoleCommand> runningJobs = new ArrayList<DevConsoleCommand>();

	public Logger logger;

	ByteArrayOutputStream recordOut;

	PrintStream oldS2;

	String outDumpFileName = null;

	private DevConsoleRemote remote;

	private boolean headless;

	Stack<Class<? extends DevConsoleCommand>> shells = new Stack<>();

	public ConsoleStatLogProvider logProvider;

	private DevConsoleStyle style = DevConsoleStyle.NORMAL;

	private Set<StatCategory> emitted = new LinkedHashSet<>();

	public DevConsole() {
		shells.push(DevConsoleCommand.class);
		DevConsoleRunnable.console = this;
	}

	public void atEndOfDomainStoreLoad() {
		new StatCategory_Console.PostDomainStore().emit();
		new StatCategory_Console().emit();
		new DevStats().parse(logProvider).dump(true);
		logProvider.startRemote();
		JobRegistry.get();
		AlcinaTopics.applicationRestart.add((k, v) -> getInstance().restart());
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
			ResourceUtilities.writeStringToFile(val, this.outDumpFileName);
			this.outDumpFileName = null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void disablePathLinks(boolean disable) {
		Runnable r = () -> ResourceUtilities.registerCustomProperty(
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
			ResourceUtilities.writeStringToFile(transforms.toString(),
					dumpFile);
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
					ShellOutputTuple outputTuple = new ShellWrapper()
							.noLogging().runShell("", "pbpaste");
					return outputTuple.output;
				} catch (Exception e2) {
					throw new WrappedRuntimeException(e2);
				}
			}
		}
		return result;
	}

	public File getDevFile(String path) {
		return new File(String.format("%s/%s", devFolder.getPath(), path));
	}

	public String getMultilineInput(String prompt) {
		return getMultilineInput(prompt, 10, 40);
	}

	public String getMultilineInput(String prompt, int rows, int cols) {
		return getClipboardContents();
	}

	public DevConsoleStyle getStyle() {
		return style;
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

	public boolean isHeadless() {
		return this.headless;
	}

	public void loadConfig() throws Exception {
		// eclipse may be caching - read directly
		if (consolePropertiesFile.exists()) {
			props = (P) deserializeProperties(newConsoleProperties().getClass(),
					consolePropertiesFile);
		} else {
			props = newConsoleProperties();
		}
		if (consoleHistoryFile.exists()) {
			history = deserializeProperties(DevConsoleHistory.class,
					consoleHistoryFile);
		} else {
			history = new DevConsoleHistory();
		}
		if (consoleStringsFile.exists()) {
			strings = deserializeProperties(DevConsoleStrings.class,
					consoleStringsFile);
		} else {
			strings = new DevConsoleStrings();
		}
		saveConfig();
		if (props.useMountSshfsFs) {
			devHelper.useMountSshfsFs();
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	public void ok(String string) {
		setStyle(DevConsoleStyle.OK);
		Ax.out(string);
		setStyle(DevConsoleStyle.NORMAL);
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
			final DevConsoleCommand c = template.getClass().newInstance();
			prepareCommand(c);
			for (DevConsoleCommand c2 : runningJobs) {
				if (c2.getClass() == c.getClass()) {
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
				}
			};
			new AlcinaChildContextRunner(
					"dev-runner-" + c.getClass().getSimpleName())
							.callNewThread(runnable);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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
				ResourceUtilities.writeStringToFile(endRecordingSysout(),
						this.outDumpFileName);
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

	public void pushSubshell(Class<? extends DevConsoleCommand> clazz) {
		shells.push(clazz);
		loadCommandMap();
	}

	public void restart() {
		String command = props.restartCommand;
		if (Ax.isBlank(command)) {
			Ax.err("Property 'restartCommand' not set");
		} else {
			try {
				new ShellWrapper().runBashScript(command).throwOnException();
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
				ResourceUtilities.write(aString, path);
				new ShellWrapper()
						.runBashScript(Ax.format("pbcopy < %s", path));
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

	private <T> T deserializeProperties(Class<T> clazz, File file)
			throws Exception {
		try {
			return new ObjectMapper().enableDefaultTyping().readValue(file,
					clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			return KryoUtils.deserializeFromFile(file, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return WrappedObjectHelper.xmlDeserialize(clazz,
				ResourceUtilities.readFileToString(file));
	}

	private boolean isOsX() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("mac") >= 0;
	}

	private void loadCommandMap() {
		synchronized (commandsById) {
			commandsById.clear();
			try {
				List<Class> lookup = Registry.get()
						.lookup(DevConsoleCommand.class);
				filterLookup(lookup);
				for (Class clazz : lookup) {
					DevConsoleCommand cmd = (DevConsoleCommand) clazz
							.newInstance();
					if (cmd.getShellClass() != shells.peek()) {
						continue;
					}
					cmd.setEnvironment(this);
					for (String s : cmd.getCommandIds()) {
						commandsById.put(s, cmd);
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
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
			};
		}.start();
	}

	protected abstract void createDevHelper();

	protected void filterLookup(List<Class> lookup) {
	}

	protected List<Class> getInitClasses() {
		return new ArrayList<>(Arrays.asList(DevConsoleProperties.class,
				DevConsoleStrings.class, DevConsoleHistory.class));
	}

	protected void init() throws Exception {
		instance = this;
		Registry.setDelegateCreator(new DelegateMapCreatorConcurrentNoNulls());
		Registry.registerSingleton(DevConsole.class, this);
		long statStartInit = System.currentTimeMillis();
		createDevHelper();
		LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
		devHelper.doParallelEarlyClassInit();
		devHelper.loadJbossConfig(new ConsolePrompter());
		devHelper.initLightweightServices();
		long statEndInitLightweightServices = System.currentTimeMillis();
		logger = devHelper.getTestLogger();
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
		LooseContext.runWithKeyValue(WrappedObject.CONTEXT_CLASSES,
				getInitClasses(), () -> {
					loadConfig();
					return null;
				});
		// initJaxb();
		// triggered by first publication
		long statEndInitJaxbServices = System.currentTimeMillis();
		initState();
		devHelper.loadJbossConfig(null);
		boolean waitForUi = !devHelper.configLoaded;
		remote = new DevConsoleRemote(this);
		remote.start(devHelper.configLoaded);
		this.headless = remote.isHasRemote();
		if (headless) {
			// -Djava.awt.headless=true
			// -Dawt.toolkit=sun.awt.HToolkit
			System.setProperty("java.awt.headless", "true");
			System.setProperty("awt.toolkit", "sun.awt.HToolkit");
		}
		devOut.s1 = new PrintStream(
				new WriterOutputStream(remote.getOutWriter()));
		devErr.s1 = new PrintStream(
				new WriterOutputStream(remote.getErrWriter()));
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
		// FIXME - to consort
		BackendTransformQueue.get().start();
		new InitPostObjectServices().emit(System.currentTimeMillis());
		new InitConsole().emit(System.currentTimeMillis());
		if (!props.lastCommand.matches("|q|re|restart")) {
			runningLastCommand = true;
			performCommand(props.lastCommand);
		} else {
			ok("Enter 'h' for help\n\n");
		}
	}

	protected abstract void initState();

	protected boolean isConsoleInstanceCommand(DevConsoleCommand c) {
		return false;
	}

	protected abstract P newConsoleProperties();

	protected void onAddDomainStore() {
		// EntityLayerLogging.setLevel(
		// AlcinaLogUtils.getMetricLogger(DomainStore.class), Level.WARN);
		DomainStore.stores().writableStore().getPersistenceEvents()
				.addDomainTransformPersistenceListener(
						new SerializationSignatureListener());
	}

	protected void performCommandInThread(List<String> args,
			DevConsoleCommand c, boolean topLevel) {
		try {
			LooseContext.push();
			PermissionsManager.get().pushUser(DevHelper.getDefaultUser(),
					LoginState.LOGGED_IN);
			
			runningJobs.add(c);
			history.addCommand(lastCommand);
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
			if (topLevel && !c.ignoreForCommandHistory()) {
				String modCommand = c.rerunIfMostRecentOnRestart() ? lastCommand
						: "";
				if (!Objects.equals(modCommand, props.lastCommand)) {
					props.lastCommand = modCommand;
					serializeObject(props, consolePropertiesFile);
				}
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
		}
	}

	protected String[] splitFile(String str) {
		return str.split("\n");
	}

	void initFiles() {
		devFolder = devHelper.getDevFolder();
		setsFolder = getDevFile("sets");
		setsFolder.mkdirs();
		profileFolder = getDevFile("profiles");
		profileFolder.mkdir();
		consolePropertiesFile = getDevFile("console-properties.xml");
		consoleHistoryFile = getDevFile("console-history.xml");
		consoleStringsFile = getDevFile("console-strings.xml");
	}

	public enum DevConsoleStyle {
		NORMAL, OK, ERR, COMMAND
	}

	public static class JConsole extends JTextPane {
		public static final int maxChars = 250000;

		public boolean scrollToTopAtEnd;

		private SimpleAttributeSet current = null;

		private SimpleAttributeSet[] attrs;

		private SimpleAttributeSet highlightRange;

		private SimpleAttributeSet normalRange;

		// @Override
		// public boolean getScrollableTracksViewportWidth() {
		// return false;
		// }
		//
		// public Dimension getPreferredSize() {
		// Dimension dim = super.getPreferredSize();
		// return new Dimension(Integer.MAX_VALUE, dim.height);
		// };
		//
		// public Dimension getMinimumSize() {
		// Dimension dim = super.getMinimumSize();
		// return new Dimension(Integer.MAX_VALUE, dim.height);
		// };
		Runnable appendRunnable = null;

		List<Runnable> runnableBuffer = new ArrayList();

		private Timer commitThread = new Timer();
		{
			commitThread.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					commit();
				}
			}, 0, 20);
		}

		String lastText = null;

		private int docIndex;

		private Element lastHighlight;

		private DevConsoleStyle currentStyle;

		public JConsole() {
			setCaretPosition(0);
			setMargin(new Insets(5, 5, 5, 5));
			initAttrs("Courier New");
			setStyle(DevConsoleStyle.NORMAL);
			setEditable(false);
			TabStop[] tabs = new TabStop[8];
			for (int i = 0; i < 8; i++) {
				tabs[i] = new TabStop((i + 1) * 40, TabStop.ALIGN_LEFT,
						TabStop.LEAD_NONE);
			}
			TabSet tabset = new TabSet(tabs);
			StyleContext sc = StyleContext.getDefaultStyleContext();
			AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
					StyleConstants.TabSet, tabset);
			setParagraphAttributes(aset, false);
		}

		public void append(final String str) {
			StyledDocument doc = getStyledDocument();
			try {
				if (doc.getLength() > maxChars) {
					doc.remove(0, doc.getLength());
					doc.insertString(doc.getLength(), "...truncated...\n",
							current);
				}
				doc.insertString(doc.getLength(), str, current);
				docIndex = 0;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public synchronized void commit() {
			if (runnableBuffer.isEmpty()) {
				return;
			}
			SwingUtilities.invokeLater(new RunnableGroup());
		}

		public void err(final String msg) {
			err.append(msg);
		}

		public void find(final String text) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					find0(text);
				}
			});
		}

		public void initAttrs(String fontName) {
			attrs = new SimpleAttributeSet[4];
			for (int i = 0; i < 4; i++) {
				attrs[i] = new SimpleAttributeSet();
				StyleConstants.setFontFamily(attrs[i], fontName);
				StyleConstants.setFontSize(attrs[i], 15);
				if (i != 0) {
					StyleConstants.setBold(attrs[i], true);
				}
			}
			StyleConstants.setForeground(attrs[1], GREEN);
			StyleConstants.setForeground(attrs[2], RED);
			StyleConstants.setForeground(attrs[3], BLUE);
			highlightRange = new SimpleAttributeSet();
			normalRange = new SimpleAttributeSet();
			StyleConstants.setBackground(highlightRange,
					new Color(190, 210, 250));
			// StyleConstants.setForeground(highlightRange, Color.PINK);
			StyleConstants.setBackground(normalRange, Color.WHITE);
		}

		public void ok(final String msg) {
			synchronized (this) {
				invoke(() -> setStyle(DevConsoleStyle.OK));
				invoke(() -> out.append(msg));
				invoke(() -> setStyle(DevConsoleStyle.NORMAL));
				invokeScrollRunnable();
			}
		}

		public void setStyle(DevConsoleStyle style) {
			this.currentStyle = style;
			switch (style) {
			case OK:
				current = attrs[1];
				break;
			case ERR:
				current = attrs[2];
				break;
			case COMMAND:
				current = attrs[3];
				break;
			case NORMAL:
				current = attrs[0];
			}
		}

		private void invokeScrollRunnable() {
			Runnable scrollRunnable = new Runnable() {
				@Override
				public void run() {
					maybeScroll(this, true);
				}
			};
			invoke(scrollRunnable);
		}

		protected void find0(String text) {
			if (text == null) {
				text = CommonUtils.nullToEmpty(lastText);
			}
			if (!text.equals(lastText)) {
				docIndex = 0;
			}
			StyledDocument doc = getStyledDocument();
			try {
				int idx = doc.getText(0, doc.getLength()).indexOf(text,
						docIndex);
				if (idx == -1) {
					System.out.println("not found");
				} else {
					if (lastHighlight != null) {
						doc.setCharacterAttributes(idx, text.length(),
								normalRange, false);
					}
					Rectangle rect = modelToView(idx);
					lastHighlight = doc.getCharacterElement(idx);
					doc.setCharacterAttributes(idx, text.length(),
							highlightRange, false);
					scrollRectToVisible(rect);
					docIndex = idx + 1;
				}
				lastText = text;
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}

		protected synchronized void invoke(Runnable runnable) {
			runnableBuffer.add(
					AlcinaChildRunnable.wrapWithCurrentThreadContext(runnable));
		}

		protected synchronized void maybeScroll(Runnable runnable,
				boolean atEnd) {
			ScrollRunnable scrollRunnable = new ScrollRunnable();
			if (atEnd) {
				if (scrollToTopAtEnd) {
					scrollRunnable.toTop = true;
					scrollToTopAtEnd = false;
				}
			}
			runnableBuffer.add(scrollRunnable);
			for (Runnable bufferedRunnable : runnableBuffer) {
				if (bufferedRunnable instanceof ScrollRunnable
						&& scrollRunnable != bufferedRunnable) {
					((ScrollRunnable) bufferedRunnable).cancelled = true;
				}
			}
		}

		private final class ScrollRunnable implements Runnable {
			boolean cancelled = false;

			boolean toTop = false;

			@Override
			public void run() {
				if (cancelled) {
					return;
				}
				JViewport pane = (JViewport) getParent().getParent();
				pane.scrollRectToVisible(new Rectangle(0,
						toTop ? -getHeight() + 12 : getHeight() - 12, 12, 12));
			}
		}

		class RunnableGroup implements Runnable {
			List<Runnable> buffer;

			public RunnableGroup() {
				buffer = new ArrayList<Runnable>(runnableBuffer);
				runnableBuffer.clear();
			}

			@Override
			public void run() {
				for (Runnable r : buffer) {
					r.run();
				}
			}
		}
	}

	class ColouredWriter extends StringWriter {
		private final JConsole console;

		private final DevConsoleStyle style;

		public ColouredWriter(DevConsoleStyle style, JConsole console) {
			this.style = style;
			this.console = console;
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			write(new String(cbuf, off, len));
		}

		@Override
		public void write(String str) {
			write(str, 0, str.length());
		}

		@Override
		public void write(final String buf, int off, int len) {
			DevConsoleStyle entryStyle = console.currentStyle;
			if (style != DevConsoleStyle.NORMAL) {
				console.invoke(() -> console.setStyle(style));
			}
			console.invoke(() -> console.append(buf));
			if (style != DevConsoleStyle.NORMAL) {
				console.invoke(() -> console.setStyle(entryStyle));
			}
			console.invokeScrollRunnable();
		}
	}

	class SwingPrompter implements StringPrompter {
		@Override
		public String getValue(String prompt) {
			return JOptionPane.showInputDialog(prompt);
		}
	}
}

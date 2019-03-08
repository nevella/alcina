package cc.alcina.extras.dev.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdHelp;
import cc.alcina.extras.dev.console.DevHelper.ConsolePrompter;
import cc.alcina.extras.dev.console.DevHelper.StringPrompter;
import cc.alcina.extras.dev.console.remote.server.DevConsoleRemote;
import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Diff;
import cc.alcina.framework.common.client.util.Diff.Change;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ClassrefScanner;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ShellWrapper;
import cc.alcina.framework.entity.util.ShellWrapper.ShellOutputTuple;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.servlet.AlcinaChildRunnable.AlcinaChildContextRunner;

public abstract class DevConsole<P extends DevConsoleProperties, D extends DevHelper, S extends DevConsoleState>
        implements ClipboardOwner {
    private static BiPrintStream out;

    private static BiPrintStream err;

    private static BiPrintStream devErr;

    private static BiPrintStream devOut;
    // has to happen early, otherwise can never redirect
    static {
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

    private MainFrame mainFrame;

    public D devHelper;

    Map<String, DevConsoleCommand> commandsById = new HashMap<String, DevConsoleCommand>();

    private JConsole consoleLeft = new JConsole();

    private JConsole consoleRight = new JConsole();

    private JPanel panelLeft = new JPanel(new BorderLayout());

    private JPanel panelRight = new JPanel(new BorderLayout());
    {
        panelLeft.add(consoleLeft, BorderLayout.CENTER);
    }
    {
        panelRight.add(consoleRight, BorderLayout.CENTER);
    }

    private JScrollPane scrollLeft = new JScrollPane(panelLeft);

    private JScrollPane scrollRight = new JScrollPane(panelRight);
    {
        scrollLeft.getVerticalScrollBar().setUnitIncrement(16);
        scrollLeft.getHorizontalScrollBar().setUnitIncrement(16);
        scrollRight.getVerticalScrollBar().setUnitIncrement(16);
    }

    private JCommandLine commandLine = new JCommandLine();

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
        consoleLeft.invoke(new Runnable() {
            @Override
            public void run() {
                consoleLeft.setText("");
            }
        });
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

    public void doCommandHistoryDelta(int delta) {
        String cmd = history.getCommand(delta);
        if (!cmd.isEmpty()) {
            setCommandLineText(cmd);
        }
    }

    public void dumpDiff(boolean ignoreEqualLength, boolean ignoreInsertions,
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
                    if (Sx.ntrim(split1[change.line0])
                            .replaceAll("[ ,.~:-]", "")
                            .equals(Sx.ntrim(split2[change.line1])
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

    public void dumpDiff(boolean ignoreEqualLength, boolean ignoreInsertions,
            boolean ignoreLatterSubstring, File f1, String s1, File f2,
            String s2) {
        dumpDiff(ignoreEqualLength, ignoreInsertions, ignoreLatterSubstring,
                false, f1, s1, f2, s2);
    }

    public void dumpTransforms() {
        System.out.println("\n\n");
        Set<DomainTransformEvent> transforms = devHelper.dumpTransforms();
        System.out.println("\n\n");
        setClipboardContents(transforms.toString());
        File dumpFile = getDevFile("dumpTransforms.txt");
        try {
            ResourceUtilities.writeStringToFile(transforms.toString(),
                    dumpFile);
            Ax.out("Transforms dumped to:\n\t%s", dumpFile.getPath());
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public void echoCommand(String commandString) {
        consoleLeft.setStyle(DevConsoleStyle.COMMAND);
        Ax.out("\n>%s", commandString);
        consoleLeft.setStyle(DevConsoleStyle.NORMAL);
    }

    public String endRecordingSysout() {
        out.s2.flush();
        String result = new String(recordOut.toByteArray());
        out.s2 = oldS2;
        return result;
    }

    public abstract void ensureDomainStore() throws Exception;

    public void find(String text) {
        consoleLeft.find(text);
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
                    ShellOutputTuple outputTuple = new ShellWrapper()
                            .runShell("", "pbpaste");
                    return outputTuple.output;
                } catch (Exception e2) {
                    throw new WrappedRuntimeException(e2);
                }
            }
        }
        return result;
    }

    public JConsole getConsoleLeft() {
        return this.consoleLeft;
    }

    public DevConsoleStyle getCurrentConsoleStyle() {
        return consoleLeft.currentStyle;
    }

    public File getDevFile(String path) {
        return new File(String.format("%s/%s", devFolder.getPath(), path));
    }

    public String getMultilineInput(String prompt) {
        return getMultilineInput(prompt, 10, 40);
    }

    public String getMultilineInput(String prompt, int rows, int cols) {
        if (isHeadless()) {
            return getClipboardContents();
        }
        final JTextArea textArea = new JTextArea(rows, cols);
        textArea.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                textArea.requestFocusInWindow();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }
        });
        textArea.setText(getClipboardContents().replace("\r", "\n"));
        JScrollPane jsp = new JScrollPane(textArea);
        int result = JOptionPane.showConfirmDialog(null, jsp, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return textArea.getText();
        } else {
            return null;
        }
    }

    public String getSingleLineInput(String prompt, String defaultValue) {
        final JTextField textArea = new JTextField(40);
        textArea.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                textArea.requestFocusInWindow();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }
        });
        textArea.setText(defaultValue);
        int result = JOptionPane.showConfirmDialog(null, textArea, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return textArea.getText();
        } else {
            return null;
        }
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
        // TODO Auto-generated method stub
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
            DevConsoleCommand template = commandsById.get(cmd);
            if (template == null) {
                consoleLeft.err(String.format("'%s' is not a command\n", cmd));
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
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
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

    public void prepareCommand(DevConsoleCommand cmd) {
        cmd.setEnvironment(this);
    }

    public void saveConfig() throws Exception {
        serializeObject(props, consolePropertiesFile);
        serializeObject(history, consoleHistoryFile);
        serializeObject(strings, consoleStringsFile);
        // this breaks our jaxb classpath loading...only use if kryo failing due
        // to signature change
        // new Thread() {
        // @Override
        // public void run() {
        // try {
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(props),
        // consolePropertiesFile);
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(history),
        // consoleHistoryFile);
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(strings),
        // consoleStringsFile);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // }
        // }.start();
    }

    public void scrollToTopAtEnd() {
        consoleLeft.scrollToTopAtEnd = true;
    }

    public void serializeState() {
        devHelper.writeObject(state);
    }

    /**
     * Place a String on the clipboard, and make this class the owner of the
     * Clipboard's contents.
     */
    public void setClipboardContents(String aString) {
        try {
            StringSelection stringSelection = new StringSelection(aString);
            Clipboard clipboard = Toolkit.getDefaultToolkit()
                    .getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        } catch (HeadlessException e) {
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
        }
    }

    public void setCommandLineText(String text) {
        commandLine.setTextWithPrompt(text);
        remote.addSetCommandLineEvent(text);
    }

    public void setNextCommand(String cmd) {
        props.lastCommand = cmd;
        try {
            saveConfig();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
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

    @SuppressWarnings("unused")
    private void loadFontMetrics() {
        new Thread(() -> {
            new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)
                    .createGraphics().getFontMetrics();
        }).start();
    }

    private void serializeObject(Object object, File file) {
        try {
            new ObjectMapper().enableDefaultTyping()
                    .writerWithDefaultPrettyPrinter().writeValue(file, object);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected void addDomainStoreAndFlowLoggers() {
        // EntityLayerUtils.setStandardAppender(
        // AlcinaLogUtils.getMetricLogger(DomainStore.class), Level.DEBUG);
        // EntityLayerUtils.setStandardAppender(
        // AlcinaLogUtils.getTaggedLogger(DomainStore.class, "sql"),
        // Level.DEBUG);
        EntityLayerUtils.setLevel(
                AlcinaLogUtils.getMetricLogger(DomainStore.class), Level.WARN);
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
        MetricLogging.get().start("init-console");
        // osx =>
        // https://bugs.openjdk.java.net/browse/JDK-8179209
        // loadFontMetrics();
        createDevHelper();
        devHelper.loadDefaultLoggingProperties();
        devHelper.loadJbossConfig(new ConsolePrompter());
        devHelper.initLightweightServices();
        logger = devHelper.getTestLogger();
        List<Class> lookup = Registry.get().lookup(DevConsoleCommand.class);
        filterLookup(lookup);
        for (Class clazz : lookup) {
            DevConsoleCommand cmd = (DevConsoleCommand) clazz.newInstance();
            cmd.setEnvironment(this);
            for (String s : cmd.getCommandIds()) {
                commandsById.put(s, cmd);
            }
        }
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
        initJaxb();
        initState();
        devHelper.loadJbossConfig(null);
        boolean waitForUi = !devHelper.configLoaded;
        consoleLeft.initAttrs(props.fontName);
        consoleRight.initAttrs(props.fontName);
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
            new AlcinaChildContextRunner("launcher-thread")
                    .callNewThreadOrCurrent(() -> initUi(), null, !waitForUi);
        }
        clear();
        MetricLogging.get().end("init-console");
        try {
            devHelper.readAppObjectGraph();
            devHelper.initPostObjectServices();
        } catch (Exception e) {
            e.printStackTrace();
            consoleLeft.err(String.format("Problem retrieving object graph"
                    + " - reloading from server\n\t%s\n\n", e));
            performCommand("gen-objects");
            return;
        }
        if (!props.lastCommand.matches("|q|re|restart")) {
            runningLastCommand = true;
            performCommand(props.lastCommand);
        } else {
            consoleLeft.ok("Enter 'h' for help\n\n");
        }
    }

    protected void initClassrefScanner() throws Exception {
        ClassMetadataCache cache = new CachingClasspathScanner("*", true, false,
                Logger.getLogger(getClass()), Registry.MARKER_RESOURCE,
                Arrays.asList(
                        new String[] { "WEB-INF/classes", "WEB-INF/lib" }))
                                .getClasses();
        ClassrefScanner classrefScanner = new ClassrefScanner();
        if (ResourceUtilities.not(ServletLayerUtils.class,
                "commitTestTransforms")) {
            classrefScanner.noPersistence();
        }
        classrefScanner.scan(cache);
    }

    protected void initJaxb() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // init full jaxb
                    WrappedObjectHelper
                            .xmlSerialize(new DevConsoleProperties());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }.start();
    }

    protected abstract void initState();

    protected void initUi() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                mainFrame = new MainFrame();
                mainFrame.setName("Dev Console");
                mainFrame.setVisible(!remote.isHasRemote());
                devHelper.loadJbossConfig(new SwingPrompter());
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected abstract P newConsoleProperties();

    protected void performCommandInThread(List<String> args,
            DevConsoleCommand c, boolean topLevel) {
        try {
            LooseContext.push();
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
                consoleLeft
                        .ok(String.format("  %s - ok - %s ms\n", msg, l2 - l1));
            }
            if (topLevel && !c.ignoreForCommandHistory()) {
                props.lastCommand = c.rerunIfMostRecentOnRestart() ? lastCommand
                        : "";
                try {
                    saveConfig();
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }
        } catch (Exception e) {
            if (!(e instanceof CancelledException)) {
                e.printStackTrace();
            }
        } finally {
            runningLastCommand = false;
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

        protected synchronized void invoke(Runnable apRun) {
            runnableBuffer.add(apRun);
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

    public static class NullPrintStream extends PrintStream {
        public NullPrintStream() {
            super(new ByteArrayOutputStream());
        }
    }

    private class JCommandLine extends JTextField {
        private KeyListener arrowListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                int delta = 0;
                switch (code) {
                case KeyEvent.VK_DOWN:
                    delta = 1;
                    break;
                case KeyEvent.VK_UP:
                    delta = -1;
                    break;
                }
                if (delta != 0) {
                    doCommandHistoryDelta(delta);
                }
                if (e.isMetaDown() && e.getKeyChar() == 'k') {
                    clear();
                }
            }
        };

        public JCommandLine() {
            addKeyListener(arrowListener);
        }

        public String getTrimmedText() {
            return getText().substring(1);
        }

        @Override
        public void setCaretPosition(int position) {
            super.setCaretPosition(
                    position == 0 ? getText().length() == 0 ? 0 : 1 : position);
        }

        public void setTextWithPrompt(final String text) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setText(">" + text);
                    setCaretPosition(getText().length());
                }
            });
        }

        private void reset() {
            requestFocusInWindow();
            setTextWithPrompt("");
            select(1, 1);
        }
    }

    private class MainFrame extends JFrame {
        private ActionListener clListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = commandLine.getTrimmedText();
                // consoleLeft.append(command + "\n");
                commandLine.reset();
                performCommand(command);
            }
        };

        public MainFrame() {
            JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            jp.add(commandLine, BorderLayout.SOUTH);
            commandLine.addActionListener(clListener);
            jp.setMinimumSize(new Dimension(300, 300));
            jp.setPreferredSize(new Dimension(1250, props.preferredHeight));
            devOut.s2 = new PrintStream(new WriterOutputStream(
                    new ColouredWriter(DevConsoleStyle.NORMAL, consoleLeft)));
            devErr.s2 = new PrintStream(new WriterOutputStream(
                    new ColouredWriter(DevConsoleStyle.ERR, consoleLeft)));
            // JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            // scrollLeft, scrollRight);
            // split.setDividerLocation(490);
            // jp.add(split, BorderLayout.CENTER);
            jp.add(scrollLeft, BorderLayout.CENTER);
            add(jp);
            pack();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    System.exit(0);
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    commandLine.reset();
                }
            });
            setTitle("Dev Console");
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

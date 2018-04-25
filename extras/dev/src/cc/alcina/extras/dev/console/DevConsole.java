package cc.alcina.extras.dev.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

import org.apache.log4j.Logger;

import cc.alcina.extras.dev.DevHelper;
import cc.alcina.extras.dev.DevHelper.StringPrompter;
import cc.alcina.extras.dev.console.DevConsoleCommand.CmdHelp;
import cc.alcina.extras.dev.console.DevConsoleCommand.CmdNextCommandCaches;
import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ClassrefScanner;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.AlcinaChildRunnable;
import cc.alcina.framework.servlet.servlet.AlcinaChildRunnable.AlcinaChildContextRunner;

public abstract class DevConsole<P extends DevConsoleProperties, D extends DevHelper, S extends DevConsoleState>
        implements ClipboardOwner {
    private static BiPrintStream out;

    private static BiPrintStream err;
    // has to happen early, otherwise can never redirect
    static {
        err = new BiPrintStream(new ByteArrayOutputStream());
        err.s1 = System.err;
        err.s2 = new NullPrintStream();
        out = new BiPrintStream(new ByteArrayOutputStream());
        out.s1 = System.out;
        out.s2 = new NullPrintStream();
        System.setErr(err);
        System.setOut(out);
    }

    public static class NullPrintStream extends PrintStream {
        public NullPrintStream() {
            super(new ByteArrayOutputStream());
        }
    }

    static final Color RED = new Color(210, 20, 20);

    static final Color GREEN = new Color(0, 174, 127);

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

    public String endRecordingSysout() {
        out.s2.flush();
        String result = new String(recordOut.toByteArray());
        out.s2 = oldS2;
        return result;
    }

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
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
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
        return result;
    }

    public JConsole getConsoleLeft() {
        return this.consoleLeft;
    }

    public File getDevFile(String path) {
        return new File(String.format("%s/%s", devFolder.getPath(), path));
    }

    public String getMultilineInput(String prompt) {
        return getMultilineInput(prompt, 10, 40);
    }

    public String getMultilineInput(String prompt, int rows, int cols) {
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

    public void resetObjects() {
        devHelper.configLoaded = false;
        devHelper.loadJbossConfig();
        devHelper.readAppObjectGraph();
        devHelper.initPostObjectServices();
    }

    public void saveConfig() throws Exception {
        serializeObject(props, consolePropertiesFile);
        serializeObject(history, consoleHistoryFile);
        serializeObject(strings, consoleStringsFile);
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(props), consolePropertiesFile);
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(history), consoleHistoryFile);
        // ResourceUtilities.writeStringToFile(
        // WrappedObjectHelper.xmlSerialize(strings), consoleStringsFile);
    }

    public void serializeState() {
        devHelper.writeObject(state);
    }

    /**
     * Place a String on the clipboard, and make this class the owner of the
     * Clipboard's contents.
     */
    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
    }

    public void setCommandLineText(String text) {
        commandLine.setTextWithPrompt(text);
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
            return KryoUtils.deserializeFromFile(file, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return WrappedObjectHelper.xmlDeserialize(clazz,
                ResourceUtilities.readFileToString(file));
    }

    private void loadFontMetrics() {
        new Thread(() -> {
            new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)
                    .createGraphics().getFontMetrics();
        }).start();
    }

    private void serializeObject(Object object, File file) {
        KryoUtils.serializeToFile(object, file);
    }

    protected abstract void createDevHelper();

    protected List<Class> getInitClasses() {
        return new ArrayList<>(Arrays.asList(DevConsoleProperties.class,
                DevConsoleStrings.class, DevConsoleHistory.class));
    }

    protected void init() throws Exception {
        MetricLogging.get().start("init-console");
        // osx =>
        // https://bugs.openjdk.java.net/browse/JDK-8179209
        loadFontMetrics();
        createDevHelper();
        devHelper.loadJbossConfig(null);
        devHelper.initLightweightServices();
        logger = devHelper.getTestLogger();
        List<Class> lookup = Registry.get().lookup(DevConsoleCommand.class);
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
        new Thread() {
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
        initState();
        devHelper.loadJbossConfig(null);
        boolean waitForUi = !devHelper.configLoaded;
        consoleLeft.initAttrs(props.fontName);
        consoleRight.initAttrs(props.fontName);
        new AlcinaChildContextRunner("launcher-thread")
                .callNewThreadOrCurrent(() -> initUi(), null, !waitForUi);
        MetricLogging.get().end("init-console");
        try {
            devHelper.readAppObjectGraph();
            devHelper.initPostObjectServices();
        } catch (Exception e) {
            consoleLeft.err(String.format("Problem retrieving object graph"
                    + " - reloading from server\n\t%s\n\n", e));
            performCommand("gen-objects");
            return;
        }
        if (!props.lastCommand.isEmpty() && !props.lastCommand.equals("q")) {
            runningLastCommand = true;
            performCommand(props.lastCommand);
        } else {
            consoleLeft.ok("Enter 'h' for help\n\n");
        }
    }

    protected void initUi() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                mainFrame = new MainFrame();
                mainFrame.setName("Dev Console");
                mainFrame.setVisible(true);
                devHelper.loadJbossConfig(new SwingPrompter());
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected abstract void initState();

    protected abstract P newConsoleProperties();

    protected void performCommand(String command) {
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
            if (topLevel && !(c instanceof CmdNextCommandCaches)) {
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

    public static class JConsole extends JTextPane {
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

        public JConsole() {
            setCaretPosition(0);
            setMargin(new Insets(5, 5, 5, 5));
            initAttrs("Courier New");
            setStyle(ConsoleStyle.NORMAL);
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

        public static final int maxChars = 250000;

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
            Runnable apRun = new Runnable() {
                public void run() {
                    setStyle(ConsoleStyle.ERR);
                    append(msg);
                    setStyle(ConsoleStyle.NORMAL);
                    maybeScroll(this);
                }
            };
            invoke(apRun);
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
            attrs = new SimpleAttributeSet[3];
            for (int i = 0; i < 3; i++) {
                attrs[i] = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attrs[i], fontName);
                StyleConstants.setFontSize(attrs[i], 15);
                if (i != 0) {
                    StyleConstants.setBold(attrs[i], true);
                }
            }
            StyleConstants.setForeground(attrs[1], GREEN);
            StyleConstants.setForeground(attrs[2], RED);
            highlightRange = new SimpleAttributeSet();
            normalRange = new SimpleAttributeSet();
            StyleConstants.setBackground(highlightRange,
                    new Color(190, 210, 250));
            // StyleConstants.setForeground(highlightRange, Color.PINK);
            StyleConstants.setBackground(normalRange, Color.WHITE);
        }

        public void ok(final String msg) {
            invoke(new Runnable() {
                public void run() {
                    setStyle(ConsoleStyle.OK);
                    append(msg);
                    setStyle(ConsoleStyle.NORMAL);
                    maybeScroll(this);
                }
            });
        }

        public void setStyle(ConsoleStyle style) {
            switch (style) {
            case OK:
                current = attrs[1];
                break;
            case ERR:
                current = attrs[2];
                break;
            case NORMAL:
                current = attrs[0];
            }
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

        protected void maybeScroll(Runnable runnable) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JViewport pane = (JViewport) getParent().getParent();
                    pane.scrollRectToVisible(
                            new Rectangle(0, getHeight() - 12, 12, 12));
                }
            });
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

    private class JCommandLine extends JTextField {
        private KeyListener arrowListener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
            }

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
                    String cmd = history.getCommand(delta);
                    if (!cmd.isEmpty()) {
                        setTextWithPrompt(cmd);
                    }
                }
                if (e.isMetaDown() && e.getKeyChar() == 'k') {
                    clear();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
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
            out.s2 = new PrintStream(new WriterOutputStream(
                    new ColouredWriter(ConsoleStyle.NORMAL, consoleLeft)));
            err.s2 = new PrintStream(new WriterOutputStream(
                    new ColouredWriter(ConsoleStyle.ERR, consoleLeft)));
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
                public void run() {
                    commandLine.reset();
                }
            });
            setTitle("Dev Console");
        }
    }

    class ColouredWriter extends StringWriter {
        private final JConsole console;

        private final ConsoleStyle style;

        public ColouredWriter(ConsoleStyle style, JConsole console) {
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

        public void write(final String buf, int off, int len) {
            console.invoke(new Runnable() {
                public void run() {
                    console.setStyle(style);
                    console.append(buf);
                    console.setStyle(ConsoleStyle.NORMAL);
                    console.maybeScroll(this);
                }
            });
        }
    }

    enum ConsoleStyle {
        NORMAL, OK, ERR
    }

    class SwingPrompter implements StringPrompter {
        @Override
        public String getValue(String prompt) {
            return JOptionPane.showInputDialog(prompt);
        }
    }

    public void setNextCommand(String cmd) {
        props.lastCommand = cmd;
        try {
            saveConfig();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
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

    public abstract void ensureMemCache() throws Exception;
}

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import cc.alcina.extras.dev.DevHelper;
import cc.alcina.extras.dev.DevHelper.StringPrompter;
import cc.alcina.extras.dev.console.DevConsoleCommand.CmdHelp;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;

@SuppressWarnings("unused")
public abstract class DevConsole<P extends DevConsoleProperties, D extends DevHelper, S extends DevConsoleState>
		implements ClipboardOwner {
	private static BiPrintStream out;

	private static BiPrintStream err;
	// has to happen early, otherwise can never redirect
	static {
		err = new BiPrintStream(new ByteArrayOutputStream());
		err.s1 = System.err;
		out = new BiPrintStream(new ByteArrayOutputStream());
		out.s1 = System.out;
		System.setErr(err);
		System.setOut(out);
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

	private String lastCommand;

	static final Color RED = new Color(210, 20, 20);

	static final Color GREEN = new Color(0, 174, 127);

	boolean secondHelperInitted = false;

	public S state;

	File consolePropertiesFile;

	File consoleHistoryFile;

	public File devFolder;

	public File setsFolder;

	File profileFolder;

	public void clear() {
		consoleLeft.setText("");
	}

	public void setCommandLineText(String text) {
		commandLine.setTextWithPrompt(text);
	}

	protected void init() throws Exception {
		MetricLogging.get().start("init-console");
		createDevHelper();
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
		// need to be before ui init, cos window height is a preference
		initFiles();
		loadConfig();
		initState();
		consoleLeft.initAttrs(props.fontName);
		consoleRight.initAttrs(props.fontName);
		mainFrame = new MainFrame();
		mainFrame.setVisible(true);
		devHelper.loadJbossConfig(new SwingPrompter());
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
		if (!props.lastCommand.isEmpty()) {
			performCommand(props.lastCommand);
		} else {
			consoleLeft.ok("Enter 'h' for help\n\n");
		}
	}

	class SwingPrompter implements StringPrompter {
		@Override
		public String getValue(String prompt) {
			return JOptionPane.showInputDialog(prompt);
		}
	}

	protected abstract void initState();

	protected abstract void createDevHelper();

	void initFiles() {
		devFolder = devHelper.getDevFolder();
		setsFolder = getDevFile("sets");
		setsFolder.mkdirs();
		profileFolder = getDevFile("profiles");
		profileFolder.mkdir();
		consolePropertiesFile = getDevFile("console-properties.xml");
		consoleHistoryFile = getDevFile("console-history.xml");
	}

	public File getDevFile(String path) {
		return new File(String.format("%s/%s", devFolder.getPath(), path));
	}

	public void loadConfig() throws JAXBException, IOException {
		// eclipse may be caching - read directly
		if (consolePropertiesFile.exists()) {
			props = (P) WrappedObjectHelper.xmlDeserialize(
					DevConsoleProperties.class,
					ResourceUtilities.readFileToString(consolePropertiesFile));
		} else {
			props = newConsoleProperties();
		}
		if (consoleHistoryFile.exists()) {
			history = WrappedObjectHelper.xmlDeserialize(
					DevConsoleHistory.class,
					ResourceUtilities.readFileToString(consoleHistoryFile));
		} else {
			history = new DevConsoleHistory();
		}
		saveConfig();
		if (props.useMountSshfsFs) {
			devHelper.useMountSshfsFs();
		}
	}

	protected abstract P newConsoleProperties();

	protected void performCommand(String command) {
		if (command.isEmpty()) {
			command = this.lastCommand;
		}
		this.lastCommand = command;
		StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(
				command));
		tokenizer.ordinaryChars('0', '9');
		tokenizer.wordChars('0', '9');
		tokenizer.ordinaryChars('.', '.');
		tokenizer.wordChars('.', '.');
		tokenizer.ordinaryChars('-', '-');
		tokenizer.wordChars('-', '-');
		tokenizer.wordChars('/', '/');
		tokenizer.wordChars('_', '_');
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
					System.err.format("Command '%s' already running\n", c2
							.getClass().getSimpleName());
					return;
				}
			}
			Runnable runnable = new Runnable() {
				public void run() {
					performCommandInThread(args, c, true);
				}
			};
			new Thread(runnable).start();
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
			System.out.format("%s...\n", lastCommand);
			consoleRight.setText("");
			long l1 = System.currentTimeMillis();
			c.configure();
			String msg = c
					.run((String[]) args.toArray(new String[args.size()]));
			c.cleanup();
			long l2 = System.currentTimeMillis();
			consoleLeft.ok(String.format("  %s - ok - %s ms\n", msg, l2 - l1));
			if (topLevel) {
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
			LooseContext.pop();
			runningJobs.remove(c);
		}
	}

	List<DevConsoleCommand> runningJobs = new ArrayList<DevConsoleCommand>();

	public Logger logger;

	public void saveConfig() throws IOException, JAXBException {
		ResourceUtilities.writeStringToFile(
				WrappedObjectHelper.xmlSerialize(props), consolePropertiesFile);
		ResourceUtilities.writeStringToFile(
				WrappedObjectHelper.xmlSerialize(history), consoleHistoryFile);
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
					String cmd = history.getCommand(delta);
					if (!cmd.isEmpty()) {
						setTextWithPrompt(cmd);
					}
				}
			}
		};

		public JCommandLine() {
			addKeyListener(arrowListener);
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

		public String getTrimmedText() {
			return getText().substring(1);
		}

		@Override
		public void setCaretPosition(int position) {
			super.setCaretPosition(position == 0 ? getText().length() == 0 ? 0
					: 1 : position);
		}

		private void reset() {
			requestFocusInWindow();
			setTextWithPrompt("");
			select(1, 1);
		}
	}

	private static class JConsole extends JTextPane {
		private SimpleAttributeSet current = null;

		private SimpleAttributeSet[] attrs;

		public JConsole() {
			setCaretPosition(0);
			setMargin(new Insets(5, 5, 5, 5));
			initAttrs("Courier New");
			setStyle(ConsoleStyle.NORMAL);
			setEditable(false);
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
		}

		public void append(final String str) {
			StyledDocument doc = getStyledDocument();
			try {
				doc.insertString(doc.getLength(), str, current);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

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

		protected void maybeScroll(Runnable runnable) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JViewport pane = (JViewport) getParent().getParent();
					pane.scrollRectToVisible(new Rectangle(0, getHeight() - 12,
							12, 12));
				}
			});
		}

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

		protected synchronized void invoke(Runnable apRun) {
			runnableBuffer.add(apRun);
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

		public synchronized void commit() {
			SwingUtilities.invokeLater(new RunnableGroup());
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
			out.s2 = new PrintStream(new WriterOutputStream(new ColouredWriter(
					ConsoleStyle.NORMAL, consoleLeft)));
			err.s2 = new PrintStream(new WriterOutputStream(new ColouredWriter(
					ConsoleStyle.ERR, consoleLeft)));
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

	public void prepareCommand(DevConsoleCommand cmd) {
		cmd.setEnvironment(this);
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

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO Auto-generated method stub
	}

	public String getMultilineInput(String prompt) {
		final JTextArea textArea = new JTextArea(10, 40);
		textArea.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorRemoved(AncestorEvent event) {
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
			}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				textArea.requestFocusInWindow();
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

	public String padLeft(String str, int tabCount) {
		return "\t" + str.replace("\n", "\n\t");
	}

	public String breakAndPad(int tabCount, int width, String text) {
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
		return padLeft(sb.toString(), width);
	}

	public void resetObjects() {
		devHelper.loadJbossConfig();
		devHelper.readAppObjectGraph();
		devHelper.initPostObjectServices();
	}
}

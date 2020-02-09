package cc.alcina.extras.dev.console.remote.server;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import cc.alcina.extras.dev.console.DevConsole;
import cc.alcina.extras.dev.console.DevConsole.DevConsoleStyle;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.jscodeserver.JsCodeServerServlet;

public class DevConsoleRemote {
	public static final transient String CONTEXT_CALLER_CLIENT_INSTANCE_UID = DevConsoleRemote.class
			.getName() + ".CONTEXT_CALLER_CLIENT_INSTANCE_UID";

	ConsoleWriter out = new ConsoleWriter(false);

	ConsoleWriter err = new ConsoleWriter(true);

	Object outputReadyNotifier = new Object();

	Timer timer = new Timer();

	TimerTask notifyTask = null;

	private List<ConsoleRecord> records = new ArrayList<>();

	private DevConsole devConsole;

	private boolean hasRemote;

	Map<String, Integer> perClientInstanceRecordOffsets = new LinkedHashMap<>();

	public DevConsoleRemote(DevConsole devConsole) {
		this.devConsole = devConsole;
	}

	public void addClearEvent() {
		ConsoleRecord record = new ConsoleRecord();
		record.clear = true;
		addRecord(record);
	}

	public void addSetCommandLineEvent(String text) {
		ConsoleRecord record = new ConsoleRecord();
		record.commandText = text;
		addRecord(record);
	}

	public void doCommandHistoryDelta(int delta) {
		devConsole.doCommandHistoryDelta(delta);
	}

	public String getAppName() {
		return Ax.blankTo(ResourceUtilities.get("appName"),
				() -> devConsole.getClass().getSimpleName());
	}

	public Writer getErrWriter() {
		return err;
	}

	public Writer getOutWriter() {
		return out;
	}

	public synchronized boolean hasRecords(String clientInstanceUid) {
		int size = this.records.size();
		int currentOffset = perClientInstanceRecordOffsets
				.computeIfAbsent(clientInstanceUid, id -> 0);
		return currentOffset != size;
	}

	public boolean isHasRemote() {
		return this.hasRemote;
	}

	public void performCommand(String commandString) {
		devConsole.echoCommand(commandString);
		devConsole.performCommand(commandString);
	}

	public void start(boolean configLoaded) throws Exception {
		if (!configLoaded || !ResourceUtilities.is("serve")) {
			return;
		}
		hasRemote = true;
		new Thread() {
			@Override
			public void run() {
				try {
					run0();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			};
		}.start();
	}

	private void run0() throws Exception {
		int port = Integer.parseInt(ResourceUtilities.get("port"));
		Ax.out("Serving on port %s", port);
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		ClassLoader cl = DevConsoleRemote.class.getClassLoader();
		URL gwtHtmlFile = cl.getResource(
				"cc/alcina/extras/dev/console/remote/war/remote.html");
		if (gwtHtmlFile == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
		// Resolve file to directory
		URI webRootUri = gwtHtmlFile.toURI().resolve("./").normalize();
		System.err.println("WebRoot is " + webRootUri);
		HandlerCollection handlers = new HandlerCollection();
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					"/remote-console.do");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new DevConsoleProtocolHandler(this));
			handlers.addHandler(protocolHandler);
		}
		{
			ContextHandler serveLocalHandler = new ContextHandler(handlers,
					"/serve-local.do");
			serveLocalHandler.setAllowNullPathInfo(true);
			serveLocalHandler.setHandler(new DevConsoleServeLocalHandler(this));
			handlers.addHandler(serveLocalHandler);
		}
		{
			ServletContextHandler jsCodeServerHandler = new ServletContextHandler(
					handlers, "/jsCodeServer.tcp");
			jsCodeServerHandler.addServlet(
					new ServletHolder(new JsCodeServerServlet()), "/*");
			jsCodeServerHandler.setAllowNullPathInfo(true);
			handlers.addHandler(jsCodeServerHandler);
		}
		{
			ServletContextHandler resourceHandler = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			resourceHandler.setContextPath("/");
			resourceHandler.setBaseResource(Resource.newResource(webRootUri));
			resourceHandler.setWelcomeFiles(new String[] { "remote.html" });
			// Lastly, the default servlet for root content (always needed, to
			// satisfy servlet spec)
			// It is important that this is last.
			ServletHolder holderPwd = new ServletHolder("default",
					DefaultServlet.class);
			holderPwd.setInitParameter("resourceBase", webRootUri.toString());
			holderPwd.setInitParameter("dirAllowed", "false");
			resourceHandler.addServlet(holderPwd, "/");
			handlers.addHandler(resourceHandler);
		}
		server.setHandler(handlers);
		server.start();
		server.dumpStdErr();
		server.join();
	}

	synchronized void addRecord(ConsoleRecord record) {
		records.add(record);
		if (notifyTask != null) {
			return;
		}
		notifyTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (outputReadyNotifier) {
					outputReadyNotifier.notifyAll();
				}
			}
		};
		timer.scheduleAtFixedRate(notifyTask, 50, 50);
	}

	synchronized List<ConsoleRecord> takeRecords(String clientInstanceUid) {
		int size = this.records.size();
		int currentOffset = perClientInstanceRecordOffsets
				.computeIfAbsent(clientInstanceUid, id -> 0);
		List<ConsoleRecord> returnRecords = this.records.stream()
				.skip(currentOffset).collect(Collectors.toList());
		currentOffset = this.records.size();
		perClientInstanceRecordOffsets.put(clientInstanceUid, currentOffset);
		int f_currentOffset = currentOffset;
		// cull any really old offsets
		perClientInstanceRecordOffsets.entrySet()
				.removeIf(e -> e.getValue() + 99999 < f_currentOffset);
		// are they all at the end?
		boolean allCurrent = perClientInstanceRecordOffsets.entrySet().stream()
				.allMatch(e -> e.getValue() == f_currentOffset);
		if (allCurrent) {
			perClientInstanceRecordOffsets.entrySet()
					.forEach(e -> e.setValue(0));
			this.records.clear();
		}
		if (notifyTask != null) {
			notifyTask.cancel();
			notifyTask = null;
		}
		returnRecords = returnRecords.stream()
				.filter(record -> record.matchesCaller(clientInstanceUid))
				.collect(Collectors.toList());
		return returnRecords;
	}

	class ConsoleRecord {
		String text = "";

		boolean clear;

		String commandText;

		boolean errWriter;

		DevConsoleStyle style;

		String callerClientInstanceUid;

		public ConsoleRecord() {
			putCallerId();
		}

		public ConsoleRecord(String text, boolean errWriter) {
			this.text = text;
			this.errWriter = errWriter;
			this.style = errWriter ? DevConsoleStyle.ERR
					: devConsole.getCurrentConsoleStyle();
			putCallerId();
		}

		public boolean matchesCaller(String clientInstanceUid) {
			return callerClientInstanceUid == null || Objects
					.equals(callerClientInstanceUid, clientInstanceUid);
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		private void putCallerId() {
			this.callerClientInstanceUid = LooseContext
					.get(CONTEXT_CALLER_CLIENT_INSTANCE_UID);
			if (this.callerClientInstanceUid == null) {
				int debug = 3;
			}
		}
	}

	class ConsoleWriter extends StringWriter {
		private boolean errWriter;

		private StringBuilder lineBuffer = new StringBuilder();

		public ConsoleWriter(boolean errWriter) {
			this.errWriter = errWriter;
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
		public synchronized void write(final String buf, int off, int len) {
			if (buf.contains("\n")) {
				addRecord(new ConsoleRecord(lineBuffer.toString() + buf,
						errWriter));
				lineBuffer = new StringBuilder();
			} else {
				lineBuffer.append(buf);
			}
		}
	}
}

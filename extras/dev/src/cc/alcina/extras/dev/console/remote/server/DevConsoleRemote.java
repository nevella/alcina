package cc.alcina.extras.dev.console.remote.server;

import java.io.IOException;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import com.google.common.base.Preconditions;

import cc.alcina.extras.dev.console.DevConsole;
import cc.alcina.extras.dev.console.DevConsole.DevConsoleStyle;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.jscodeserver.JsCodeServerServlet;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentHandler;
import cc.alcina.framework.servlet.component.test.server.AlcinaDevTestHandler;
import cc.alcina.framework.servlet.logging.FlightEventJettyHandler;
import cc.alcina.framework.servlet.servlet.JobJettyHandler;

@Registration.Singleton(DevConsoleRemote.class)
public class DevConsoleRemote {
	public static final transient String CONTEXT_CALLER_CLIENT_INSTANCE_UID = DevConsoleRemote.class
			.getName() + ".CONTEXT_CALLER_CLIENT_INSTANCE_UID";

	public static DevConsoleRemote get() {
		return Registry.impl(DevConsoleRemote.class);
	}

	ConsoleWriter out = new ConsoleWriter(false);

	ConsoleWriter err = new ConsoleWriter(true);

	Object outputReadyNotifier = new Object();

	Timer timer = new Timer();

	TimerTask notifyTask = null;

	private List<ConsoleRecord> records = new ArrayList<>();

	private DevConsole devConsole;

	private boolean hasRemote;

	Map<String, Integer> perClientInstanceRecordOffsets = new LinkedHashMap<>();

	private Integer overridePort;

	HandlerCollection handlers;

	public void addClearEvent() {
		ConsoleRecord record = new ConsoleRecord();
		record.clear = true;
		addRecord(record);
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

	public void addSetCommandLineEvent(String text) {
		ConsoleRecord record = new ConsoleRecord();
		record.commandText = text;
		addRecord(record);
	}

	protected void addSubclassHandlers(HandlerCollection handlers) {
	}

	public void doCommandHistoryDelta(int delta) {
		devConsole.doCommandHistoryDelta(delta);
	}

	public String getAppName() {
		return Ax.blankTo(Configuration.get("appName"),
				() -> devConsole.getClass().getSimpleName());
	}

	public DevConsole getDevConsole() {
		return this.devConsole;
	}

	public Writer getErrWriter() {
		return err;
	}

	public Writer getOutWriter() {
		return out;
	}

	public Integer getOverridePort() {
		return this.overridePort;
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

	Map<String, RomcomServerHandler> hostComponentHandlers = new LinkedHashMap<>();

	void registerRemoteComponent(RemoteComponent component) {
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					component.getPath());
			protocolHandler.setAllowNullPathInfo(true);
			RomcomServerHandler pathHandler = new RomcomServerHandler(component,
					false);
			String host = component.getHost();
			if (host != null) {
				RomcomServerHandler rootHandler = new RomcomServerHandler(
						component, true);
				Preconditions
						.checkState(!hostComponentHandlers.containsKey(host));
				hostComponentHandlers.put(host, rootHandler);
			}
			protocolHandler.setHandler(pathHandler);
		}
	}

	public static class RomcomServerHandler extends AbstractHandler {
		RemoteComponentHandler handler;

		public RomcomServerHandler(RemoteComponent component,
				boolean rootContext) {
			handler = new RemoteComponentHandler(component,
					rootContext ? "" : component.getPath(), true);
		}

		@Override
		public void handle(String target, Request baseRequest,
				HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			handler.handle(request, response);
			baseRequest.setHandled(true);
		}

		public void setLoadIndicatorHtml(String loadIndicatorHtml) {
			handler.setLoadIndicatorHtml(loadIndicatorHtml);
		}
	}

	public static class TestServerHandler extends AbstractHandler {
		AlcinaDevTestHandler handler;

		public TestServerHandler() {
			handler = new AlcinaDevTestHandler();
		}

		@Override
		public void handle(String target, Request baseRequest,
				HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			handler.handle(request, response);
			baseRequest.setHandled(true);
		}
	}

	protected void registerRemoteComponents(HandlerCollection handlers) {
		Registry.query(RemoteComponent.class).implementations()
				.forEach(this::registerRemoteComponent);
	}

	private void run0() throws Exception {
		int port = overridePort != null ? overridePort.intValue()
				: Integer.parseInt(Configuration.get("port"));
		Ax.out("Dev console: serving on port %s", port);
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		ClassLoader cl = DevConsoleRemote.class.getClassLoader();
		URL consoleGwtHtmlFile = cl.getResource(
				"cc/alcina/extras/dev/console/remote/war/remote.html");
		if (consoleGwtHtmlFile == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
		// Resolve file to directory
		URI consoleWebRootUri = consoleGwtHtmlFile.toURI().resolve("./")
				.normalize();
		handlers = new HandlerCollection();
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					"/remote-console.do");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new DevConsoleProtocolHandler(this));
		}
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					"/flight");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new FlightEventJettyHandler());
		}
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					// "job.do" for consistency with the servlet path
					"/job.do");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new JobJettyHandler());
		}
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					"/rpc-request-router.do");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler
					.setHandler(new DevConsoleRpcRequestRouterHandler(this));
		}
		{
			ContextHandler serveLocalHandler = new ContextHandler(handlers,
					"/serve-local.do");
			serveLocalHandler.setAllowNullPathInfo(true);
			serveLocalHandler.setHandler(new DevConsoleServeLocalHandler(this));
		}
		{
			ContextHandler serveControlHandler = new ContextHandler(handlers,
					"/control");
			serveControlHandler.setAllowNullPathInfo(true);
			serveControlHandler
					.setHandler(new DevConsoleServeControlHandler(this));
		}
		{
			ServletContextHandler jsCodeServerHandler = new ServletContextHandler(
					handlers, "/jsCodeServer.tcp");
			jsCodeServerHandler.addServlet(
					new ServletHolder(new JsCodeServerServlet()), "/*");
			jsCodeServerHandler.setAllowNullPathInfo(true);
		}
		{
			ContextHandler protocolHandler = new ContextHandler(handlers,
					"/alcina.gwt.test");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new TestServerHandler());
		}
		registerRemoteComponents(handlers);
		addSubclassHandlers(handlers);
		{
			ContextHandler protocolHandler = new ContextHandler(handlers, "/");
			protocolHandler.setAllowNullPathInfo(true);
			protocolHandler.setHandler(new RootComponentHandler());
		}
		{
			ServletContextHandler resourceHandler = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			resourceHandler.setContextPath("/");
			resourceHandler
					.setBaseResource(Resource.newResource(consoleWebRootUri));
			resourceHandler.setWelcomeFiles(new String[] { "remote.html" });
			// Lastly, the default servlet for root content (always needed, to
			// satisfy servlet spec)
			// It is important that this is last.
			ServletHolder holderPwd = new ServletHolder("default",
					DefaultServlet.class);
			holderPwd.setInitParameter("resourceBase",
					consoleWebRootUri.toString());
			holderPwd.setInitParameter("dirAllowed", "false");
			resourceHandler.addServlet(holderPwd, "/");
			handlers.addHandler(resourceHandler);
		}
		server.setAttribute("org.mortbay.jetty.Request.maxFormContentSize", -1);
		server.setAttribute(
				"org.eclipse.jetty.server.Request.maxFormContentSize", -1);
		server.setHandler(handlers);
		try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		// server.dumpStdErr();
		server.join();
	}

	class RootComponentHandler extends AbstractHandler {
		public RootComponentHandler() {
		}

		@Override
		public void handle(String target, Request baseRequest,
				HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			String host = request.getHeader("host");
			RomcomServerHandler componentHandler = hostComponentHandlers
					.get(host);
			if (componentHandler != null) {
				componentHandler.handle(target, baseRequest, request, response);
			}
		}
	}

	public void setDevConsole(DevConsole devConsole) {
		this.devConsole = devConsole;
	}

	public void setOverridePort(Integer overridePort) {
		this.overridePort = overridePort;
	}

	public void start() throws Exception {
		if (!Configuration.is("serve")) {
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
					: devConsole.getStyle();
			putCallerId();
		}

		public boolean matchesCaller(String clientInstanceUid) {
			return callerClientInstanceUid == null || Objects
					.equals(callerClientInstanceUid, clientInstanceUid);
		}

		private void putCallerId() {
			this.callerClientInstanceUid = LooseContext
					.get(CONTEXT_CALLER_CLIENT_INSTANCE_UID);
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
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

package cc.alcina.extras.dev.component.remote.server;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentConsoleChanges;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest.RemoteComponentRequestType;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentStartupModel;
import cc.alcina.extras.dev.component.remote.server.RemoteComponentRemote.ConsoleRecord;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;

public class RemoteComponentProtocolHandler extends AbstractHandler {
	private RemoteComponentRemote consoleRemote;

	public RemoteComponentProtocolHandler() {
		this.consoleRemote = RemoteComponentRemote.get();
		URL url = getResourceUrl("/rc.html");
		if (url == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		switch (method) {
		case "GET":
			serveFile(baseRequest, request, response);
			break;
		case "POST":
			serveProtocol(baseRequest, request, response);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	URL getResourceUrl(String warRelativePart) {
		ClassLoader cl = RemoteComponentProtocolHandler.class.getClassLoader();
		return cl.getResource(
				Ax.format("cc/alcina/extras/dev/component/remote/war%s",
						warRelativePart));
	}

	void serveFile(Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String path = request.getPathInfo();
		if (path.matches("/c/.+")) {
			path = "/rc.html";
		}
		URL url = getResourceUrl(path);
		if (url == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.addHeader("Cross-Origin-Opener-Policy", "same-origin");
		response.addHeader("Cross-Origin-Embedder-Policy", "require-corp");
		String suffix = path.replaceFirst(".+\\.(.+)", "$1");
		switch (suffix) {
		case "html":
			response.setContentType("text/html");
			break;
		case "json":
			response.setContentType("application/json");
			break;
		case "js":
			response.setContentType("text/javascript");
			break;
		default:
			throw new UnsupportedOperationException();
		}
		Io.read().fromStream(url.openStream()).write()
				.toStream(response.getOutputStream());
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
	}

	void serveProtocol(Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			LooseContext.push();
			response.setContentType("application/json");
			String requestJson = Io.read().fromStream(request.getInputStream())
					.asString();
			if (requestJson.length() > 0) {
				RemoteComponentRequest consoleRequest = ReflectiveSerializer
						.deserialize(requestJson);
				LooseContext.set(
						RemoteComponentRemote.CONTEXT_CALLER_CLIENT_INSTANCE_UID,
						consoleRequest.getClientInstanceUid());
				MethodHandler methodHandler = Registry
						.query(MethodHandler.class)
						.forEnum(consoleRequest.getType());
				RemoteComponentResponse consoleResponse = methodHandler
						.handle(consoleRequest, this);
				response.getWriter()
						.write(ReflectiveSerializer.serialize(consoleResponse));
			}
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
		} finally {
			LooseContext.pop();
		}
	}

	@Registration(MethodHandler.class)
	public static abstract class MethodHandler implements
			Registration.EnumDiscriminator<RemoteComponentRequestType> {
		public abstract RemoteComponentRequestType getType();

		@Override
		public RemoteComponentRequestType provideEnumDiscriminator() {
			return getType();
		}

		protected abstract RemoteComponentResponse handle(
				RemoteComponentRequest consoleRequest,
				RemoteComponentProtocolHandler devConsoleProtocolHandler);
	}

	public static class MethodHandler_DO_COMMAND extends MethodHandler {
		@Override
		public RemoteComponentRequestType getType() {
			return RemoteComponentRequestType.DO_COMMAND;
		}

		@Override
		protected RemoteComponentResponse handle(
				RemoteComponentRequest consoleRequest,
				RemoteComponentProtocolHandler devConsoleProtocolHandler) {
			// devConsoleProtocolHandler.consoleRemote
			// .performCommand(consoleRequest.getCommandString());
			RemoteComponentResponse response = new RemoteComponentResponse();
			return response;
		}
	}

	public static class MethodHandler_GET_RECORDS extends MethodHandler {
		@Override
		public RemoteComponentRequestType getType() {
			return RemoteComponentRequestType.GET_RECORDS;
		}

		@Override
		protected RemoteComponentResponse handle(
				RemoteComponentRequest consoleRequest,
				RemoteComponentProtocolHandler devConsoleProtocolHandler) {
			RemoteComponentResponse response = new RemoteComponentResponse();
			Object outputReadyNotifier = devConsoleProtocolHandler.consoleRemote.outputReadyNotifier;
			synchronized (outputReadyNotifier) {
				try {
					if (!devConsoleProtocolHandler.consoleRemote.hasRecords(
							consoleRequest.getClientInstanceUid())) {
						outputReadyNotifier.wait(1 * TimeConstants.ONE_HOUR_MS);
					}
				} catch (InterruptedException e) {
					//
				}
				List<ConsoleRecord> records = devConsoleProtocolHandler.consoleRemote
						.takeRecords(consoleRequest.getClientInstanceUid());
				RemoteComponentConsoleChanges changes = new RemoteComponentConsoleChanges();
				response.setChanges(changes);
				StringBuilder builder = new StringBuilder();
				for (ConsoleRecord consoleRecord : records) {
					String text = consoleRecord.text;
					if (Ax.notBlank(text)) {
						if (!text.contains("<") && !text.contains(">")
								&& !Configuration.is("disablePathLinks")) {
							text = text.replaceAll(
									"(?:^|\\s)(/(?:tmp|Users|~).+?)(?:\n|\t|$)",
									"<a href='/serve-local.do?$1' target='_blank'>$1</a>");
						}
						String escaped = text.contains("<a href=")
								&& !text.contains("\"<") ? text
										: StringEscapeUtils.escapeHtml(text);
					}
					if (consoleRecord.clear) {
						changes.setClearOutput(true);
						builder.setLength(0);
					}
					if (consoleRecord.commandText != null) {
						changes.setCommandLine(consoleRecord.commandText);
					}
				}
				changes.setOutputHtml(builder.toString());
			}
			return response;
		}
	}

	public static class MethodHandler_STARTUP extends MethodHandler {
		@Override
		public RemoteComponentRequestType getType() {
			return RemoteComponentRequestType.STARTUP;
		}

		@Override
		protected RemoteComponentResponse handle(
				RemoteComponentRequest consoleRequest,
				RemoteComponentProtocolHandler devConsoleProtocolHandler) {
			RemoteComponentResponse response = new RemoteComponentResponse();
			RemoteComponentStartupModel startupModel = new RemoteComponentStartupModel();
			// String appName = devConsoleProtocolHandler.devConsoleRemote
			// .getAppName();
			// startupModel.setAppName(appName);
			response.setStartupModel(startupModel);
			return response;
		}
	}
}

package cc.alcina.extras.dev.component.remote.server;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.PathrefDom;

public class ProtocolRequestHandler extends AbstractHandler {
	// FIXME - remcon - remove (and possibly the whole class, since most of it
	// is/should be covered by Environment)
	private RemoteComponentRemote consoleRemote;

	public ProtocolRequestHandler() {
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
		ClassLoader cl = ProtocolRequestHandler.class.getClassLoader();
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

	void serveProtocol(Request baseRequest, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws IOException {
		try {
			LooseContext.push();
			servletResponse.setContentType("application/json");
			String requestJson = Io.read()
					.fromStream(servletRequest.getInputStream()).asString();
			if (requestJson.length() > 0) {
				RemoteComponentRequest request = ReflectiveSerializer
						.deserialize(requestJson);
				LooseContext.set(
						RemoteComponentRemote.CONTEXT_CALLER_CLIENT_INSTANCE_UID,
						request.session.clientInstanceUid);
				ProtocolMessageHandlerServer messageHandler = Registry.impl(
						ProtocolMessageHandlerServer.class,
						request.protocolMessage.getClass());
				RemoteComponentResponse response = new RemoteComponentResponse();
				response.requestId = request.requestId;
				response.session = request.session;
				Environment env = PathrefDom.get()
						.getEnvironment(request.session);
				try {
					env.validateSession(request.session,
							messageHandler.isValidateClientInstanceUid());
				} catch (Exception e) {
					e.printStackTrace();
					ProtocolMessage.ProcessingException processingException = new ProtocolMessage.ProcessingException();
					processingException.exceptionClassName = e.getClass()
							.getName();
					processingException.exceptionMessage = CommonUtils
							.toSimpleExceptionMessage(e);
					response.protocolMessage = processingException;
				}
				// FIXME - remcon - handle missed, out-of-order messages
				synchronized (env) {
					messageHandler.handle(request, response, env,
							request.protocolMessage);
				}
				servletResponse.getWriter()
						.write(ReflectiveSerializer.serialize(response));
			}
			servletResponse.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
		} finally {
			LooseContext.pop();
		}
	}
}

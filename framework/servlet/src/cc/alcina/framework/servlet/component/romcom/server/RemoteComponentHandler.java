package cc.alcina.framework.servlet.component.romcom.server;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException.Action;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.ProtocolException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageHandlerServer;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageHandlingToken;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.EnvironmentManager;
import cc.alcina.framework.servlet.dom.RemoteUi;

/*
 * Provides common implementation for jetty/servlet remotecomponent handling
 */
class RemoteComponentHandler {
	RemoteComponent component;

	String loadIndicatorHtml = "";

	String featurePath;

	RemoteComponentHandler(RemoteComponent component, String featurePath) {
		this.component = component;
		this.featurePath = featurePath;
		URL url = getResourceUrl("/rc.html");
		if (url == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
	}

	private InvalidClientException
			buildInvalidClientException(String componentClassName) {
		Class<? extends RemoteUi> uiType = Reflections
				.forName(componentClassName);
		boolean singleInstance = RemoteUi.SingleInstance.class
				.isAssignableFrom(uiType);
		boolean existingInstance = singleInstance
				&& EnvironmentManager.get().hasEnvironment(uiType);
		String message = null;
		InvalidClientException.Action action = Action.REFRESH;
		if (existingInstance) {
			action = Action.EXPIRED;
			message = "This component client (tab) has ben superseded "
					+ "by a newer access to this component. \n\nPlease use the newer client, "
					+ "or refresh to switch rendering to this client";
		}
		return new InvalidClientException(message, action,
				NestedName.get(uiType));
	}

	URL getResourceUrl(String warRelativePart) {
		ClassLoader cl = RemoteComponentHandler.class.getClassLoader();
		return cl.getResource(
				Ax.format("cc/alcina/framework/servlet/component/romcom/war%s",
						warRelativePart));
	}

	void handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		switch (method) {
		case "GET":
			serveFile(request, response);
			break;
		case "POST":
			serveProtocol(request, response);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	void serveFile(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String path = request.getPathInfo();
		boolean injectSession = false;
		if (path == null) {
			path = "/rc.html";
			injectSession = true;
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
		case "gif":
			response.setContentType("image/gif");
			break;
		default:
			throw new UnsupportedOperationException();
		}
		if (injectSession) {
			String bootstrapHtml = Io.read().fromStream(url.openStream())
					.asString();
			RemoteComponentProtocol.Session session = component
					.createEnvironment(request);
			String sessionJson = StringEscapeUtils
					.escapeJavaScript(ReflectiveSerializer.serialize(session));
			URL nocacheJsUrl = getResourceUrl(
					"/cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient/cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.nocache.js");
			String nocacheJs = Io.read().fromStream(nocacheJsUrl.openStream())
					.asString();
			String websocketTransportClientPrefix = featurePath.substring(1);
			bootstrapHtml = bootstrapHtml.replace("%%SESSION_JSON%%",
					sessionJson);
			bootstrapHtml = bootstrapHtml.replace("%%NOCACHE_JS%%", nocacheJs);
			bootstrapHtml = bootstrapHtml.replace("%%FEATURE_PATH%%",
					featurePath);
			bootstrapHtml = bootstrapHtml.replace(
					"%%WEBSOCKET_TRANSPORT_CLIENT_PREFIX%%",
					websocketTransportClientPrefix);
			bootstrapHtml = bootstrapHtml.replace("%%LOAD_INDICATOR_HTML%%",
					loadIndicatorHtml);
			Io.write().string(bootstrapHtml)
					.toStream(response.getOutputStream());
		} else {
			Io.read().fromStream(url.openStream()).write()
					.toStream(response.getOutputStream());
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	void serveProtocol(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws IOException {
		try {
			LooseContext.push();
			servletResponse.setContentType("application/json");
			String requestJson = Io.read()
					.fromStream(servletRequest.getInputStream()).asString();
			if (requestJson.length() > 0) {
				RemoteComponentRequest request = ReflectiveSerializer
						.deserialize(requestJson);
				MessageHandlerServer messageHandler = Registry.impl(
						MessageHandlerServer.class,
						request.protocolMessage.getClass());
				RemoteComponentResponse response = new RemoteComponentResponse();
				response.requestId = request.requestId;
				response.session = request.session;
				logger.debug("{} received request #{} - {} {}", Ax.appMillis(),
						request.requestId,
						NestedName.get(request.protocolMessage),
						request.protocolMessage.toDebugString());
				try {
					Environment env = EnvironmentManager.get()
							.getEnvironment(request.session);
					if (env == null) {
						throw buildInvalidClientException(
								request.session.componentClassName);
					}
					MessageHandlingToken token = new MessageHandlingToken(
							request, response, messageHandler);
					// http thread
					messageHandler
							.onBeforeMessageHandled(request.protocolMessage);
					// unless sync, on the env thread
					env.handleFromClientMessage(token);
					// http thread
					messageHandler
							.onAfterMessageHandled(request.protocolMessage);
				} catch (Exception e) {
					if (e instanceof ProtocolException) {
						boolean handled = false;
						if (e instanceof InvalidClientException) {
							InvalidClientException clex = (InvalidClientException) e;
							if (clex.action == Action.REFRESH) {
								Ax.out("Refreshing remotecomponent '%s' ",
										clex.uiType);
								handled = true;
							}
						}
						if (!handled) {
							Ax.simpleExceptionOut(e);
						}
					} else {
						e.printStackTrace();
					}
					Message.ProcessingException processingException = new Message.ProcessingException();
					processingException.exceptionClassName = e.getClass()
							.getName();
					processingException.exceptionMessage = CommonUtils
							.toSimpleExceptionMessage(e);
					if (e instanceof ProtocolException) {
						processingException.protocolException = e;
					}
					response.protocolMessage = processingException;
				}
				logger.debug("{} dispatched response #{} - {}", Ax.appMillis(),
						response.requestId,
						NestedName.get(response.protocolMessage));
				servletResponse.getWriter()
						.write(ReflectiveSerializer.serialize(response));
			}
			servletResponse.setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			LooseContext.pop();
		}
	}

	public void setLoadIndicatorHtml(String loadIndicatorHtml) {
		this.loadIndicatorHtml = loadIndicatorHtml;
	}
}

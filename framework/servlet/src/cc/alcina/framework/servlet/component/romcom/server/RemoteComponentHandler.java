package cc.alcina.framework.servlet.component.romcom.server;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.EnvelopeId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageEnvelope;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.SendChannelId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.AwaitTimedOutException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException.Action;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.SetCookieServerSide;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.ProtocolException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;
import cc.alcina.framework.servlet.environment.EnvironmentManager;
import cc.alcina.framework.servlet.environment.EnvironmentManager.EnvironmentList;
import cc.alcina.framework.servlet.publication.DirndlRenderer;

/**
 * <p>
 * Provides common implementation for jetty/servlet remotecomponent handling
 * <p>
 * Threading - note that handle can be called *concurrently* from multiple
 * threads on the same environment - during synchronous server-side evaluation,
 * the return from the client will come on a different servlet thread
 */
public class RemoteComponentHandler {
	static Configuration.Key awaitTimeout = Configuration.key("awaitTimeout");

	RemoteComponent component;

	String loadIndicatorHtml = "";

	String featurePath;

	boolean addOriginHeaders;

	public RemoteComponentHandler(RemoteComponent component, String featurePath,
			boolean addOriginHeaders) {
		this.component = component;
		this.featurePath = featurePath;
		this.addOriginHeaders = addOriginHeaders;
		URL url = getResourceUrl("/rc.html");
		if (url == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
	}

	URL getResourceUrl(String warRelativePart) {
		ClassLoader cl = RemoteComponentHandler.class.getClassLoader();
		return cl.getResource(
				Ax.format("cc/alcina/framework/servlet/component/romcom/war%s",
						warRelativePart));
	}

	public void handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		switch (method) {
		case "GET":
			if (Ax.notBlank(request.getQueryString())
					&& !request.getQueryString().matches("gwt.l")
					&& Ax.notBlank(request.getParameter("action"))) {
				serveQuery(request, response);
			} else {
				serveFile(request, response, null, "");
			}
			break;
		case "POST":
			serveProtocol(request, response);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	void serveQuery(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		new QueryHandler(component, request, response).serve();
	}

	static class QueryHandler {
		HttpServletRequest request;

		HttpServletResponse response;

		RemoteComponent component;

		String path;

		Action action;

		QueryHandler(RemoteComponent component, HttpServletRequest request,
				HttpServletResponse response) {
			this.component = component;
			this.request = request;
			this.response = response;
		}

		void serve() throws IOException {
			action = Action.valueOf(request.getParameter("action"));
			path = request.getParameter("path");
			switch (action) {
			case await:
				try {
					EnvironmentManager.get().await(component, path);
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
				writeTextResponse("Component %s/%s available",
						component.getPath(), path);
				break;
			case list:
				renderEnvironmentList();
				break;
			}
		}

		void renderEnvironmentList() throws IOException {
			EnvironmentList list = EnvironmentManager.get()
					.getEnvironmentList();
			String html = DirndlRenderer.instance().withRenderable(list)
					.addStyleResource(getClass(), "EnvironmentList.css")
					.asDocument().html().toHtml();
			response.setContentType("text/html; charset=utf-8");
			Io.write().string(html).toStream(response.getOutputStream());
		}

		void writeTextResponse(String template, Object... args)
				throws IOException {
			String string = Ax.format(template, args);
			response.setContentType("text/plain");
			Io.write().string(string).toStream(response.getOutputStream());
		}

		enum Action {
			await, list
		}
	}

	volatile String nocacheJs;

	String shimJs;

	public void serveFile(HttpServletRequest request,
			HttpServletResponse response,
			BiFunction<HttpServletRequest, String, String> rcHtmlCustomiser,
			String remotePath) throws IOException {
		String path = request.getPathInfo();
		boolean injectSession = false;
		if (component.isApplicationPath(path)) {
			path = "/rc.html";
			injectSession = true;
		}
		URL url = getResourceUrl(path);
		if (url == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		URL nocacheJsUrl = getResourceUrl(
				"/cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient/cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.nocache.js");
		if (nocacheJs == null) {
			synchronized (this) {
				if (nocacheJs == null) {
					String nocacheJs = Io.read()
							.fromStream(nocacheJsUrl.openStream()).asString();
					Pattern pattern = Pattern.compile("strongName = '(.+)';");
					Matcher matcher = pattern.matcher(nocacheJs);
					matcher.find();
					String strongName = matcher.group(1);
					URL shimUrl = getResourceUrl(Ax.format(
							"/cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient/%s.cache.js",
							strongName));
					shimJs = Io.read().fromStream(shimUrl.openStream())
							.asString();
					this.nocacheJs = nocacheJs;
				}
			}
		}
		if (addOriginHeaders) {
			boolean isSecureLocalhost = Ax.matches(request.getHeader("host"),
					"127.0.0.1(:\\d+)?");
			if (request.isSecure() || isSecureLocalhost) {
				// persuade the browser (at least Chrome) to allow GWT dev
				// mode/ws
				// (sharedarraybuffer)
				response.addHeader("Cross-Origin-Opener-Policy", "same-origin");
				response.addHeader("Cross-Origin-Embedder-Policy",
						"require-corp");
			}
		}
		boolean addCacheHeaders = path.matches(".+\\.cache\\.js");
		if (addCacheHeaders) {
			response.addHeader("Cache-Control", "max-age=8640000");
		}
		String suffix = path.replaceFirst(".+\\.(.+)", "$1");
		switch (suffix) {
		case "html":
			response.setContentType("text/html; charset=utf-8");
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
			RemoteComponentProtocol.Session session = null;
			Exception sessionCreationException = null;
			try {
				List<String> headers = Collections
						.list(request.getHeaderNames()).stream()
						.map(n -> Ax.format("%s=%s", n, request.getHeader(n)))
						.collect(Collectors.toList());
				session = component.createEnvironment(request, response);
				session.shimBytes = shimJs.length();
				logger.info("Created environment - {} ", session);
				logger.debug("Environment headers - {} - http headers: {}",
						session, headers);
			} catch (Exception e) {
				sessionCreationException = e;
				logger.warn("Exception creating session", e);
			}
			if (session != null) {
				String sessionJson = StringEscapeUtils.escapeJavaScript(
						ReflectiveSerializer.serializeForRpc(session));
				String websocketTransportClientPrefix = featurePath.isEmpty()
						? ""
						: featurePath.substring(1) + "/";
				bootstrapHtml = bootstrapHtml.replace("%%SESSION_JSON%%",
						sessionJson);
				bootstrapHtml = bootstrapHtml.replace("%%NOCACHE_JS%%",
						nocacheJs);
				bootstrapHtml = bootstrapHtml.replace("%%FEATURE_PATH%%",
						featurePath);
				bootstrapHtml = bootstrapHtml.replace("%%REMOTE_PATH%%",
						remotePath);
				bootstrapHtml = bootstrapHtml.replace("%%HISTORY_PUSHSTATE%%",
						String.valueOf(component.isHistoryPushState()));
				bootstrapHtml = bootstrapHtml.replace("%%COMPONENT_META%%",
						component.getMetaMarkup());
				bootstrapHtml = bootstrapHtml.replace(
						"%%WEBSOCKET_TRANSPORT_CLIENT_PREFIX%%",
						websocketTransportClientPrefix);
				bootstrapHtml = bootstrapHtml.replace("%%LOAD_INDICATOR_HTML%%",
						loadIndicatorHtml);
				if (rcHtmlCustomiser != null) {
					bootstrapHtml = rcHtmlCustomiser.apply(request,
							bootstrapHtml);
				}
				Io.write().string(bootstrapHtml)
						.toStream(response.getOutputStream());
			} else {
				response.setContentType("text/plain");
				Io.write()
						.string(Ax.blankTo(
								sessionCreationException.getMessage(),
								"session creation issue"))
						.toStream(response.getOutputStream());
			}
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
			long start = System.currentTimeMillis();
			if (requestJson.length() > 0) {
				RemoteComponentRequest request = ReflectiveSerializer
						.deserializeRpc(requestJson);
				RemoteComponentResponse response = new RemoteComponentResponse();
				response.session = request.session;
				logger.debug("{} received request [#{}/#_] - {} {}",
						Ax.appMillis(),
						request.messageEnvelope.envelopeId.number,
						request.messageEnvelope.toMessageSummaryString(),
						request.messageEnvelope.toMessageDebugString());
				try {
					RequestToken token = new RequestToken(requestJson, request,
							response, servletRequest, servletResponse);
					EnvironmentManager.get().acceptRequest(token);
					/*
					 * normally this just runs once, but AwaitRemote is special
					 */
					for (;;) {
						boolean await = token.latch.await(
								awaitTimeout.longValue(),
								TimeUnit.MILLISECONDS);
						if (await) {
							break;
						} else {
							boolean isSessionActive = EnvironmentManager.get()
									.isSessionActive(request.session.id);
							if (isSessionActive && isAwaitRemote(request)) {
								continue;
							} else {
								throw new AwaitTimedOutException(Ax.format(
										"Await timed out :: session - %s :: active - %s :: request - %s",
										request.session.id, isSessionActive,
										request.messageEnvelope
												.toMessageSummaryString()));
							}
						}
					}
				} catch (Exception e) {
					if (e instanceof ProtocolException) {
						boolean handled = false;
						if (e instanceof InvalidClientException) {
							InvalidClientException clex = (InvalidClientException) e;
							if (clex.action == Action.REFRESH) {
								Ax.out("Refreshing remotecomponent '%s' - client %s",
										clex.uiType, request.session.id);
								handled = true;
							}
						}
						if (!handled) {
							Ax.simpleExceptionOut(e);
						}
					} else {
						e.printStackTrace();
					}
					/*
					 * this envelope will have 'queue jump' (-1)
					 * envelope/message ids - client execution will be halted
					 * after processing the message with either a refresh or
					 * finish
					 */
					response.messageEnvelope = new MessageEnvelope();
					SendChannelId sendChannelId = SendChannelId.SERVER_TO_CLIENT;
					response.messageEnvelope.envelopeId = new EnvelopeId(
							sendChannelId, -1);
					ProcessingException message = ProcessingException.wrap(e,
							false);
					MessageId messageId = new MessageId(sendChannelId, -1);
					message.messageId = messageId;
					response.messageEnvelope.messages.add(message);
				}
				logger.debug("{} dispatched response [#{}/#{}] - {}",
						Ax.appMillis(),
						request.messageEnvelope.envelopeId.number,
						response.messageEnvelope.envelopeId.number,
						response.messageEnvelope.toMessageSummaryString());
				new RemoteComponentEvent(request, response, start,
						System.currentTimeMillis()).publish();
				if (response.messageEnvelope.messages.size() > 0) {
					List<Message> withOriginating = response.messageEnvelope.messages
							.stream()
							.filter(m -> m.messageHistory != null
									&& m.messageHistory.originatingMessage != null)
							.toList();
				}
				applyOutgoingMessagesToServletResponse(servletResponse,
						response);
				servletResponse.getWriter()
						.write(ReflectiveSerializer.serializeForRpc(response));
			}
			servletResponse.setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			LooseContext.pop();
		}
	}

	boolean isAwaitRemote(RemoteComponentRequest request) {
		return request.messageEnvelope.messages.size() == 1
				&& request.messageEnvelope.messages
						.get(0) instanceof AwaitRemote;
	}

	@Feature.Ref(Feature_Romcom_Impl._Authentication.class)
	void applyOutgoingMessagesToServletResponse(
			HttpServletResponse servletResponse,
			RemoteComponentResponse response) {
		response.messageEnvelope.messages.forEach(message -> {
			if (message instanceof SetCookieServerSide) {
				SetCookieServerSide cookieMessage = (SetCookieServerSide) message;
				servletResponse.addCookie(
						new Cookie(cookieMessage.name, cookieMessage.value));
				logger.info("Set cookie: {}->'{}'", cookieMessage.name,
						cookieMessage.value);
			}
		});
	}

	public void setLoadIndicatorHtml(String loadIndicatorHtml) {
		this.loadIndicatorHtml = loadIndicatorHtml;
	}
}

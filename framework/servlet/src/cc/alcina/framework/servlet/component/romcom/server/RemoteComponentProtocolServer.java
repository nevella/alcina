package cc.alcina.framework.servlet.component.romcom.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.dom.Environment;

public class RemoteComponentProtocolServer {
	public static final transient String ROMCOM_SERIALIZED_SESSION_KEY = "__alc_romcom_session";

	static Logger logger = LoggerFactory
			.getLogger(RemoteComponentProtocolServer.class);

	public static class MessageHandlingToken {
		public final RemoteComponentRequest request;

		public final RemoteComponentResponse response;

		public final MessageHandlerServer messageHandler;

		public final CountDownLatch latch;

		public final String requestJson;

		public MessageHandlingToken(String requestJson,
				RemoteComponentRequest request,
				RemoteComponentResponse response,
				MessageHandlerServer messageHandler) {
			this.requestJson = requestJson;
			this.request = request;
			this.response = response;
			this.messageHandler = messageHandler;
			this.latch = new CountDownLatch(1);
		}
	}

	@Registration.NonGenericSubtypes(MessageHandlerServer.class)
	public static abstract class MessageHandlerServer<PM extends Message> {
		public abstract void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env, PM message);

		public boolean isValidateClientInstanceUid() {
			return true;
		}

		public void onAfterMessageHandled(PM message) {
		}

		public void onBeforeMessageHandled(PM message) {
		}
	}

	public static class ServerHandler extends AbstractHandler {
		RemoteComponentHandler handler;

		public ServerHandler(RemoteComponent component) {
			handler = new RemoteComponentHandler(component, component.getPath(),
					true);
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
}

package cc.alcina.framework.servlet.component.romcom.server;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

public class RemoteComponentProtocolServer {
	public static final transient String ROMCOM_SERIALIZED_SESSION_KEY = "__alc_romcom_session";

	static Logger logger = LoggerFactory
			.getLogger(RemoteComponentProtocolServer.class);

	/**
	 * Models the state required for Message processing. Some of this will be
	 * upped to RequestToken when message/request are not 1-1
	 * 
	 * FIXME - possibly remove most of this
	 */
	public static class MessageToken {
		public Handler<?, ?> messageHandler;

		public final CountDownLatch latch;

		public final Message message;

		public void messageConsumed() {
			latch.countDown();
		}

		public interface Handler<E, PM extends Message> {
			void handle(MessageToken token, E environment, PM message);
		}

		public MessageToken(Message message) {
			this.messageHandler = null;
			// Registry.impl(Handler.class,
			// message.getClass());
			this.message = message;
			this.latch = new CountDownLatch(1);
		}
	}

	/**
	 * Models the state required for envelope/request processing.
	 */
	public static class RequestToken {
		public final RemoteComponentRequest request;

		public final RemoteComponentResponse response;

		public final CountDownLatch latch;

		public final String requestJson;

		public RequestToken(String requestJson, RemoteComponentRequest request,
				RemoteComponentResponse response) {
			this.requestJson = requestJson;
			this.request = request;
			this.response = response;
			this.latch = new CountDownLatch(1);
		}
	}
}

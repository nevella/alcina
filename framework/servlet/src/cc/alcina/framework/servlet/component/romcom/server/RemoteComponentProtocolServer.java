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
	 */
	public static class MessageToken {
		public final RemoteComponentRequest request;

		public final RemoteComponentResponse response;

		public Handler<?, ?> messageHandler;

		public final CountDownLatch latch;

		public final String requestJson;

		public interface Handler<E, PM extends Message> {
			void handle(MessageToken token, E environment, PM message);
		}

		public MessageToken(String requestJson, RemoteComponentRequest request,
				RemoteComponentResponse response) {
			this.requestJson = requestJson;
			this.request = request;
			this.response = response;
			this.latch = new CountDownLatch(1);
		}

		public void messageConsumed() {
			latch.countDown();
		}
	}
}

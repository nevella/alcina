package cc.alcina.framework.jscodeserver;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsCodeServerWsServlet extends WebSocketServlet {
	static Logger logger = LoggerFactory.getLogger(JsCodeServerWsServlet.class);

	public JsCodeServerWsServlet() {
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setMaxTextMessageSize(9999999);
		factory.register(JsCodeServerWsAdapter.class);
	}

	public static class JsCodeServerWsAdapter extends WebSocketAdapter {
		private WsTcpSession codeserverSession;

		int packetCount = 0;

		double packetEvalTime = 0;

		Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			codeserverSession.close();
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			codeserverSession = new WsTcpSession(sess);
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			cause.printStackTrace();
			getSession().close();
		}

		@Override
		public void onWebSocketText(String b64payload) {
			try {
				long t0 = System.nanoTime();
				String response = codeserverSession
						.sendPacketToCodeServer(b64payload);
				getSession().getRemote().sendString(response);
				long t1 = System.nanoTime();
				double millis = (t1 - t0) / 1000000.0;
				packetCount++;
				packetEvalTime += millis;
				if (packetCount % 1000 == 0) {
					logger.debug("timing data: {} : {}", packetCount,
							packetEvalTime);
				}
			} catch (Exception e) {
				e.printStackTrace();
				getSession().close();
			}
		}
	}
}

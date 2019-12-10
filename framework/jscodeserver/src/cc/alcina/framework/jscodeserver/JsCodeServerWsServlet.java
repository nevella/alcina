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
		factory.register(JsCodeServerWsAdapter.class);
	}

	public static class JsCodeServerWsAdapter extends WebSocketAdapter {
		private WsTcpSession codeserverSession;

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
				String response = codeserverSession
						.sendPacketToCodeServer(b64payload);
				getSession().getRemote().sendString(response);
			} catch (Exception e) {
				e.printStackTrace();
				getSession().close();
			}
		}
	}
}

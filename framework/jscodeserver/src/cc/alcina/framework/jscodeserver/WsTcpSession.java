package cc.alcina.framework.jscodeserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.Date;

import org.eclipse.jetty.websocket.api.Session;

import com.google.gwt.dev.shell.JsCodeserverTcpClientJava;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.entity.Configuration;

public class WsTcpSession {
	Socket socket;

	int messageId;

	int socketPort;

	private JsCodeserverTcpClientJava client;

	int messageLogPer = Configuration.getInt("messageLogPer");

	private Session websocketSession;

	public WsTcpSession(Session websocketSession) {
		this.websocketSession = websocketSession;
	}

	void close() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initSocket() throws Exception {
		String gwtCodesvr = websocketSession.getUpgradeRequest()
				.getParameterMap().get("gwt.codesvr").get(0);
		socketPort = Integer
				.parseInt(gwtCodesvr.replaceFirst(".+:(\\d+)", "$1"));
		socket = new Socket(Configuration.get("host"), socketPort);
		client = new JsCodeserverTcpClientJava(socket);
	}

	public String sendPacketToCodeServer(String payload) throws Exception {
		if (socket == null) {
			initSocket();
		}
		byte[] bytes = Base64.getDecoder().decode(payload);
		socket.getOutputStream().write(bytes);
		byte[] responseBytes = client.receiveMessageBytes();
		if (messageId % messageLogPer == 0) {
			Ax.out("%s :: %s :: %s :: %s",
					CommonUtils.formatDate(new Date(),
							DateStyle.TIMESTAMP_NO_DAY),
					messageId, client.getLastMessageName(),
					client.getLastMessageDetails());
		}
		messageId++;
		return Base64.getEncoder().encodeToString(responseBytes);
	}
}

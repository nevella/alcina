package cc.alcina.framework.jscodeserver;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.dev.shell.XhrTcpClientJava;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.entity.ResourceUtilities;

public class XhrTcpSession {
    Socket socket;

    public int handleId;

    public int messageId;

    int socketPort;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    private XhrTcpClientJava client;

    private XhrTcpBridge xhrTcpBridge;

    int messageLogPer = 100;

    public XhrTcpSession(XhrTcpBridge xhrTcpBridge) {
        this.xhrTcpBridge = xhrTcpBridge;
    }

    /*
     * single-threaded (since js is)
     */
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (socket == null) {
            initSocket(request);
        }
        String meta = request.getHeader(XhrTcpBridge.HEADER_META);
        if (meta != null) {
            switch (meta) {
            case "close_socket":
                client.endSession();
                xhrTcpBridge.removeSession(this);
                return;
            default:
                throw new UnsupportedOperationException();
            }
        }
        String payload = ResourceUtilities
                .readStreamToString(request.getInputStream());
        byte[] bytes = Base64.getDecoder().decode(payload);
        socket.getOutputStream().write(bytes);
        // Ax.out(">>> to codeserver - %s bytes", bytes.length);
        byte[] messageBytes = client.receiveMessageBytes();
        // Ax.out("<<<to browser - %s bytes - %s", messageBytes.length,
        // client.getLastMessageName());
        if (messageId % messageLogPer == 0) {
            Ax.out("%s :: %s :: %s :: %s",
                    CommonUtils.formatDate(new Date(),
                            DateStyle.TIMESTAMP_NO_DAY),
                    messageId, client.getLastMessageName(),
                    client.getLastMessageDetails());
        }
        response.setHeader(XhrTcpBridge.HEADER_HANDLE_ID,
                String.valueOf(handleId));
        response.setHeader(XhrTcpBridge.HEADER_MESSAGE_ID,
                String.valueOf(messageId++));
        response.getOutputStream()
                .write(Base64.getEncoder().encode(messageBytes));
        response.getOutputStream().close();
        return;
    }

    private void initSocket(HttpServletRequest request) throws Exception {
        socketPort = Integer.parseInt(
                request.getHeader(XhrTcpBridge.HEADER_CODE_SERVER_PORT));
        socket = new Socket(ResourceUtilities.get("host"), socketPort);
        client = new XhrTcpClientJava(socket);
    }
}

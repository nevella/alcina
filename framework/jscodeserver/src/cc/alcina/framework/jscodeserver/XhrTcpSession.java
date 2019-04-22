package cc.alcina.framework.jscodeserver;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import cc.alcina.framework.entity.ResourceUtilities;

public class XhrTcpSession {
    Socket socket;

    public int handleId;

    int socketPort;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    /*
     * single-threaded (since js is)
     */
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (socket == null) {
            initSocket(request);
        }
        out.reset();
        String payload = ResourceUtilities
                .readStreamToString(request.getInputStream());
        byte[] bytes = Base64.getDecoder().decode(payload);
        socket.getOutputStream().write(bytes);
        int b = -1;
        while ((b = socket.getInputStream().read()) != -1) {
            out.write(b);
            if (messageComplete()) {
                response.setHeader(XhrTcpBridge.HEADER_HANDLE_ID,
                        String.valueOf(handleId));
                // writeMessageToResponse
                response.getOutputStream()
                        .write(Base64.getEncoder().encode(out.toByteArray()));
                response.getOutputStream().close();
                return;
            }
        }
        socket.close();
    }

    private void initSocket(HttpServletRequest request) throws Exception {
        socketPort = Integer.parseInt(
                request.getHeader(XhrTcpBridge.HEADER_CODE_SERVER_PORT));
        socket = new Socket("127.0.0.1", socketPort);
    }

    private boolean messageComplete() {
        return false;
    }
}

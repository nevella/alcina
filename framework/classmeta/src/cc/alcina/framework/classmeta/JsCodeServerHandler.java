package cc.alcina.framework.classmeta;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extas.jscodeserver.XhrTcpBridge;

public class JsCodeServerHandler extends AbstractHandler {
    static Logger logger = LoggerFactory.getLogger(JsCodeServerHandler.class);

    public JsCodeServerHandler() {
    }

    @Override
    public synchronized void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            XhrTcpBridge.get().handle(target, baseRequest, request, response);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}

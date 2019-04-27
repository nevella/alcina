package cc.alcina.framework.classmeta;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.jscodeserver.XhrTcpBridge;

public class JsCodeServerServlet extends HttpServlet {
    static Logger logger = LoggerFactory.getLogger(JsCodeServerServlet.class);

    public JsCodeServerServlet() {
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        try {
            XhrTcpBridge.get().handle(request, response);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}

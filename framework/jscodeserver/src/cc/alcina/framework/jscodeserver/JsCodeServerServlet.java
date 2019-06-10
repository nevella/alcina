package cc.alcina.framework.jscodeserver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

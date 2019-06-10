package cc.alcina.framework.jscodeserver;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class JsCodeServerHttp {
    public void start(int port) throws Exception {
        Server server = new Server(port);
        HandlerCollection handlers = new HandlerCollection(true);
        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(
                CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
                "OPTIONS,GET,POST,HEAD");
        cors.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM,
                "XhrTcpBridge.handle_id,XhrTcpBridge.message_id");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                "X-Requested-With,Content-Type,Accept,Origin,Cache-Control,xhrtcpbridge.codeserver_port,XhrTcpBridge.handle_id,XhrTcpBridge.message_id,XhrTcpBridge.meta,mixed-content");
        cors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
        {
            ServletContextHandler jsCodeServerHandler = new ServletContextHandler(
                    handlers, "/jsCodeServer.tcp");
            jsCodeServerHandler.addServlet(
                    new ServletHolder(new JsCodeServerServlet()), "/*");
            jsCodeServerHandler.setAllowNullPathInfo(true);
            handlers.addHandler(jsCodeServerHandler);
            jsCodeServerHandler.addFilter(cors, "/*",
                    EnumSet.of(DispatcherType.REQUEST));
        }
        server.setHandler(handlers);
        server.start();
        server.dumpStdErr();
        server.join();
    }
}

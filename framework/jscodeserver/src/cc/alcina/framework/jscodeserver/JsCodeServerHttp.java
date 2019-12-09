package cc.alcina.framework.jscodeserver;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import cc.alcina.framework.common.client.util.Ax;

public class JsCodeServerHttp {
	public void start(int port) throws Exception {
		Server server = new Server(port);
		HandlerCollection handlers = new HandlerCollection(true);
		FilterHolder cors = new FilterHolder(CrossOriginFilterWithTiming.class);
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

	public static class CrossOriginFilterWithTiming extends CrossOriginFilter {
		double xhrTimingCumulativeMillisecondsPost = 0;

		double xhrTimingCumulativeMillisecondsOptions = 0;

		double xhrTimingCumulativeMillisecondsOther = 0;

		int xhrTimingCumulativeCountPost = 0;

		int xhrTimingCumulativeCountOptions = 0;

		int xhrTimingCumulativeCountOther = 0;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			long nanoTime0 = System.nanoTime();
			try {
				super.doFilter(request, response, chain);
			} finally {
				long nanoTime1 = System.nanoTime();
				double millis = (nanoTime1 - nanoTime0) / 1000000.0;
				String method = ((HttpServletRequest) request).getMethod();
				switch (method.toUpperCase()) {
				case "OPTIONS":
					xhrTimingCumulativeCountOptions++;
					xhrTimingCumulativeMillisecondsOptions += millis;
					break;
				case "POST":
					xhrTimingCumulativeCountPost++;
					xhrTimingCumulativeMillisecondsPost += millis;
					break;
				default:
					xhrTimingCumulativeCountOther++;
					xhrTimingCumulativeMillisecondsOther += millis;
					break;
				}
				if (xhrTimingCumulativeCountPost % 1000 == 0) {
					Ax.out("timing data (post/options/other): %s/%s/%s : %s/%s/%s ",
							xhrTimingCumulativeCountPost,
							xhrTimingCumulativeCountOptions,
							xhrTimingCumulativeCountOther,
							xhrTimingCumulativeMillisecondsPost,
							xhrTimingCumulativeMillisecondsOptions,
							xhrTimingCumulativeMillisecondsOther);
				}
			}
		}
	}
}

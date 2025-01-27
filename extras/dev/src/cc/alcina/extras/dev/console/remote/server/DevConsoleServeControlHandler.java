package cc.alcina.extras.dev.console.remote.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.util.MethodContext;

public class DevConsoleServeControlHandler extends AbstractHandler {
	DevConsoleRemote devConsoleRemote;

	public DevConsoleServeControlHandler(DevConsoleRemote devConsoleRemote) {
		this.devConsoleRemote = devConsoleRemote;
	}

	enum Action {
		stop, set_property
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		Action action = CommonUtils.getEnumValueOrNull(Action.class,
				request.getParameter("action"), true, null);
		switch (action) {
		case stop:
			getMessage(response);
			break;
		case set_property:
			set_property(request, response);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		baseRequest.setHandled(true);
	}

	void set_property(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String key = request.getParameter("key");
		String value = request.getParameter("value");
		String before = MethodContext.instance()
				.withContextTrue(Configuration.CONTEXT_SUPPRESS_WARN_MISSING)
				.call(() -> Configuration.get(key));
		Configuration.properties.set(key, value);
		String message = Ax.format("config :: %s :: '%s' -> '%s'", key, before,
				value);
		response.getWriter().write(message);
		response.setStatus(HttpServletResponse.SC_OK);
		logger.info(message);
	}

	void getMessage(HttpServletResponse response) throws IOException {
		String message = "action: stop -- stopping console [10ms delay]";
		response.getWriter().write(message);
		response.setStatus(HttpServletResponse.SC_OK);
		logger.info(message);
		new Thread("control-exit") {
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}

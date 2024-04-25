package cc.alcina.extras.dev.console.remote.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevConsoleServeControlHandler extends AbstractHandler {
	DevConsoleRemote devConsoleRemote;

	public DevConsoleServeControlHandler(DevConsoleRemote devConsoleRemote) {
		this.devConsoleRemote = devConsoleRemote;
	}

	enum Action {
		stop
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		Action action = Action.valueOf(request.getParameter("action"));
		switch (action) {
		case stop:
			String message = "action: stop -- stopping console [10ms delay]";
			response.getWriter().write(message);
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
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
			break;
		}
	}
}

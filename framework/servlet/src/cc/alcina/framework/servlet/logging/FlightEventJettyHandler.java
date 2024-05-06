package cc.alcina.framework.servlet.logging;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.entity.Configuration;

public class FlightEventJettyHandler extends AbstractHandler {
	FlightEventHandler handler;

	public FlightEventJettyHandler() {
		handler = new FlightEventHandler(
				Configuration.get(FlightEventRecorder.class, "path"));
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			handler.handleRequest(request, response);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		baseRequest.setHandled(true);
	}
}
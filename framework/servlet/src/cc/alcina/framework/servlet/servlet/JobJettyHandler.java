package cc.alcina.framework.servlet.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.util.LooseContext;

public class JobJettyHandler extends AbstractHandler {
	public JobJettyHandler() {
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			LooseContext.push();
			TransactionEnvironment.get().ensureBegun();
			new JobHandler().handleRequest(request, response);
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			baseRequest.setHandled(true);
			TransactionEnvironment.get().ensureEnded();
			LooseContext.pop();
		}
	}
}
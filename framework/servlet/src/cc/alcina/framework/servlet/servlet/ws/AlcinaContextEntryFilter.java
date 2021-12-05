package cc.alcina.framework.servlet.servlet.ws;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

@Provider
public class AlcinaContextEntryFilter implements ContainerRequestFilter {
	@Context
	private HttpServletRequest httpRequest;

	@Context
	private HttpServletResponse httpResponse;

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		String threadName = Ax.format("%s::%s",
				Thread.currentThread().getName(), httpRequest.getRequestURI());
		// Create a new context
		AlcinaServletContext alcinaContext = new AlcinaServletContext();
		alcinaContext.begin(httpRequest, httpResponse, threadName);
	}
}

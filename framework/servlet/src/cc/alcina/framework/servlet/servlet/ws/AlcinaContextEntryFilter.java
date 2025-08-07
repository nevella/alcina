package cc.alcina.framework.servlet.servlet.ws;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
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

	@Context
	private ResourceInfo resourceInfo;

	private AtomicInteger requestCounter = new AtomicInteger(0);

	static final String ATTR_REQUEST_START = AlcinaContextEntryFilter.class
			.getName() + "ATTR_REQUEST_START";

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		httpRequest.setAttribute(ATTR_REQUEST_START,
				System.currentTimeMillis());
		if (AlcinaServletContext.checkRefusing(httpRequest, httpResponse)) {
			return;
		}
		String resourceName = resourceInfo.getResourceClass().getSimpleName();
		String methodName = resourceInfo.getResourceMethod().getName();
		// Format the thread using the calling method's class and name
		// value and a unique(ish) integer
		String threadName = Ax.format("rpc::%s/%s-%s", resourceName, methodName,
				requestCounter.incrementAndGet());
		// Create a new context
		AlcinaServletContext alcinaContext = new AlcinaServletContext()
				.withRootPermissions(shouldRunWithRootPermissions());
		alcinaContext.begin(httpRequest, httpResponse, threadName);
	}

	private boolean shouldRunWithRootPermissions() {
		return resourceInfo.getResourceMethod()
				.getAnnotation(RootPermissions.class) != null;
	}

	public static long getRequestStart(HttpServletRequest httpRequest) {
		return (Long) httpRequest.getAttribute(ATTR_REQUEST_START);
	}
}

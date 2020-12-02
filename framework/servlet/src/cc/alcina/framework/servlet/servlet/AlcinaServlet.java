package cc.alcina.framework.servlet.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public abstract class AlcinaServlet extends HttpServlet {
	private static Topic<Throwable> topicApplicationThrowables = Topic.local();

	public static Topic<Throwable> topicApplicationThrowables() {
		return topicApplicationThrowables;
	}

	private AlcinaServletContext alcinaContext;

	Logger logger = LoggerFactory.getLogger(getClass());

	private AtomicInteger callCounter = new AtomicInteger(0);

	public AlcinaServlet() {
		this.alcinaContext = new AlcinaServletContext()
				.withRootPermissions(isRunWithRootPermissions());
	}

	public void writeAndClose(String s, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/plain");
		response.getWriter().write(s);
		response.getWriter().close();
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		wrapRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		wrapRequest(request, response);
	}

	protected abstract void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception;

	protected boolean isRunWithRootPermissions() {
		return false;
	}

	protected Boolean parseBooleanParam(HttpServletRequest request,
			String param) {
		try {
			return Boolean.parseBoolean(request.getParameter(param));
		} catch (Exception e) {
			return null;
		}
	}

	protected Integer parseIntegerParam(HttpServletRequest request,
			String param) {
		try {
			return Integer.parseInt(request.getParameter(param));
		} catch (Exception e) {
			return null;
		}
	}

	protected Long parseLongParam(HttpServletRequest request, String param) {
		try {
			return Long.parseLong(request.getParameter(param));
		} catch (Exception e) {
			return null;
		}
	}

	protected boolean trackMetrics() {
		return false;
	}

	protected void wrapRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		try {
			String threadName = Ax.format("task-%s:%s",
					getClass().getSimpleName(), callCounter.incrementAndGet());
			alcinaContext.begin(request, response, threadName);
			if (trackMetrics()) {
				MetricLogging.get().start(getClass().getSimpleName());
				InternalMetrics.get().startTracker(request,
						() -> Ax.format("Alcina servlet %s",
								getClass().getSimpleName()),
						InternalMetricTypeAlcina.servlet,
						Thread.currentThread().getName(), () -> true);
			}
			handleRequest(request, response);
		} catch (Throwable t) {
			topicApplicationThrowables().publish(t);
			logger.warn("Alcina servlet request issue - user {} - url {}",
					PermissionsManager.get().getUser().toIdNameString(),
					request.getRequestURI());
			EntityLayerLogging.persistentLog(LogMessageType.RPC_EXCEPTION, t);
			try {
				writeTextResponse(response, "Sorry, that's a 500");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		} finally {
			if (trackMetrics()) {
				InternalMetrics.get().endTracker(request);
				MetricLogging.get().end(getClass().getSimpleName());
			}
			alcinaContext.end();
		}
	}

	protected void writeHtmlResponse(HttpServletResponse response,
			String string) throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			response.setContentType("text/html");
			response.getWriter().write(string);
		}
	}

	protected void writeTextResponse(HttpServletResponse response,
			String string) throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			response.setContentType("text/plain");
			response.getWriter().write(string);
		}
	}
}

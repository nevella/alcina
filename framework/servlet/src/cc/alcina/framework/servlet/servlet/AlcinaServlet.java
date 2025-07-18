package cc.alcina.framework.servlet.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public abstract class AlcinaServlet extends HttpServlet
		implements HttpWriteUtils {
	private static final List<String> IGNORABLE_IO_EXCEPTIONS = Arrays.asList(
			"Connection reset by peer", "Connection timed out", "Broken pipe");

	private static Topic<Throwable> topicApplicationThrowables = Topic.create();

	public static final Topic<Throwable> topicApplicationThrowables() {
		return topicApplicationThrowables;
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private AtomicInteger callCounter = new AtomicInteger(0);

	public AlcinaServlet() {
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

	protected Permission getRequiredPermission() {
		return Permission.SimplePermissions.getPermission(AccessLevel.EVERYONE);
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

	protected boolean throwDetailedExceptions() {
		return false;
	}

	protected boolean trackMetrics() {
		return false;
	}

	protected void wrapRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		if (AlcinaServletContext.checkRefusing(request, response)) {
			return;
		}
		AlcinaServletContext alcinaContext = null;
		try {
			String threadName = Ax.format("task-%s:%s",
					getClass().getSimpleName(), callCounter.incrementAndGet());
			alcinaContext = new AlcinaServletContext()
					.withRootPermissions(isRunWithRootPermissions());
			alcinaContext.begin(request, response, threadName);
			if (trackMetrics()) {
				MetricLogging.get().start(getClass().getSimpleName());
				InternalMetrics.get().startTracker(request,
						() -> Ax.format("Alcina servlet %s",
								getClass().getSimpleName()),
						InternalMetricTypeAlcina.servlet,
						Thread.currentThread().getName(), () -> true);
			}
			String externalAuthorizationUrl = AuthenticationManager.get()
					.getExternalAuthorizationUrl(getRequiredPermission());
			if (externalAuthorizationUrl == null) {
				handleRequest(request, response);
			} else {
				response.sendRedirect(externalAuthorizationUrl);
			}
		} catch (Throwable t) {
			logger.warn("Alcina servlet request issue - user {} - url {}",
					Permissions.get().getUser().toIdNameString(),
					request.getRequestURI());
			// If the connection has been reset, we can't print anything to the
			// response
			if (t instanceof IOException
					&& IGNORABLE_IO_EXCEPTIONS.contains(t.getMessage())) {
				logger.warn("IOException: {}", t.getMessage());
				return;
			}
			topicApplicationThrowables().publish(t);
			logger.warn("Exception detail:", t);
			EntityLayerLogging.persistentLog(LogMessageType.RPC_EXCEPTION, t);
			try {
				response.setStatus(500);
				writeTextResponse(response,
						throwDetailedExceptions()
								? SEUtilities.getFullExceptionMessage(t)
								: "Sorry, that's a 500");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		} finally {
			try {
				if (trackMetrics()) {
					InternalMetrics.get().endTracker(request);
					MetricLogging.get().end(getClass().getSimpleName());
				}
			} finally {
				if (alcinaContext != null) {
					alcinaContext.end();
				}
			}
		}
	}
}

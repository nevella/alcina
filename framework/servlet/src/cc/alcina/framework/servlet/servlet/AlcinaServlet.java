package cc.alcina.framework.servlet.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class AlcinaServlet extends HttpServlet {
	private AlcinaServletContext alcinaContext;

	Logger logger = LoggerFactory.getLogger(getClass());

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

	protected void wrapRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		try {
			alcinaContext.begin(request, response);
			handleRequest(request, response);
		} catch (Exception e) {
			logger.warn("Alcina servlet request issue - user {} - url {}",
					PermissionsManager.get().getUser().toIdNameString(),
					request.getRequestURI());
			throw new ServletException(e);
		} finally {
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

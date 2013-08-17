package cc.alcina.framework.servlet.servlet.control;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.entity.ResourceUtilities;

public class ControlServlet extends HttpServlet {
	public class InformException extends Exception {
		public InformException(String message) {
			super(message);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			doGet0(req, resp);
		} catch (Exception e) {
			if (e instanceof InformException) {
				writeAndClose(e.getMessage(), resp);
			}
			throw new ServletException(e);
		}
	}

	public void writeAndClose(String s, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().write(s);
		resp.getWriter().close();
	}

	private void doGet0(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		String apiKey = getApiKey();
		authenticate(req, req.getParameter("apiKey"), apiKey);
	}

	private void authenticate(HttpServletRequest req, String reqApiKey,
			String appApiKey) throws Exception {
		if (appApiKey.isEmpty()) {
			throw new InformException("Api key not set");
		}
		if (!appApiKey.equals(reqApiKey)) {
			throw new InformException("Invalid api key");
		}
	}

	protected String getApiKey() {
		return ResourceUtilities.getBundledString(ControlServlet.class,
				"apiKey");
	}
}

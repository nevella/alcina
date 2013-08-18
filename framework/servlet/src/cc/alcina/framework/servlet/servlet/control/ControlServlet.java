package cc.alcina.framework.servlet.servlet.control;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.servlet.ServletLayerRegistry;
import cc.alcina.framework.servlet.servlet.AppLifecycleManager;

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
		ControlServletRequest csr = parseRequest(req, resp);
		handleRequest(csr, resp);
	}

	private ControlServletRequest parseRequest(HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		String jsonPayload = req.getParameter("json");
		if (jsonPayload != null) {
			ControlServletRequest csr = new AlcinaBeanSerializerS()
					.deserialize(jsonPayload, getClass().getClassLoader());
			csr.setJson(true);
			return csr;
		}
		String cmd = CommonUtils.nullToEmpty(req.getParameter("cmd"));
		ControlServletRequest csr = new ControlServletRequest();
		if (cmd.equals("refresh-config")) {
			csr.setCommand(ControlServletRequestCommand.REFRESH_CONFIG);
			return csr;
		} else if (cmd.equals("get-status")) {
			csr.setCommand(ControlServletRequestCommand.GET_STATUS);
			return csr;
		}
		writeAndClose(
				"Usage:\n"
						+ "control.do?apiKey=xxx&"
						+ "{json=yyy|cmd=[refresh-config|to-reader|to-writer|get-status]}",
				resp);
		return null;
	}

	private void handleRequest(ControlServletRequest csr,
			HttpServletResponse resp) throws Exception {
		if (csr.getCommand() == null) {
			return;
		}
		switch (csr.getCommand()) {
		case REFRESH_CONFIG:
			ServletLayerRegistry.impl(AppLifecycleManager.class).refreshProperties();
			writeAndClose("Properties refreshed", resp);
			break;
		case GET_STATUS:
			ControlServletStatus status = ServletLayerRegistry.impl(
					AppLifecycleManager.class).getStatus();
			if (csr.isJson()) {
				writeAndClose(new AlcinaBeanSerializerS().serialize(status),
						resp);
			} else {
				writeAndClose(status.toString(), resp);
			}
			break;
		}
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

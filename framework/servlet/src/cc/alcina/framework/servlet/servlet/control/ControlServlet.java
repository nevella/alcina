package cc.alcina.framework.servlet.servlet.control;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_EMAIL;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase.TestContentRequest;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.ResourceUtilities.SimpleQuery;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.control.ClusterStateProvider;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.publication.PublicationContext;
import cc.alcina.framework.servlet.publication.delivery.ContentDelivery;
import cc.alcina.framework.servlet.publication.delivery.ContentDeliveryEmail;
import cc.alcina.framework.servlet.servlet.AlcinaServlet;

public class ControlServlet extends AlcinaServlet {
	public static String createActionUrl(RemoteAction action) {
		StringMap queryParameters = new StringMap();
		queryParameters.put("cmd", "perform-action");
		queryParameters.put("apiKey", getApiKey());
		queryParameters.put("actionClassName", action.getClass().getName());
		queryParameters.put("actionJson",
				JacksonUtils.serializeWithDefaultsAndTypes(action));
		UrlBuilder urlBuilder = new UrlBuilder();
		urlBuilder.path("/control.do");
		queryParameters.forEach((k, v) -> urlBuilder.qsParam(k, v));
		return urlBuilder.build();
	}

	public static String invokeRemoteAction(RemoteAction action, String url,
			String apiKey) {
		StringMap queryParameters = new StringMap();
		queryParameters.put("cmd", "perform-action");
		queryParameters.put("apiKey", apiKey);
		queryParameters.put("actionClassName", action.getClass().getName());
		queryParameters.put("actionJson",
				JacksonUtils.serializeWithDefaultsAndTypes(action));
		try {
			return new SimpleQuery(url)
					.withQueryStringParameters(queryParameters).asString();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static String getApiKey() {
		return Registry.impl(AppLifecycleManager.class).getState().getApiKey();
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	public void writeAndCloseHtml(String s, HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		String host = req.getRequestURL().toString()
				.replaceFirst("https?://(.+?)/.+", "$1");
		String html = String.format(
				"<html><head><style>body{font-family:courier;white-space:pre;font-size:13px}</style><title>%s control servlet</title></head><body>%s</body></html>",
				host, StringEscapeUtils.escapeHtml(s));
		resp.getWriter().write(html);
		resp.getWriter().close();
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

	private void handle0(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		try {
			String apiKey = getApiKey();
			authenticate(req, req.getParameter("apiKey"), apiKey);
			ControlServletRequest csr = parseRequest(req, resp);
			handle1(csr, req, resp);
		} catch (Exception e) {
			e.printStackTrace();
			writeAndClose(SEUtilities.getFullExceptionMessage(e), resp);
		}
	}

	private void handle1(ControlServletRequest csr, HttpServletRequest req,
			HttpServletResponse response) throws Exception {
		if (csr.getCommand() == null) {
			return;
		}
		switch (csr.getCommand()) {
		case REFRESH_CONFIG:
			Registry.impl(AppLifecycleManager.class).refreshProperties();
			writeAndClose(
					String.format("Properties refreshed - %s", new Date()),
					response);
			break;
		case GET_STATUS:
			ControlServletState status = Registry
					.impl(AppLifecycleManager.class).getState();
			if (csr.isJson()) {
				writeAndClose(new AlcinaBeanSerializerS().serialize(status),
						response);
			} else {
				String msg = status.toString();
				msg += "\n";
				msg += Registry.impl(AppLifecycleManager.class)
						.getLifecycleServlet().dumpCustomProperties();
				writeAndClose(msg, response);
			}
			break;
		case CLUSTER_STATUS:
			writeAndCloseHtml(Registry.impl(ClusterStateProvider.class)
					.getMemberClusterState(), req, response);
			break;
		case CLUSTER_LEADER:
			writeAndCloseHtml(Registry.impl(ClusterStateProvider.class)
					.getClusterLeaderState(), req, response);
			break;
		case VM_HEALTH:
			writeAndClose(
					Registry.impl(ClusterStateProvider.class).getVmHealth(),
					response);
			break;
		case TEST_SENDMAIL: {
			String toAddress = testSendmail();
			String message = Ax.format(
					"Test email sent to: %s from: %s via: %s", toAddress,
					EntityLayerUtils.getLocalHostName(), ResourceUtilities
							.get(ContentDeliveryEmail.class, "smtp.host.name"));
			logger.warn(message);
			writeAndClose(message, response);
			break;
		}
		case PERFORM_ACTION: {
			String message = performAction(req);
			message = Ax.blankTo(message, "<No log>");
			logger.info(message);
			String regex = "(?s).*(<\\?xml|<html.*)";
			if (message.matches(regex)) {
				response.setContentType("text/html");
				response.getWriter().write(message.replaceFirst(regex, "$1"));
				response.getWriter().close();
			} else {
				writeAndClose(message, response);
			}
			break;
		}
		}
	}

	private ControlServletRequest parseRequest(HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		String jsonPayload = req.getParameter("json");
		if (jsonPayload != null) {
			ControlServletRequest csr = new AlcinaBeanSerializerS()
					.deserialize(jsonPayload);
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
		} else if (cmd.equals("cluster-status")) {
			csr.setCommand(ControlServletRequestCommand.CLUSTER_STATUS);
			return csr;
		} else if (cmd.equals("cluster-leader")) {
			csr.setCommand(ControlServletRequestCommand.CLUSTER_LEADER);
			return csr;
		} else if (cmd.equals("vm-health")) {
			csr.setCommand(ControlServletRequestCommand.VM_HEALTH);
			return csr;
		} else if (cmd.equals("test-sendmail")) {
			csr.setCommand(ControlServletRequestCommand.TEST_SENDMAIL);
			return csr;
		} else if (cmd.equals("perform-action")) {
			csr.setCommand(ControlServletRequestCommand.PERFORM_ACTION);
			return csr;
		}
		writeAndClose("Usage:\n" + "control.do?apiKey=xxx&"
				+ "{json=yyy|cmd=[refresh-config|to-reader|to-writer|get-status|vm-health|test-sendmail|cluster-leader]}",
				resp);
		return null;
	}

	private String performAction(HttpServletRequest req) {
		if (Ax.notBlank(req.getHeader("X-Forwarded-Server"))) {
			throw new RuntimeException("Internal/non-proxied access only");
		}
		String actionClassName = req.getParameter("actionClassName");
		String actionJson = req.getParameter("actionJson");
		return ThreadedPermissionsManager.cast()
				.callWithPushedSystemUserIfNeededNoThrow(() -> {
					RemoteAction action = (RemoteAction) JacksonUtils
							.deserialize(actionJson,
									Class.forName(actionClassName));
					return JobRegistry.get().perform(action).getActionLog();
				});
	}

	private String testSendmail() throws Exception {
		ContentDelivery deliverer = (ContentDelivery) Registry.get()
				.instantiateSingle(ContentDeliveryType.class,
						ContentDeliveryType_EMAIL.class);
		TestContentRequest testContentRequest = new TestContentRequest();
		testContentRequest.setEmailInline(true);
		testContentRequest.setEmailSubject(Ax.format("Test: %s :: %s",
				EntityLayerUtils.getLocalHostName(), new Date()));
		String emailAddress = ResourceUtilities.get("testSendmailAddress");
		testContentRequest.setEmailAddress(emailAddress);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				"test".getBytes(StandardCharsets.UTF_8));
		String token = deliverer.deliver(new PublicationContext(), inputStream,
				testContentRequest, null);
		return emailAddress;
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			handle0(request, response);
		} catch (Exception e) {
			if (e instanceof InformException) {
				writeAndClose(e.getMessage(), response);
			}
			throw new ServletException(e);
		}
	}

	public static interface ControlServletActionPerformer {
		String performAction(String actionClassName, String actionParameters);
	}

	public class InformException extends Exception {
		public InformException(String message) {
			super(message);
		}
	}
}

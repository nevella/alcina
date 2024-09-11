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
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_EMAIL;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase.TestContentRequest;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.entity.control.ClusterStateProvider;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.publication.PublicationContext;
import cc.alcina.framework.servlet.publication.delivery.ContentDelivery;
import cc.alcina.framework.servlet.publication.delivery.ContentDeliveryEmail;
import cc.alcina.framework.servlet.servlet.AlcinaServlet;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;

// FIXME - API - use a jax-ws endpoint, ensure internal-only
public class ControlServlet extends AlcinaServlet {
	public static final String CONTEXT_HTTP_REQUEST_USE_GZIP = ControlServlet.class
			.getName() + ".CONTEXT_HTTP_REQUEST_USE_GZIP";

	public static String createTaskUrl(Task task) {
		StringMap queryParameters = new StringMap();
		queryParameters.put("cmd", "perform-task");
		queryParameters.put("apiKey", getApiKey());
		queryParameters.put("taskClassName", task.getClass().getName());
		queryParameters.put("taskJson",
				JacksonUtils.serializeWithDefaultsAndTypes(task));
		UrlBuilder urlBuilder = new UrlBuilder();
		urlBuilder.path("/control.do");
		queryParameters.forEach((k, v) -> urlBuilder.qsParam(k, v));
		return urlBuilder.build();
	}

	public static String getApiKey() {
		return Configuration.get("apiKey");
	}

	public static String invokeTask(Task task, String url, String apiKey) {
		return invokeTask(task, url, apiKey, TaskExecutionType.WAIT_RETURN_LOG);
	}

	public static String invokeTask(Task task, String url, String apiKey,
			TaskExecutionType executionType) {
		StringMap queryParameters = new StringMap();
		queryParameters.put("cmd", "perform-task");
		queryParameters.put("executionType", Ax.friendly(executionType));
		queryParameters.put("apiKey", apiKey);
		queryParameters.put("taskClassName", task.getClass().getName());
		queryParameters.put("taskJson",
				JacksonUtils.serializeWithDefaultsAndTypes(task));
		boolean gzip = LooseContext.is(CONTEXT_HTTP_REQUEST_USE_GZIP);
		try {
			return new SimpleHttp(url).withGzip(gzip).withDecodeGz(gzip)
					.withQueryStringParameters(queryParameters).asString();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	Logger logger = LoggerFactory.getLogger(getClass());

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
			Registry.impl(AppLifecycleServletBase.class).loadCustomProperties();
			writeAndClose(
					String.format("Properties refreshed - %s", new Date()),
					response);
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
			MetricLogging.get().start("vm-health");
			writeAndClose(
					Registry.impl(ClusterStateProvider.class).getVmHealth(),
					response);
			MetricLogging.get().end("vm-health");
			break;
		case TEST_SENDMAIL: {
			String toAddress = testSendmail();
			String message = Ax.format(
					"Test email sent to: %s from: %s via: %s", toAddress,
					EntityLayerUtils.getLocalHostName(), Configuration
							.get(ContentDeliveryEmail.class, "smtp.host.name"));
			logger.warn(message);
			writeAndClose(message, response);
			break;
		}
		case PERFORM_TASK: {
			TaskExecutionType executionType = CommonUtils.getEnumValueOrNull(
					TaskExecutionType.class, req.getParameter("executionType"),
					true, null);
			String message = invokeTask(req, executionType);
			message = Ax.blankTo(message, "<No log>");
			String trimmedMessage = CommonUtils.trimToWsChars(message, 5000);
			logger.info(trimmedMessage);
			// no regex, non-performant
			String lcMessage = message.toLowerCase();
			int trimStart = lcMessage.indexOf("<?xml");
			if (trimStart == -1) {
				trimStart = lcMessage.indexOf("<html");
			}
			if (trimStart != -1) {
				response.setContentType("text/html");
				response.getWriter().write(message.substring(trimStart));
				response.getWriter().close();
			} else {
				writeAndClose(message, response);
			}
			break;
		}
		}
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

	private String invokeTask(HttpServletRequest req,
			TaskExecutionType executionType) {
		if (Ax.notBlank(req.getHeader("X-Forwarded-Server"))) {
			throw new RuntimeException("Internal/non-proxied access only");
		}
		String taskClassName = req.getParameter("taskClassName");
		String taskJson = req.getParameter("taskJson");
		return MethodContext.instance()
				// FIXME - instead, use a threaded topic listener (in the
				// context)
				.withContextTrue(
						JobRegistry.CONTEXT_LAUNCHED_FROM_CONTROL_SERVLET)
				.withRootPermissions(true).call(() -> {
					Task task = null;
					if (taskJson == null) {
						task = (Task) Reflections
								.newInstance(Class.forName(taskClassName));
					} else {
						task = (Task) JacksonUtils.deserialize(taskJson,
								Class.forName(taskClassName));
					}
					switch (executionType) {
					case SCHEDULE_RETURN_ID: {
						Job job = task.schedule();
						Transaction.commit();
						return String.valueOf(job.getId());
					}
					case WAIT_RETURN_LOG: {
						Job job = task.perform();
						return job.getLog();
					}
					case WAIT_RETURN_ID: {
						Job job = task.perform();
						return String.valueOf(job.getId());
					}
					case WAIT_RETURN_LARGE_OBJECT_SERIALIZED: {
						Job job = task.perform();
						return JobRegistry.get().getLargeResult(job);
					}
					default:
						throw new UnsupportedOperationException();
					}
				});
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
		} else if (cmd.equals("perform-task")) {
			csr.setCommand(ControlServletRequestCommand.PERFORM_TASK);
			return csr;
		} else if (cmd.equals("schedule-task")) {
			csr.setCommand(ControlServletRequestCommand.SCHEDULE_TASK);
			return csr;
		}
		writeAndClose("Usage:\n" + "control.do?apiKey=xxx&"
				+ "{json=yyy|cmd=[refresh-config|to-reader|to-writer|get-status|vm-health|test-sendmail|cluster-leader]}",
				resp);
		return null;
	}

	private String testSendmail() throws Exception {
		ContentDelivery deliverer = Registry.query(ContentDelivery.class)
				.setKeys(ContentDeliveryType.class,
						ContentDeliveryType_EMAIL.class)
				.impl();
		TestContentRequest testContentRequest = new TestContentRequest();
		testContentRequest.setEmailInline(true);
		testContentRequest.setEmailSubject(Ax.format("Test: %s :: %s",
				EntityLayerUtils.getLocalHostName(), new Date()));
		String emailAddress = Configuration.get("testSendmailAddress");
		testContentRequest.setEmailAddress(emailAddress);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				"test".getBytes(StandardCharsets.UTF_8));
		String token = deliverer.deliver(new PublicationContext(), inputStream,
				testContentRequest, null);
		return emailAddress;
	}

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

	public class InformException extends Exception {
		public InformException(String message) {
			super(message);
		}
	}

	public enum TaskExecutionType {
		WAIT_RETURN_LOG, WAIT_RETURN_ID,
		// requires recordLargeInMemoryResult
		WAIT_RETURN_LARGE_OBJECT_SERIALIZED, SCHEDULE_RETURN_ID;

		public static TaskExecutionType defaultForWait(boolean wait) {
			return wait ? WAIT_RETURN_LOG : SCHEDULE_RETURN_ID;
		}

		public boolean isWait() {
			return this != SCHEDULE_RETURN_ID;
		}
	}
}

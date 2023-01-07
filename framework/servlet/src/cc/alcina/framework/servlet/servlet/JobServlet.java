/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.servlet;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.task.ServletAwaitTask;
import cc.alcina.framework.servlet.task.TaskCancelJob;
import cc.alcina.framework.servlet.task.TaskListJobs;
import cc.alcina.framework.servlet.task.TaskLogJobDetails;
import cc.alcina.framework.servlet.task.TaskRunJob;
import cc.alcina.framework.servlet.task.TaskWakeupJobScheduler;

/**
 *
 * @author Nick Reddel
 *
 *         FIXME - jobs - add 'rerun'
 */
public class JobServlet extends AlcinaServlet {
	public static String createTaskUrl(String host, Task task) {
		StringMap queryParameters = new StringMap();
		if (task instanceof TaskLogJobDetails) {
			queryParameters.put("action", "detail");
			queryParameters.put("id", ((TaskLogJobDetails) task).value);
		} else if (task instanceof TaskCancelJob) {
			queryParameters.put("action", "cancel");
			queryParameters.put("id", ((TaskCancelJob) task).value);
		} else if (task instanceof TaskRunJob) {
			queryParameters.put("action", "run");
			queryParameters.put("id", ((TaskRunJob) task).value);
		} else {
			queryParameters.put("action", "task");
			String serialized = TransformManager.serialize(task);
			String defaultSerialized = TransformManager
					.serialize(Reflections.newInstance(task.getClass()));
			queryParameters.put("task",
					defaultSerialized.equals(serialized)
							? task.getClass().getName()
							: serialized);
		}
		UrlBuilder urlBuilder = new UrlBuilder();
		urlBuilder.path("/job.do");
		urlBuilder.host(host);
		queryParameters.forEach((k, v) -> urlBuilder.qsParam(k, v));
		return urlBuilder.build();
	}

	public static String createTaskUrl(Task task) {
		return createTaskUrl(null, task);
	}

	public static long invokeAsSystemUser(String taskUrl) {
		taskUrl += "&return_job_id=true";
		SimpleHttp query = new SimpleHttp(taskUrl);
		StringMap headers = new StringMap();
		ClientInstance clientInstance = EntityLayerObjects.get()
				.getServerAsClientInstance();
		headers.put(
				AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY,
				String.valueOf(clientInstance.getId()));
		headers.put(
				AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY,
				String.valueOf(clientInstance.getAuth()));
		try {
			return Long.parseLong(query.withHeaders(headers).asString());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	protected Permission getRequiredPermission() {
		return Permission.SimplePermissions.getPermission(AccessLevel.ADMIN);
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (!PermissionsManager.get().isAdmin()) {
			writeTextResponse(response, "Not authorised");
			return;
		}
		Action action = Action
				.valueOf(Ax.blankTo(request.getParameter("action"), "list"));
		String id = request.getParameter("id");
		String filter = request.getParameter("filter");
		boolean returnJobId = Objects
				.equals(request.getParameter("return_job_id"), "true");
		Job job = null;
		boolean outputAsHtml = false;
		switch (action) {
		case list:
			TaskListJobs logJobs = new TaskListJobs();
			logJobs.setFilter(filter);
			job = logJobs.perform();
			break;
		case detail:
			job = new TaskLogJobDetails().withValue(id).perform();
			break;
		case control_job:
			job = new TaskLogJobDetails()
					.withValue(String.valueOf(JobRegistry.get()
							.getLaunchedFromControlServlet().getId()))
					.perform();
			break;
		case cancel:
			job = new TaskCancelJob().withValue(id).perform();
			break;
		case run:
			job = new TaskRunJob().withValue(id).perform();
			break;
		case wakeup:
			job = new TaskWakeupJobScheduler().perform();
			break;
		case control:
			ServerControlAction serverControlAction = new ServerControlAction();
			serverControlAction.getParameters()
					.setPropertyName(request.getParameter("value"));
			job = serverControlAction.perform();
			break;
		case task:
			String serialized = request.getParameter("task");
			Task task = null;
			try {
				task = (Task) Reflections.newInstance(
						Class.forName(request.getParameter("task")));
			} catch (Exception e) {
				task = TransformManager.Serializer.get()
						.deserialize(serialized);
			}
			if (task instanceof ServletAwaitTask
					&& ((ServletAwaitTask) task).isAwaitJobCompletion()) {
				job = task.perform();
			} else {
				job = task.schedule();
				Transaction.commit();
				if (returnJobId) {
					response.setContentType("text/plain");
					response.getWriter().write(String.valueOf(job.getId()));
					response.flushBuffer();
				} else {
					String href = createTaskUrl(new TaskLogJobDetails()
							.withValue(String.valueOf(job.getId())));
					outputAsHtml = task instanceof TaskWithHtmlResult;
					response.setContentType(
							outputAsHtml ? "text/html" : "text/plain");
					response.getWriter()
							.write(Ax.format("Job started:\n%s\n", href));
					response.flushBuffer();
				}
				JobRegistry.get().await(job);
			}
			break;
		}
		job = job.domain().ensurePopulated();
		if (job.getResultType().isFail() || job.getLargeResult() == null) {
			String message = Ax.blankTo(job.getLog(),
					Ax.format("Job %s - complete", job));
			if (Ax.matches(request.getHeader("User-Agent"), ".*Mozilla.*")
					&& outputAsHtml) {
				DomDocument doc = DomDocument.basicHtmlDoc();
				doc.html().head().builder().tag("title")
						.text(Ax.format("Job - %s",
								job.getTask().getClass().getSimpleName()))
						.append();
				doc.html().body().builder().tag("div")
						.attr("style", "font-family:monospace").text(message)
						.append();
				writeHtmlResponse(response, doc.prettyToString());
			} else {
				writeTextResponse(response, message);
			}
		} else {
			writeHtmlResponse(response, job.getLargeResult().toString());
		}
	}

	@Override
	protected boolean trackMetrics() {
		return true;
	}

	enum Action {
		list, cancel, detail, wakeup, task, run, control, control_job
	}
}

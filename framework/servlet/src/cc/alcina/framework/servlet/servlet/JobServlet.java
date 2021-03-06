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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.task.TaskCancelJob;
import cc.alcina.framework.servlet.task.TaskListJobs;
import cc.alcina.framework.servlet.task.TaskLogJobDetails;
import cc.alcina.framework.servlet.task.TaskRunJob;
import cc.alcina.framework.servlet.task.TaskWakeupJobScheduler;

/**
 *
 * @author Nick Reddel
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
		Job job = null;
		switch (action) {
		case list:
			TaskListJobs logJobs = new TaskListJobs();
			logJobs.setFilter(filter);
			job = logJobs.perform();
			break;
		case detail:
			job = new TaskLogJobDetails().withValue(id).perform();
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
			job = task.schedule();
			Transaction.commit();
			String href = createTaskUrl(new TaskLogJobDetails()
					.withValue(String.valueOf(job.getId())));
			response.setContentType("text/plain");
			response.getWriter().write(Ax.format("Job started:\n%s\n", href));
			response.flushBuffer();
			JobRegistry.get().await(job);
			break;
		}
		job = Domain.find(job);
		if (job.getResultType().isFail() || job.getLargeResult() == null) {
			writeTextResponse(response, Ax.blankTo(job.getLog(),
					Ax.format("Job %s - complete", job)));
		} else {
			writeHtmlResponse(response, job.getLargeResult().toString());
		}
	}

	@Override
	protected boolean trackMetrics() {
		return true;
	}

	enum Action {
		list, cancel, detail, wakeup, task, run, control
	}
}

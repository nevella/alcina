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

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.task.TaskCancelJob;
import cc.alcina.framework.servlet.task.TaskLogJobDetails;
import cc.alcina.framework.servlet.task.TaskLogJobs;
import cc.alcina.framework.servlet.task.TaskWakeupJobScheduler;

/**
 *
 * @author Nick Reddel
 */
public class JobServlet extends AlcinaServlet {
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
			TaskLogJobs logJobs = new TaskLogJobs();
			logJobs.setFilter(filter);
			logJobs.setUseDefaultFilter(!Objects.equals("false",
					request.getParameter("useDefaultFilter")));
			job = logJobs.perform();
			break;
		case detail:
			job = new TaskLogJobDetails().withValue(id).perform();
			break;
		case cancel:
			job = new TaskCancelJob().withValue(id).perform();
			break;
		case wakeup:
			job = new TaskWakeupJobScheduler().withValue(id).perform();
			break;
		case task:
			Task task = (Task) Reflections
					.newInstance(Class.forName(request.getParameter("task")));
			job = JobRegistry.get().perform(task);
			break;
		}
		if (job.getResultType().isFail()) {
			writeTextResponse(response, job.getLog());
		} else {
			writeHtmlResponse(response, job.getLog());
		}
	}

	@Override
	protected boolean trackMetrics() {
		return true;
	}

	enum Action {
		list, cancel, detail, wakeup, task
	}
}

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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.entity.logic.ServerClientInstance;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.task.TaskCancelJob;
import cc.alcina.framework.servlet.task.TaskLogJobDetails;
import cc.alcina.framework.servlet.task.TaskRunJob;

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
			{
				TaskLogJobDetails typed = (TaskLogJobDetails) task;
				queryParameters.put("action", "detail");
				queryParameters.put("id", String.valueOf(typed.getJobId()));
				if (typed.isDetails()) {
					queryParameters.put("details", Boolean.TRUE.toString());
				}
				if (!typed.isTruncateFields()) {
					queryParameters.put("truncateFields",
							Boolean.FALSE.toString());
				}
			}
		} else if (task instanceof TaskCancelJob) {
			queryParameters.put("action", "cancel");
			queryParameters.put("id",
					String.valueOf(((TaskCancelJob) task).getJobId()));
		} else if (task instanceof TaskRunJob) {
			queryParameters.put("action", "run");
			queryParameters.put("id",
					String.valueOf(((TaskRunJob) task).getJobId()));
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
		urlBuilder.withPath("/job.do");
		urlBuilder.withHost(host);
		queryParameters
				.forEach((k, v) -> urlBuilder.withQueryStringParam(k, v));
		return urlBuilder.build();
	}

	public static String createTaskUrl(Task task) {
		return createTaskUrl(null, task);
	}

	public static long invokeAsSystemUser(String taskUrl) {
		taskUrl += "&return_job_id=true";
		SimpleHttp query = new SimpleHttp(taskUrl);
		StringMap headers = new StringMap();
		ClientInstance clientInstance = ServerClientInstance.get();
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
		if (!Permissions.get().isAdmin()) {
			writeTextResponse(response, "Not authorised");
			return;
		}
		new JobHandler().handleRequest(request, response);
	}

	@Override
	protected boolean trackMetrics() {
		return true;
	}

	enum Action {
		list, cancel, detail, wakeup, task, run, control, control_job
	}
}

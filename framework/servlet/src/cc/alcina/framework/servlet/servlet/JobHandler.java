package cc.alcina.framework.servlet.servlet;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.servlet.JobServlet.Action;
import cc.alcina.framework.servlet.task.ServletAwaitTask;
import cc.alcina.framework.servlet.task.TaskCancelJob;
import cc.alcina.framework.servlet.task.TaskListJobs;
import cc.alcina.framework.servlet.task.TaskLogJobDetails;
import cc.alcina.framework.servlet.task.TaskRunJob;
import cc.alcina.framework.servlet.task.TaskWakeupJobScheduler;

public class JobHandler implements HttpWriteUtils {
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Action action = Action
				.valueOf(Ax.blankTo(request.getParameter("action"), "list"));
		String id = request.getParameter("id");
		boolean returnJobId = Objects
				.equals(request.getParameter("return_job_id"), "true");
		Job job = null;
		boolean outputAsHtml = false;
		switch (action) {
		case list:
			job = new TaskListJobs()
					.populateFromParameters(request.getParameterMap())
					.perform();
			break;
		case detail:
			job = new TaskLogJobDetails()
					.populateFromParameters(request.getParameterMap())
					.perform();
			break;
		case control_job:
			job = new TaskLogJobDetails().withJobId(
					JobRegistry.get().getLaunchedFromControlServlet().getId())
					.perform();
			break;
		case cancel:
			job = new TaskCancelJob().withJobId(Long.parseLong(id)).perform();
			break;
		case run:
			job = new TaskRunJob().withJobId(Long.parseLong(id)).perform();
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
					String href = JobServlet.createTaskUrl(
							new TaskLogJobDetails().withJobId(job.getId()));
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
}

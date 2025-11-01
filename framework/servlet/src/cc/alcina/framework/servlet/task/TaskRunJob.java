package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.servlet.JobServlet;

public class TaskRunJob extends PerformerTask {
	private long jobId;

	public long getJobId() {
		return this.jobId;
	}

	@Override
	public void run() throws Exception {
		Job job = Job.byId(jobId);
		if (job == null) {
			logger.info("Job {} does not exist", jobId);
		} else if (job.provideIsComplete()) {
			logger.info("Job {} already completed", jobId);
		} else {
			job.setRunAt(null);
			job.setPerformer(ClientInstance.current());
			job.setState(JobState.PENDING);
			String message = Ax.format("TaskRunJob - future-to-pending - %s",
					job);
			DomDocument doc = DomDocument.basicHtmlDoc();
			doc.html().head().builder().tag("title").text(Ax.format("Job - %s",
					job.getTask().getClass().getSimpleName())).append();
			DomNode div = doc.html().body().builder().tag("div").append();
			div.builder().tag("p").attr("style", "font-family:monospace")
					.text(message).append();
			DomNode p = div.builder().tag("p")
					.attr("style", "font-family:monospace").append();
			p.builder().tag("span").text("Job details: ").append();
			String id = String.valueOf(job.getId());
			Task task = new TaskLogJobDetails().withJobId(jobId);
			p.html().addLink(id, JobServlet.createTaskUrl(task), null);
			JobContext.setLargeResult(doc.toPrettyMarkup());
			Transaction.commit();
		}
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public TaskRunJob withJobId(long jobId) {
		this.jobId = jobId;
		return this;
	}
}

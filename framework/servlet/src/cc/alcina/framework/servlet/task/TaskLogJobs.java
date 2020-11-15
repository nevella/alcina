package cc.alcina.framework.servlet.task;

import java.util.List;

import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job2.JobRegistry;
import cc.alcina.framework.servlet.job2.JobRegistry.PendingStat;
import cc.alcina.framework.servlet.job2.JobRegistry.QueueStat;
import cc.alcina.framework.servlet.servlet.control.ControlServlet;

public class TaskLogJobs extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		DomDoc doc = DomDoc.basicHtmlDoc();
		{
			List<QueueStat> queues = JobRegistry.get().getActiveQueueStats();
			doc.html().body().builder().tag("h2").text("Active queues")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Name").cell("Active").cell("Pending")
					.cell("Total");
			queues.forEach(queue -> builder.row().cell(queue.name)
					.cell(queue.active).cell(queue.pending).cell(queue.total));
		}
		{
			List<PendingStat> pending = JobRegistry.get()
					.getPendingQueueStats();
			doc.html().body().builder().tag("h2").text("Pending queues")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Name").cell("Run at").cell("Task").cell("Id")
					.cell("Link");
			pending.forEach(stat -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(stat.name).cell(stat.runAt).cell(stat.taskName)
						.cell(stat.jobId);
				DomNode td = cellBuilder.append();
				ServerControlAction action = new ServerControlAction();
				action.getParameters()
						.setPropertyName(TaskCancelJob.class.getName());
				action.getParameters().setPropertyValue(stat.jobId);
				String href = ControlServlet.createActionUrl(action);
				td.html().addLink("Cancel", href, "_blank");
			});
		}
		{
			doc.html().body().builder().tag("h2")
					.text("Active and pending jobs").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Queue").cell("Started")
					.cell("Thread").cell("Performer").cell("Link");
			DomainDescriptorJob.get().getNotCompletedJobs()
					.filter(job -> Ax.isBlank(value)
							|| job.getTaskClassName().matches(value))
					.limit(30).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName()).cell(job.getQueue())
								.cell(job.getStartTime())
								.cell(JobRegistry.get()
										.getPerformerThreadName(job))
								.cell(job.getPerformer() == null ? "(null)"
										: job.getPerformer()
												.getAuthenticationSession()
												.getUser().toIdNameString());
						DomNode td = cellBuilder.append();
						ServerControlAction action = new ServerControlAction();
						action.getParameters().setPropertyName(
								TaskLogJobDetails.class.getName());
						action.getParameters()
								.setPropertyValue(String.valueOf(job.getId()));
						String href = ControlServlet.createActionUrl(action);
						td.html().addLink("Details", href, "_blank");
					});
		}
		{
			doc.html().body().builder().tag("h2")
					.text("Recently completed jobs").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Started")
					.cell("Finished").cell("Performer").cell("Link");
			DomainDescriptorJob.get().getRecentlyCompletedJobs()
					.filter(job -> Ax.isBlank(value)
							|| job.getTaskClassName().matches(value))
					.limit(30).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName())
								.cell(job.getStartTime()).cell(job.getEndTime())
								.cell(job.getPerformer() == null ? "(null)"
										: job.getPerformer()
												.getAuthenticationSession()
												.getUser().toIdNameString());
						DomNode td = cellBuilder.append();
						ServerControlAction action = new ServerControlAction();
						action.getParameters().setPropertyName(
								TaskLogJobDetails.class.getName());
						action.getParameters()
								.setPropertyValue(String.valueOf(job.getId()));
						String href = ControlServlet.createActionUrl(action);
						td.html().addLink("Details", href, "_blank");
					});
		}
		slf4jLogger.info(doc.prettyToString());
	}
}

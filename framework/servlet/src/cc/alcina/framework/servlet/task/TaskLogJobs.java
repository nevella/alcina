package cc.alcina.framework.servlet.task;

import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.PendingStat;
import cc.alcina.framework.servlet.job.JobRegistry.QueueStat;
import cc.alcina.framework.servlet.servlet.control.ControlServlet;

public class TaskLogJobs extends AbstractTaskPerformer {
	protected void addActiveAndPending(DomDoc doc, String sectionFilterName,
			Predicate<Job> sectionFilter, int limit) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active and pending jobs (%s)", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Queue").cell("Started")
					.cell("Thread").cell("Performer").cell("Links");
			DomainDescriptorJob.get().getNotCompletedJobs()
					.filter(job -> Ax.isBlank(value)
							|| job.getTaskClassName().matches(value))
					.filter(sectionFilter).limit(30).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName()).cell(job.getQueue())
								.cell(job.getStartTime())
								.cell(JobRegistry.get()
										.getPerformerThreadName(job))
								.cell(job.getPerformer());
						DomNode td = cellBuilder.append();
						{
							ServerControlAction action = new ServerControlAction();
							action.getParameters().setPropertyName(
									TaskLogJobDetails.class.getName());
							action.getParameters().setPropertyValue(
									String.valueOf(job.getId()));
							String href = ControlServlet
									.createActionUrl(action);
							td.html().addLink("Details", href, "_blank");
						}
						td.builder().text(" - ").tag("span").append();
						{
							ServerControlAction action = new ServerControlAction();
							action.getParameters().setPropertyName(
									TaskCancelJob.class.getName());
							action.getParameters().setPropertyValue(
									String.valueOf(job.getId()));
							String href = ControlServlet
									.createActionUrl(action);
							td.html().addLink("Cancel", href, "_blank");
						}
					});
		}
	}

	protected void addCompleted(DomDoc doc, String sectionFilterName,
			Predicate<Job> sectionFilter, int limit) {
		{
			doc.html().body().builder().tag("h2")
					.text("Recently completed jobs %s", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Started")
					.cell("Finished").cell("Performer").cell("Link");
			Predicate<Job> nameFilter = job -> Ax.isBlank(value)
					|| job.getTaskClassName().matches(value);
			DomainDescriptorJob.get().getRecentlyCompletedJobs()
					.filter(job -> Ax.isBlank(value)
							|| job.getTaskClassName().matches(value))
					.filter(sectionFilter).limit(limit).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName())
								.cell(job.getStartTime()).cell(job.getEndTime())
								.cell(job.getPerformer());
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
	}

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
					.cell("Completed").cell("Total");
			queues.forEach(queue -> builder.row().cell(queue.name)
					.cell(queue.active).cell(queue.pending)
					.cell(queue.completed).cell(queue.total));
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
		addActiveAndPending(doc, "top-level",
				job -> !job.provideParent().isPresent(), 20);
		addActiveAndPending(doc, "child",
				job -> job.provideParent().isPresent(), 20);
		addCompleted(doc, "top-level", job -> !job.provideParent().isPresent(),
				20);
		addCompleted(doc, "child", job -> job.provideParent().isPresent(), 20);
		slf4jLogger.info(doc.prettyToString());
	}
}

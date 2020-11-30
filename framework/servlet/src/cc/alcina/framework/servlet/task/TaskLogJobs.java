package cc.alcina.framework.servlet.task;

import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue.QueueStat;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.FutureStat;
import cc.alcina.framework.servlet.servlet.control.ControlServlet;

public class TaskLogJobs extends AbstractTaskPerformer {
	private void run1() throws Exception {
		DomDoc doc = DomDoc.basicHtmlDoc();
		{
			Stream<QueueStat> queues = JobRegistry.get().getActiveQueueStats();
			doc.html().body().builder().tag("h2")
					.text("Active allocation queues").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Name").cell("Started").cell("Active")
					.cell("Pending").cell("Completed").cell("Total");
			queues.filter(q -> q.active != 0)
					.forEach(queue -> builder.row().cell(queue.name)
							.cell(queue.startTime).cell(queue.active)
							.cell(queue.pending).cell(queue.completed)
							.cell(queue.total));
		}
		{
			Stream<FutureStat> pending = JobRegistry.get()
					.getFutureQueueStats();
			doc.html().body().builder().tag("h2").text("Pending queues")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Task").cell("Run at").cell("Id").cell("Link");
			pending.forEach(stat -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(stat.taskName).cell(stat.runAt).cell(stat.jobId);
				DomNode td = cellBuilder.append();
				ServerControlAction action = new ServerControlAction();
				action.getParameters()
						.setPropertyName(TaskCancelJob.class.getName());
				action.getParameters().setPropertyValue(stat.jobId);
				String href = ControlServlet.createActionUrl(action);
				td.html().addLink("Cancel", href, "_blank");
			});
		}
		addActive(doc, "top-level - active", Job::provideIsTopLevel);
		addActive(doc, "child - active", Job::provideIsNotTopLevel);
		addCompleted(doc, "top-level", Job::provideIsTopLevel, 20);
		addCompleted(doc, "child", Job::provideIsNotTopLevel, 20);
		slf4jLogger.info(doc.prettyToString());
	}

	protected void addActive(DomDoc doc, String sectionFilterName,
			Predicate<Job> sectionFilter) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active and pending jobs (%s)", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Started").cell("Thread")
					.cell("Performer").cell("Links");
			DomainDescriptorJob.get().getActiveJobs()
					.filter(job -> Ax.isBlank(value)
							|| job.getTaskClassName().matches(value))
					.filter(sectionFilter).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName())
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
		MethodContext.instance().withContextTrue(
				DomainStore.CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES)
				.run(this::run1);
	}
}

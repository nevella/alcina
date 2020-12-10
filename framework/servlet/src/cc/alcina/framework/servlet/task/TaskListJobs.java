package cc.alcina.framework.servlet.task;

import java.util.Date;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue.QueueStat;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.FutureStat;
import cc.alcina.framework.servlet.servlet.JobServlet;

public class TaskListJobs extends AbstractTaskPerformer {
	private String filter;

	private Pattern filterPattern;

	public String getFilter() {
		return this.filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	private DomNodeHtmlTableCellBuilder
			date(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("date");
		return builder;
	}

	private DomNodeHtmlTableCellBuilder
			instance(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("instance");
		return builder;
	}

	private DomNodeHtmlTableCellBuilder
			links(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("links");
		return builder;
	}

	private DomNodeHtmlTableCellBuilder
			numeric(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("numeric");
		return builder;
	}

	private void run1() throws Exception {
		DomDoc doc = DomDoc.basicHtmlDoc();
		String css = ResourceUtilities.readClazzp("res/TaskListJobs.css");
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		{
			Stream<QueueStat> queues = JobRegistry.get().getActiveQueueStats();
			doc.html().body().builder().tag("h2")
					.text("Active allocation queues").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(this::numeric).cell("Name")
					.accept(this::large).cell("Started").accept(this::date)
					.cell("Active").accept(this::date).cell("Pending")
					.accept(this::numeric).cell("Completed")
					.accept(this::numeric).cell("Total").accept(this::numeric);
			queues.filter(q -> q.active != 0).filter(q -> filter(q.name))
					.forEach(queue -> builder.row().cell(queue.jobId)
							.cell(queue.name).accept(this::large)
							.cell(timestamp(queue.startTime)).cell(queue.active)
							.cell(queue.pending).cell(queue.completed)
							.cell(queue.total));
		}
		{
			Stream<FutureStat> pending = JobRegistry.get().getFutureQueueStats()
					.filter(s -> filter(s.taskName));
			doc.html().body().builder().tag("h2").text("Pending queues")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(this::numeric).cell("Task")
					.cell("Run at").cell("Link").accept(this::links);
			pending.forEach(stat -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(stat.jobId).cell(stat.taskName)
						.accept(this::large).cell(timestamp(stat.runAt));
				DomNode td = cellBuilder.append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskCancelJob().withValue(stat.jobId));
					td.html().addLink("Cancel", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskRunJob().withValue(stat.jobId));
					td.html().addLink("Run", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskLogJobDetails().withValue(stat.jobId));
					td.html().addLink("Details", href, "_blank");
				}
			});
		}
		addActive(doc, "top-level - active", Job::provideIsTopLevel);
		addActive(doc, "child - active", Job::provideIsNotTopLevel);
		addCompleted(doc, "top-level", true, 20);
		addCompleted(doc, "child", false, 20);
		JobContext.get().getJob().setLargeResult(doc.prettyToString());
		logger.info("Log output to job.largeResult");
	}

	protected void addActive(DomDoc doc, String sectionFilterName,
			Predicate<Job> sectionFilter) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active and pending jobs (%s)", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(this::numeric).cell("Name")
					.accept(this::large).cell("Started").accept(this::date)
					.cell("Thread").accept(this::medium).cell("Performer")
					.accept(this::instance).cell("Links").accept(this::links);
			Predicate<Job> nameFilter = job -> filter(job.getTaskClassName());
			DomainDescriptorJob.get().getActiveJobs().filter(nameFilter)
					.filter(sectionFilter).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName()).accept(this::large)
								.cell(timestamp(job.getStartTime()))
								.cell(JobRegistry.get()
										.getPerformerThreadName(job))
								.accept(this::medium).cell(job.getPerformer())
								.accept(this::instance);
						DomNode td = cellBuilder.append();
						{
							String href = JobServlet.createTaskUrl(
									new TaskLogJobDetails().withValue(
											String.valueOf(job.getId())));
							td.html().addLink("Details", href, "_blank");
						}
						td.builder().text(" - ").tag("span").append();
						{
							String href = JobServlet.createTaskUrl(
									new TaskCancelJob().withValue(
											String.valueOf(job.getId())));
							td.html().addLink("Cancel", href, "_blank");
						}
					});
		}
	}

	protected void addCompleted(DomDoc doc, String sectionFilterName,
			boolean topLevel, int limit) {
		{
			doc.html().body().builder().tag("h2")
					.text("Recently completed jobs %s", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(this::numeric).cell("Name")
					.accept(this::large).cell("Started").accept(this::date)
					.cell("Finished").accept(this::date).cell("Performer")
					.accept(this::instance).cell("Link").accept(this::links);
			Predicate<Job> nameFilter = job -> filter(job.getTaskClassName());
			DomainDescriptorJob.get().getRecentlyCompletedJobs(topLevel)
					.filter(nameFilter).limit(limit).forEach(job -> {
						DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
								.cell(String.valueOf(job.getId()))
								.cell(job.provideName()).accept(this::large)
								.cell(timestamp(job.getStartTime()))
								.cell(timestamp(job.getEndTime()))
								.cell(job.getPerformer())
								.accept(this::instance);
						DomNode td = cellBuilder.append();
						String href = JobServlet.createTaskUrl(
								new TaskLogJobDetails().withValue(
										String.valueOf(job.getId())));
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

	boolean filter(String test) {
		if (filter == null) {
			return true;
		}
		if (test == null) {
			return false;
		}
		if (filterPattern == null) {
			filterPattern = Pattern.compile(filter);
		}
		return filterPattern.matcher(test).matches();
	}

	DomNodeHtmlTableCellBuilder large(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("trim-large");
		lastNode.setAttr("title", lastNode.textContent());
		return builder;
	}

	DomNodeHtmlTableCellBuilder medium(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("trim-medium");
		lastNode.setAttr("title", lastNode.textContent());
		return builder;
	}

	String timestamp(Date date) {
		return CommonUtils.formatDate(date, DateStyle.TIMESTAMP_HUMAN);
	}
}

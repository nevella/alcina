package cc.alcina.framework.servlet.task;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.ObjectWrapper;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.QueueStat;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.FutureStat;
import cc.alcina.framework.servlet.servlet.JobServlet;
import cc.alcina.framework.servlet.servlet.TaskWithHtmlResult;

public class TaskListJobs extends AbstractTaskPerformer
		implements TaskWithHtmlResult {
	private String filter;

	private transient Pattern filterPattern;

	public String getFilter() {
		return this.filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	private void addConsistency(DomDocument doc) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active consistency jobs").append();
			JobRegistry.get().getActiveConsistencyJobs().forEach(j -> {
				doc.html().body().builder().tag("div").text(j.toString())
						.append();
			});
		}
	}

	private DomNodeHtmlTableCellBuilder applyCompletedResultStyle(
			DomNodeHtmlTableCellBuilder builder, Job job) {
		if (job.getResultType() != JobResultType.OK
				|| !job.getState().isCompletedNormally()) {
			DomNode lastNode = builder.previousElement();
			lastNode.addClassName("imperfect-state");
			lastNode.setAttr("title",
					Ax.format("%s - %s - %s", lastNode.textContent(),
							job.getState(), job.getResultType()));
		}
		return builder;
	}

	protected void addActive(DomDocument doc, String sectionFilterName,
			Predicate<Job> sectionFilter) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active and pending jobs (%s)", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(Utils::numeric).cell("Name")
					.accept(Utils::large).cell("Started").accept(Utils::date)
					.cell("Thread").accept(Utils::medium).cell("Performer")
					.accept(Utils::instance).cell("Links").accept(Utils::links);
			Predicate<Job> textFilter = job -> filter(job.getTaskClassName(),
					job.getTaskSerialized());
			Stream<? extends Job> stream = JobDomain.get().getActiveJobs()
					.filter(textFilter).filter(sectionFilter);
			ObjectWrapper<Stream<? extends Entity>> streamRef = ObjectWrapper
					.of(stream);
			List<Job> jobs = (List<Job>) DomainStore.queryPool().call(
					() -> streamRef.get().collect(Collectors.toList()),
					streamRef, true);
			jobs.forEach(job -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(String.valueOf(job.getId()))
						.cell(job.provideName()).accept(Utils::large)
						.cell(timestamp(job.getStartTime()))
						.cell(JobRegistry.get().getPerformerThreadName(job))
						.accept(Utils::medium).cell(job.getPerformer())
						.accept(Utils::instance);
				DomNode td = cellBuilder.append();
				{
					String href = JobServlet
							.createTaskUrl(new TaskLogJobDetails()
									.withValue(String.valueOf(job.getId())));
					td.html().addLink("Details", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(new TaskCancelJob()
							.withValue(String.valueOf(job.getId())));
					td.html().addLink("Cancel", href, "_blank");
				}
			});
		}
	}

	protected void addCompleted(DomDocument doc, String sectionFilterName,
			boolean topLevel, int limit) {
		{
			doc.html().body().builder().tag("h2")
					.text("Recently completed jobs %s", sectionFilterName)
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(Utils::numeric).cell("Name")
					.accept(Utils::large).cell("Started").accept(Utils::date)
					.cell("Finished").accept(Utils::date).cell("Performer")
					.accept(Utils::instance).cell("Link").accept(Utils::links);
			Predicate<Job> textFilter = job -> filter(job.getTaskClassName(),
					job.getTaskSerialized(),
					Optional.ofNullable(job.getPerformer())
							.map(ClientInstance::toString)
							.orElse("--unmatched--"));
			Predicate<? extends Job> topLevelAdditional = topLevel
					? Job::provideIsFirstInSequence
					: job -> true;
			Stream<? extends Job> recentlyCompletedJobs = JobDomain.get()
					.getRecentlyCompletedJobs(topLevel);
			if (Ax.notBlank(filter)) {
				recentlyCompletedJobs = recentlyCompletedJobs.parallel();
			}
			Stream<? extends Job> stream = recentlyCompletedJobs
					.filter(textFilter).filter((Predicate) topLevelAdditional)
					.limit(limit);
			boolean parallel = false;
			if (Ax.notBlank(filter)) {
				stream = stream.sorted(
						Comparator.comparing(Job::getEndTime).reversed());
				parallel = true;
			}
			ObjectWrapper<Stream<? extends Entity>> streamRef = ObjectWrapper
					.of(stream);
			List<Job> jobs = (List<Job>) DomainStore.queryPool().call(
					() -> streamRef.get().collect(Collectors.toList()),
					streamRef, parallel);
			jobs.forEach(job -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(String.valueOf(job.getId()))
						.cell(job.provideName()).accept(Utils::large)
						.accept(b -> this.applyCompletedResultStyle(b, job))
						.cell(timestamp(job.getStartTime()))
						.cell(timestamp(job.getEndTime()))
						.cell(job.getPerformer()).accept(Utils::instance);
				DomNode td = cellBuilder.append();
				String href = JobServlet.createTaskUrl(new TaskLogJobDetails()
						.withValue(String.valueOf(job.getId())));
				td.html().addLink("Details", href, "_blank");
			});
		}
	}

	@Override
	protected void run0() throws Exception {
		DomDocument doc = DomDocument.basicHtmlDoc();
		String css = ResourceUtilities
				.readRelativeResource("res/TaskListJobs.css");
		doc.xpath("//head").node().builder().tag("style").text(css).append();
		{
			Stream<QueueStat> queues = JobRegistry.get().getActiveQueueStats();
			doc.html().body().builder().tag("h2")
					.text("Active allocation queues").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(Utils::numeric).cell("Name")
					.accept(Utils::large).cell("Started").accept(Utils::date)
					.cell("Active").accept(Utils::date).cell("Pending")
					.accept(Utils::numeric).cell("Completed")
					.accept(Utils::numeric).cell("Total")
					.accept(Utils::numeric);
			queues.filter(q -> filter(q.name))
					.forEach(queue -> builder.row().cell(queue.jobId)
							.cell(queue.name).accept(Utils::large)
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
			builder.row().cell("Id").accept(Utils::numeric).cell("Task")
					.cell("Run at").cell("Link").accept(Utils::links);
			pending.forEach(stat -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(stat.jobId).cell(stat.taskName)
						.accept(Utils::large).cell(timestamp(stat.runAt));
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
		addConsistency(doc);
		JobContext.get().getJob().setLargeResult(doc.prettyToString());
		logger.info("Log output to job.largeResult");
	}

	boolean filter(String... tests) {
		if (filter == null) {
			return true;
		}
		if (filterPattern == null) {
			filterPattern = Pattern.compile(filter);
		}
		return Arrays.stream(tests).filter(Objects::nonNull)
				.anyMatch(test -> filterPattern.matcher(test).matches());
	}

	String timestamp(Date date) {
		return CommonUtils.formatDate(date, DateStyle.TIMESTAMP_HUMAN);
	}
}

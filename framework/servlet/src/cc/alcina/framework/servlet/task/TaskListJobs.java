package cc.alcina.framework.servlet.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.QueueStat;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.FutureStat;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.servlet.JobServlet;
import cc.alcina.framework.servlet.servlet.TaskWithHtmlResult;

public class TaskListJobs extends PerformerTask implements TaskWithHtmlResult {
	private String filterText;

	private transient Pattern filterPattern;

	private boolean listConsistencyJobs;

	private JobResultType jobResultType;

	private Boolean scheduled;

	transient Filter filter;

	private int limit;

	private boolean showDateMillis;

	protected void addActive(DomDocument doc, String sectionFilterName,
			Predicate<Job> sectionFilter, boolean descendantTree) {
		{
			doc.html().body().builder().tag("h2")
					.text("Active jobs (%s)", sectionFilterName).append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(Utils::numeric).cell("Name")
					.accept(Utils::large).cell("Started").accept(Utils::date)
					.cell("Thread").accept(Utils::medium).cell("Performer")
					.accept(Utils::instance).cell("Links").accept(Utils::links);
			Predicate<Job> textFilter = job -> filter.test(job)
					&& filter(job.getTaskClassName(), job.getTaskSerialized());
			Stream<? extends Job> stream = JobDomain.get().getActiveJobs()
					.filter(textFilter).filter(sectionFilter);
			Ref<Stream<? extends Entity>> streamRef = Ref.of(stream);
			List<Job> jobs = query(streamRef, true, descendantTree);
			jobs.forEach(job -> {
				long id = id(job);
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(String.valueOf(id == 0 ? "\u00a0" : id))
						.cell(indentTree(job, descendantTree))
						.accept(Utils::large)
						.cell(timestamp(job.getStartTime()))
						.cell(JobRegistry.get().getPerformerThreadName(job))
						.accept(Utils::medium).cell(job.getPerformer())
						.accept(Utils::instance);
				DomNode td = cellBuilder.append();
				if (id == 0) {
					cellBuilder.blank();
					cellBuilder.blank();
				} else {
					{
						String href = JobServlet.createTaskUrl(
								new TaskLogJobDetails().withJobId(id(job)));
						td.html().addLink("Details", href, "_blank");
					}
					td.builder().text(" - ").tag("span").append();
					{
						String href = JobServlet.createTaskUrl(
								new TaskCancelJob().withJobId(id(job)));
						td.html().addLink("Cancel", href, "_blank");
					}
				}
			});
		}
	}

	String indentTree(Job job, boolean descendantTree) {
		String name = job.provideName();
		if (!descendantTree) {
			return name;
		}
		if (job.provideIsComplete()) {
			name = "[C] " + name;
		}
		if (job.getState() == JobState.PENDING) {
			name = "[P] " + name;
		}
		int indent = 0;
		indent = job.depth() * 2;
		if (indent == 0 && !job.provideIsProcessing()) {
			indent++;
		}
		int lineLength = name.length() + indent * 2;
		return CommonUtils.padStringLeft(name, lineLength, "\u00a0");
	}

	long id(Job job) {
		return job.domain().getIdOrLocalIdIfZero();
	}

	List<Job> query(Ref<Stream<? extends Entity>> streamRef, boolean parallel,
			boolean descendantTree) {
		List<Job> directResults = DomainStore.hasStores()
				? (List<Job>) DomainStore.queryPool().call(
						() -> streamRef.get().collect(Collectors.toList()),
						streamRef, parallel)
				: (List<Job>) streamRef.get().collect(Collectors.toList());
		if (!descendantTree) {
			return directResults;
		}
		List<Job> treeResult = new ArrayList<>();
		directResults.forEach(job -> {
			treeResult.add(job);
			int max = 15;
			List<Job> descendants = job
					.provideDescendantsAndSubsequentsAndAwaited().toList();
			descendants.stream().limit(max)
					.filter(j -> !j.provideIsComplete()
							|| j == j.provideFirstInSequence())
					.forEach(treeResult::add);
			if (descendants.size() >= max) {
				Job placeholderJob = PersistentImpl
						.getNewImplementationInstance(Job.class);
				placeholderJob.setTask(new PlaceholderTask(descendants.size()));
				treeResult.add(placeholderJob);
			}
		});
		return treeResult;
	}

	static class PlaceholderTask implements Task {
		int treeDescendantCount;

		PlaceholderTask(int treeDescendantCount) {
			this.treeDescendantCount = treeDescendantCount;
		}

		@Override
		public String getName() {
			return Ax.format(" ... (%s jobs)", treeDescendantCount);
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
			Predicate<Job> textFilter = job -> filter.test(job)
					&& filter(job.getTaskClassName(), job.getTaskSerialized(),
							job.provideName(),
							Optional.ofNullable(job.getPerformer())
									.map(ClientInstance::toString)
									.orElse("--unmatched--"));
			Predicate<? extends Job> topLevelAdditional = topLevel
					? Job::provideIsFirstInSequence
					: job -> true;
			Stream<? extends Job> recentlyCompletedJobs = JobDomain.get()
					.getRecentlyCompletedJobs(topLevel);
			if (filter.active) {
				recentlyCompletedJobs = recentlyCompletedJobs.parallel();
			}
			Stream<? extends Job> stream = recentlyCompletedJobs
					.filter(textFilter).filter((Predicate) topLevelAdditional)
					.limit(limit);
			boolean parallel = false;
			if (filter.active) {
				stream = stream.sorted(
						Comparator.comparing(Job::getEndTime).reversed());
				parallel = true;
			}
			Ref<Stream<? extends Entity>> streamRef = Ref.of(stream);
			List<Job> jobs = query(streamRef, parallel, false);
			jobs.forEach(job -> {
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(String.valueOf(id(job))).cell(job.provideName())
						.accept(Utils::large)
						.accept(b -> this.applyCompletedResultStyle(b, job))
						.cell(timestamp(job.getStartTime()))
						.cell(timestamp(job.getEndTime()))
						.cell(job.getPerformer()).accept(Utils::instance);
				DomNode td = cellBuilder.append();
				String href = JobServlet.createTaskUrl(
						new TaskLogJobDetails().withJobId(id(job)));
				td.html().addLink("Details", href, "_blank");
			});
		}
	}

	private void addConsistency(DomDocument doc) {
		doc.html().body().builder().tag("h2")
				.text("Active consistency jobs (this jvm)").append();
		JobRegistry.get().getActiveConsistencyJobs().forEach(j -> {
			doc.html().body().builder().tag("div").text(j.toString()).append();
		});
		doc.html().body().builder().tag("h2").text("Consistency job stats")
				.append();
		{
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			Map<Class<? extends Task>, Integer> counts = JobDomain.get()
					.getFutureConsistencyTaskCountByTaskClass();
			builder.row().cell("Task").cell("Count")
					.accept(Utils::numericRight);
			counts.forEach((taskClass, count) -> {
				builder.row().cell(taskClass.getSimpleName()).cell(count)
						.accept(Utils::numericRight);
			});
			builder.row().cell("Total")
					.cell(counts.values().stream()
							.collect(Collectors.summingInt(i -> i)))
					.accept(Utils::numericRight);
		}
		if (listConsistencyJobs) {
			doc.html().body().builder().tag("h2")
					.text("Pending consistency jobs").append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").accept(Utils::numeric).cell("Name")
					.accept(Utils::large).cell("Creation date")
					.accept(Utils::date).cell("Priority").cell("Cause")
					.accept(Utils::large).cell("Links").accept(Utils::links);
			CountingMap<Class<? extends Task>> loggedCountsByTaskClass = new CountingMap<>();
			Stream<Job> futureConsistencyJobs = JobDomain.get()
					.getFutureConsistencyJobs();
			futureConsistencyJobs.forEach(job -> {
				Class<? extends Task> taskClass = job.provideTaskClass();
				int count = loggedCountsByTaskClass.add(taskClass);
				if (count > 10) {
					return;
				}
				DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
						.cell(String.valueOf(id(job))).cell(job.provideName())
						.accept(Utils::large).cell(job.getCreationDate())
						.accept(Utils::date).cell(job.getConsistencyPriority())
						.cell(job.getCause()).accept(Utils::large);
				DomNode td = cellBuilder.append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskLogJobDetails().withJobId(id(job)));
					td.html().addLink("Details", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskCancelJob().withJobId(id(job)));
					td.html().addLink("Cancel", href, "_blank");
				}
			});
		}
	}

	private DomNodeHtmlTableCellBuilder applyCompletedResultStyle(
			DomNodeHtmlTableCellBuilder builder, Job job) {
		if (job.getResultType() != JobResultType.OK
				|| !job.getState().isCompletedNormally()) {
			DomNode lastNode = builder.previousElement();
			lastNode.addClassName("imperfect-state");
			try {
				if (job.getResultType().isFail()) {
					lastNode.addClassName("error-state");
				}
			} catch (Exception e) {
				// DEVEX
				logger.warn("processing job: " + job.getId(), e);
			}
			lastNode.setAttr("title",
					Ax.format("%s - %s - %s", lastNode.textContent(),
							job.getState(), job.getResultType()));
		}
		return builder;
	}

	boolean filter(String... tests) {
		if (filterText == null) {
			return true;
		}
		if (filterPattern == null) {
			filterPattern = Pattern.compile(filterText);
		}
		return Arrays.stream(tests).filter(Objects::nonNull)
				.anyMatch(test -> filterPattern.matcher(test).find());
	}

	public String getFilterText() {
		return this.filterText;
	}

	public JobResultType getJobResultType() {
		return this.jobResultType;
	}

	public int getLimit() {
		return this.limit;
	}

	public Boolean getScheduled() {
		return this.scheduled;
	}

	public boolean isListConsistencyJobs() {
		return this.listConsistencyJobs;
	}

	public TaskListJobs
			populateFromParameters(Map<String, String[]> parameterMap) {
		StringMap map = StringMap.flatten(parameterMap);
		filterText = map.get("filter");
		if (map.containsKey("limit")) {
			limit = map.getInt("limit");
		}
		if (map.containsKey("showDateMillis")) {
			showDateMillis = map.is("showDateMillis");
		}
		listConsistencyJobs = map.is("listConsistencyJobs");
		jobResultType = map.enumValue("jobResultType", JobResultType.class);
		scheduled = map.containsKey("scheduled") ? map.is("scheduled") : null;
		return this;
	}

	@Override
	public void run() throws Exception {
		TransactionEnvironment.withDomainTxThrowing(this::run0);
	}

	void run0() throws Exception {
		filter = new Filter();
		DomDocument doc = DomDocument.basicHtmlDoc();
		String css = Io.read().resource("res/TaskListJobs.css").asString();
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
						.cell(String.valueOf(stat.jobId)).cell(stat.taskName)
						.accept(Utils::large).cell(timestamp(stat.runAt));
				DomNode td = cellBuilder.append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskCancelJob().withJobId(stat.jobId));
					td.html().addLink("Cancel", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskRunJob().withJobId(stat.jobId));
					td.html().addLink("Run", href, "_blank");
				}
				td.builder().text(" - ").tag("span").append();
				{
					String href = JobServlet.createTaskUrl(
							new TaskLogJobDetails().withJobId(stat.jobId));
					td.html().addLink("Details", href, "_blank");
				}
			});
		}
		addActive(doc, "top-level - active", Job::provideIsTopLevel, false);
		addActive(doc, "top-level - tree", Job::provideIsTopLevel, true);
		addActive(doc, "child - active", Job::provideIsNotTopLevel, false);
		limit = Math.max(limit, 20);
		addCompleted(doc, "top-level", true, limit);
		addCompleted(doc, "child", false, limit);
		addConsistency(doc);
		JobContext.get().getJob().setLargeResult(doc.toPrettyMarkup());
		logger.info("Log output to job.largeResult");
		// FIXME - localdomain.mvcc - remove
		TransactionEnvironment.get().commit();
	}

	public void setFilterText(String filterText) {
		this.filterText = filterText;
	}

	public void setJobResultType(JobResultType jobResultType) {
		this.jobResultType = jobResultType;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setListConsistencyJobs(boolean listConsistencyJobs) {
		this.listConsistencyJobs = listConsistencyJobs;
	}

	public void setScheduled(Boolean scheduled) {
		this.scheduled = scheduled;
	}

	String timestamp(Date date) {
		DateStyle style = showDateMillis ? DateStyle.TIMESTAMP
				: DateStyle.TIMESTAMP_HUMAN;
		return style.format(date);
	}

	public boolean isShowDateMillis() {
		return showDateMillis;
	}

	public void setShowDateMillis(boolean showDateMillis) {
		this.showDateMillis = showDateMillis;
	}

	class Filter implements Predicate<Job> {
		List<Predicate<Job>> filters = new ArrayList<>();

		Predicate<Job> cumulative = null;

		boolean active;

		Filter() {
			// the noop filter for filtertext signifies "yes filtering, but
			// filter elsewhere" (since different result sets filter different
			// job fields)
			if (Ax.notBlank(filterText)) {
				filters.add(job -> true);
			}
			if (jobResultType != null) {
				filters.add(job -> job.getResultType() == jobResultType);
			}
			if (scheduled != null) {
				filters.add(job -> scheduled ? job.root().getRunAt() != null
						: job.root().getRunAt() == null);
			}
			if (filters.size() > 0) {
				active = true;
				cumulative = filters.get(0);
				for (int idx = 1; idx < filters.size(); idx++) {
					cumulative = cumulative.and(filters.get(idx));
				}
			}
		}

		@Override
		public boolean test(Job t) {
			return active ? cumulative.test(t) : true;
		}
	}
}

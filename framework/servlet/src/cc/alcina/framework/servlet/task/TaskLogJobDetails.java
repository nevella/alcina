package cc.alcina.framework.servlet.task;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.JobStateMessage;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.servlet.JobServlet;

public class TaskLogJobDetails extends PerformerTask {
	private long jobId;

	private boolean details;

	private int limit = 50;

	private DomNodeHtmlTableCellBuilder
			date(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("date");
		return builder;
	}

	protected void renderRelated(Job top, DomNode body) {
		Stream<Job> relatedProcessing = top.provideDescendantsAndSubsequents()
				.filter(j -> j.getState() == JobState.PROCESSING)
				.sorted(EntityComparator.INSTANCE).limit(limit);
		Stream<Job> relatedNonProcessing = top
				.provideDescendantsAndSubsequents()
				.filter(j -> j.getState() != JobState.PROCESSING)
				.sorted(EntityComparator.INSTANCE).limit(limit);
		Stream<Job> childAndSubsequentJobs = Stream.concat(relatedProcessing,
				relatedNonProcessing);
		renderRelatedSection(body, "Child/Subsequent jobs",
				childAndSubsequentJobs);
		renderRelatedSection(body, "Awaited jobs", top.provideAwaitedSubtree()
				.sorted(EntityComparator.INSTANCE).limit(limit));
	}

	long id(Job job) {
		return job.domain().getIdOrLocalIdIfZero();
	}

	protected void renderRelatedSection(DomNode body, String title,
			Stream<Job> jobs) {
		body.builder().tag("h2").text(title).append();
		DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
		builder.row().cell("Id").accept(this::numeric).cell("Name")
				.accept(Utils::large).cell("State").cell("Result")
				.cell("Started").accept(this::date).cell("Finished")
				.accept(this::date).cell("Performer").accept(Utils::instance)
				.cell("Link").accept(Utils::links);
		jobs.forEach(job -> {
			DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
					.cell(String.valueOf(id(job))).cell(job.provideName())
					.accept(Utils::large).cell(job.getState())
					.cell(job.getResultType())
					.cell(timestamp(job.getStartTime()))
					.cell(timestamp(job.getEndTime())).cell(job.getPerformer())
					.accept(Utils::instance);
			DomNode td = cellBuilder.append();
			String href = JobServlet
					.createTaskUrl(new TaskLogJobDetails().withJobId(id(job)));
			td.html().addLink("Details", href, "_blank");
		});
		body.builder().tag("hr").append();
	}

	protected void fields(Job job, DomNode body)
			throws IllegalAccessException, InvocationTargetException {
		job.domain().ensurePopulated();
		DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
		builder.row().cell("Field").cell("Value");
		Reflections.at(job.entityClass()).properties().stream()
				.filter(p -> !p.getName().matches(
						"largeResult|largeResultSerialized|result|resultSerialized|"
								+ "processStateSerialized|processSerialized|cachedDisplayName"))
				.forEach(p -> {
					DomNodeHtmlTableRowBuilder row = builder.row();
					Object fieldValue = p.get(job);
					String fieldText = null;
					if (fieldValue == null) {
					} else if (fieldValue instanceof Collection) {
						fieldText = CommonUtils.toLimitedCollectionString(
								(Collection<?>) fieldValue, 50);
					} else {
						fieldText = Ax.trim(fieldValue.toString(), 100000);
					}
					row.cell(p.getName()).cell(fieldText)
							.style("whitespace:pre-wrap");
				});
	}

	public long getJobId() {
		return this.jobId;
	}

	public boolean isDetails() {
		return this.details;
	}

	private DomNodeHtmlTableCellBuilder
			numeric(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("numeric");
		return builder;
	}

	public TaskLogJobDetails
			populateFromParameters(Map<String, String[]> parameterMap) {
		StringMap map = StringMap.flatten(parameterMap);
		jobId = Long.parseLong(map.get("id"));
		limit = Integer.parseInt(map.getOrDefault("limit", "50"));
		details = map.is("details");
		return this;
	}

	protected void processData(List<Job> threadData, DomNode body) {
		body.builder().tag("h2").text("Process state").append();
		body.builder().tag("div")
				.text("%s %s", threadData.size(),
						CommonUtils.pluralise("active job", threadData))
				.append();
		body.builder().tag("hr").append();
		for (Job active : threadData) {
			ProcessState processState = active.getProcessState();
			ProcessState messageState = active.getStateMessages().stream()
					.sorted(EntityComparator.REVERSED_INSTANCE).findFirst()
					.map(m -> ((JobStateMessage) m.domain().ensurePopulated())
							.getProcessState())
					.orElse(null);
			DomNode threadDiv = body.builder().tag("div")
					.className("thread-data").append();
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
			DomNode td = builder.row().cell("Job").append();
			String href = JobServlet.createTaskUrl(
					new TaskLogJobDetails().withJobId(active.getId()));
			td.html().addLink(active.toDisplayName(), href, "_blank");
			td.builder().text("\u00a0-\u00a0").append();
			String cancelHref = JobServlet.createTaskUrl(
					new TaskCancelJob().withJobId(active.getId()));
			td.html().addLink("Cancel", cancelHref, "_blank");
			if (messageState == null) {
				builder.row().cell("Response").cell("(No state response)");
			} else {
				Optional<String> logHref = Registry
						.optional(JobThreadLogUrlProvider.class)
						.map(p -> p.getLogUrl(active, messageState));
				DomNode threadTd = builder.row().cell("Thread").append();
				if (logHref.isPresent()) {
					threadTd.html().addLink(messageState.getThreadName(),
							logHref.get(), "_top");
				} else {
					threadTd.setText(messageState.getThreadName());
				}
				builder.row().cell("Allocator thread")
						.cell(messageState.getAllocatorThreadName());
				DomNode resourcesTd = builder.row().cell("Resources").append();
				resourcesTd.setClassName("resources");
				if (processState != null) {
					processState.getResources().stream().forEach(res -> {
						Class<? extends JobResource> resourceClass = Reflections
								.forName(res.getClassName());
						DomNode tdDiv = resourcesTd.builder().tag("div")
								.append();
						tdDiv.builder().tag("span").text(res.toString())
								.append();
						if (JobResource.Deletable.class
								.isAssignableFrom(resourceClass)) {
							String deleteHref = JobServlet
									.createTaskUrl(new TaskDeleteJobResource()
											.withJobId(id(active))
											.withResourceClass(
													res.getClassName())
											.withResourcePath(res.getPath()));
							tdDiv.builder().tag("a").attr("target", "_blank")
									.attr("onclick", Ax.format(
											"return window.confirm('Are you sure you want to delete resource :: %s ::?')",
											res.getPath()))
									.attr("href", deleteHref).text("[Delete]")
									.style("margin-left:1rem; display:inline-block")
									.append();
						}
					});
				}
				builder.row().cell("Stack").className("stack-trace")
						.cell(messageState.getStackTrace());
			}
			body.builder().tag("hr").append();
		}
	}

	@Override
	public void run() throws Exception {
		TransactionEnvironment.withDomainTxThrowing(this::run0);
	}

	void run0() throws Exception {
		Job job = Job.byId(jobId);
		if (job == null) {
			JobContext.info("Job {} does not exist", jobId);
		} else {
			job.domain().ensurePopulated();
			if (job.getLargeResult() != null) {
				if (details) {
					JobContext.get().getJob()
							.setLargeResult(job.getLargeResult().toString());
					logger.info("Details output to job.largeResult");
					return;
				}
			}
			DomDocument doc = DomDocument.basicHtmlDoc();
			String css = Io.read().resource("res/TaskListJobs.css").asString();
			doc.xpath("//head").node().builder().tag("style").text(css)
					.append();
			css = Io.read().resource("res/TaskLogJobDetails.css").asString();
			doc.xpath("//head").node().builder().tag("style").text(css)
					.append();
			DomNode body = doc.html().body();
			body.builder().tag("h2").text("Allocator").append();
			DomNodeBuilder queueDiv = body.builder().tag("div")
					.className("allocation-queue");
			AllocationQueue allocationQueue = JobDomain.get()
					.getAllocationQueue(job);
			if (allocationQueue == null) {
				queueDiv.text("(No allocation queue)");
			} else {
				queueDiv.text(allocationQueue.toString());
			}
			queueDiv.append();
			if (job.getLargeResult() != null) {
				DomNode div = body.builder().tag("div").append();
				String href = JobServlet.createTaskUrl(new TaskLogJobDetails()
						.withJobId(id(job)).withDetails(true));
				div.html().addLink("Large result/details", href, "");
			}
			List<Job> threadData = JobRegistry.get().getThreadData(job);
			processData(threadData, body);
			renderRelated(job, body);
			fields(job, body);
			JobContext.get().getJob().setLargeResult(doc.fullToString());
			logger.info("Details output to job.largeResult");
			// FIXME - localdomain.mvcc - remove
			TransactionEnvironment.get().commit();
		}
	}

	public void setDetails(boolean details) {
		this.details = details;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	String timestamp(Date date) {
		return DateStyle.TIMESTAMP_HUMAN.format(date);
	}

	private TaskLogJobDetails withDetails(boolean details) {
		this.details = details;
		return this;
	}

	public TaskLogJobDetails withJobId(long jobId) {
		this.jobId = jobId;
		return this;
	}

	public static interface JobThreadLogUrlProvider {
		public String getLogUrl(Job job, ProcessState messageState);
	}
}

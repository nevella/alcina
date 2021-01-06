package cc.alcina.framework.servlet.task;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.servlet.JobServlet;

public class TaskLogJobDetails extends AbstractTaskPerformer {
	private DomNodeHtmlTableCellBuilder
			date(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("date");
		return builder;
	}

	private DomNodeHtmlTableCellBuilder
			numeric(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("numeric");
		return builder;
	}

	protected void descendantAndSubsequentJobs(Job top, DomNode body) {
		body.builder().tag("h2").text("Child/Subsequent jobs").append();
		DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
		builder.row().cell("Id").accept(this::numeric).cell("Name")
				.accept(Utils::large).cell("State").cell("Result")
				.cell("Started").accept(this::date).cell("Finished")
				.accept(this::date).cell("Performer").accept(Utils::instance)
				.cell("Link").accept(Utils::links);
		Stream<Job> relatedProcessing = top.provideDescendantsAndSubsequents()
				.filter(j -> j.getState() == JobState.PROCESSING)
				.sorted(EntityComparator.INSTANCE).limit(50);
		Stream<Job> relatedNonProcessing = top
				.provideDescendantsAndSubsequents()
				.filter(j -> j.getState() != JobState.PROCESSING)
				.sorted(EntityComparator.INSTANCE).limit(50);
		Stream.concat(relatedProcessing, relatedNonProcessing).forEach(job -> {
			DomNodeHtmlTableCellBuilder cellBuilder = builder.row()
					.cell(String.valueOf(job.getId())).cell(job.provideName())
					.accept(Utils::large).cell(job.getState())
					.cell(job.getResultType())
					.cell(timestamp(job.getStartTime()))
					.cell(timestamp(job.getEndTime())).cell(job.getPerformer())
					.accept(Utils::instance);
			DomNode td = cellBuilder.append();
			String href = JobServlet.createTaskUrl(new TaskLogJobDetails()
					.withValue(String.valueOf(job.getId())));
			td.html().addLink("Details", href, "_blank");
		});
		body.builder().tag("hr").append();
	}

	protected void fields(Job job, DomNode body)
			throws IllegalAccessException, InvocationTargetException {
		DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
		builder.row().cell("Field").cell("Value");
		List<PropertyDescriptor> pds = SEUtilities
				.getPropertyDescriptorsSortedByField(job.entityClass());
		pds.removeIf(pd -> pd.getName().matches(
				"largeResult|largeResultSerialized|result|resultSerialized|"
						+ "processStateSerialized|processSerialized|cachedDisplayName"));
		for (PropertyDescriptor pd : pds) {
			DomNodeHtmlTableRowBuilder row = builder.row();
			Object fieldValue = pd.getReadMethod().invoke(job, new Object[0]);
			String fieldText = null;
			if (fieldValue == null) {
			} else if (fieldValue instanceof Collection) {
				fieldText = CommonUtils.toLimitedCollectionString(
						(Collection<?>) fieldValue, 50);
			} else {
				fieldText = fieldValue.toString();
			}
			row.cell(pd.getName()).cell(fieldText).style("whitespace:pre-wrap");
		}
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
					.map(m -> Domain.find(m).getProcessState()).orElse(null);
			DomNode threadDiv = body.builder().tag("div")
					.className("thread-data").append();
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
			DomNode td = builder.row().cell("Job").append();
			String href = JobServlet.createTaskUrl(new TaskLogJobDetails()
					.withValue(String.valueOf(active.getId())));
			td.html().addLink(active.toDisplayName(), href, "_blank");
			if (messageState == null) {
				builder.row().cell("Response").cell("(No state response)");
			} else {
				builder.row().cell("Thread").cell(messageState.getThreadName());
				builder.row().cell("Allocator thread")
						.cell(messageState.getAllocatorThreadName());
				DomNode resourcesTd = builder.row().cell("Resources").append();
				resourcesTd.setClassName("resources");
				if (processState != null) {
					processState.getResources().stream().map(Object::toString)
							.forEach(t -> resourcesTd.builder().tag("div")
									.text(t).append());
				}
				builder.row().cell("Stack").className("stack-trace")
						.cell(messageState.getStackTrace());
			}
			body.builder().tag("hr").append();
		}
	}

	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			JobContext.info("Job {} does not exist", jobId);
		} else {
			List<Job> threadData = JobRegistry.get().getThreadData(job);
			DomDoc doc = DomDoc.basicHtmlDoc();
			String css = ResourceUtilities.readClazzp("res/TaskListJobs.css");
			doc.xpath("//head").node().builder().tag("style").text(css)
					.append();
			css = ResourceUtilities.readClazzp("res/TaskLogJobDetails.css");
			doc.xpath("//head").node().builder().tag("style").text(css)
					.append();
			DomNode body = doc.html().body();
			body.builder().tag("h2").text("Allocator").append();
			DomNodeBuilder queueDiv = body.builder().tag("div")
					.className("allocation-queue");
			AllocationQueue allocationQueue = DomainDescriptorJob.get()
					.getAllocationQueue(job);
			if (allocationQueue == null) {
				queueDiv.text("(No allocation queue)");
			} else {
				queueDiv.text(allocationQueue.toString());
			}
			queueDiv.append();
			processData(threadData, body);
			descendantAndSubsequentJobs(job, body);
			fields(job, body);
			JobContext.get().getJob().setLargeResult(doc.fullToString());
			logger.info("Details output to job.largeResult");
		}
	}

	String timestamp(Date date) {
		return CommonUtils.formatDate(date, DateStyle.TIMESTAMP_HUMAN);
	}
}

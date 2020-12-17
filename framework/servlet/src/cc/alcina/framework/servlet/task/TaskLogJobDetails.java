package cc.alcina.framework.servlet.task;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.servlet.JobServlet;

public class TaskLogJobDetails extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			JobContext.info("Job {} does not exist", jobId);
		} else {
			List<Job> threadData = JobRegistry.get().getThreadData(job);
			DomDoc doc = DomDoc.basicHtmlDoc();
			String css = ResourceUtilities
					.readClazzp("res/TaskLogJobDetails.css");
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
						.map(m -> Domain.find(m).getProcessState())
						.orElse(null);
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
					builder.row().cell("Thread")
							.cell(messageState.getThreadName());
					builder.row().cell("Allocator thread")
							.cell(messageState.getAllocatorThreadName());
					DomNode resourcesTd = builder.row().cell("Resources")
							.append();
					resourcesTd.setClassName("resources");
					if (processState != null) {
						processState.getResources().stream()
								.map(Object::toString).forEach(t -> resourcesTd
										.builder().tag("div").text(t).append());
					}
					builder.row().cell("Stack").className("stack-trace")
							.cell(messageState.getStackTrace());
				}
				body.builder().tag("hr").append();
			}
			DomNodeHtmlTableBuilder builder = body.html().tableBuilder();
			builder.row().cell("Field").cell("Value");
			List<PropertyDescriptor> pds = SEUtilities
					.getPropertyDescriptorsSortedByField(job.entityClass());
			pds.removeIf(pd -> pd.getName().matches(
					"largeResult|largeResultSerialized|result|resultSerialized|"
							+ "processStateSerialized|processSerialized|cachedDisplayName"));
			for (PropertyDescriptor pd : pds) {
				DomNodeHtmlTableRowBuilder row = builder.row();
				Object fieldValue = pd.getReadMethod().invoke(job,
						new Object[0]);
				String fieldText = null;
				if (fieldValue == null) {
				} else if (fieldValue instanceof Collection) {
					fieldText = CommonUtils.toLimitedCollectionString(
							(Collection<?>) fieldValue, 50);
				} else {
					fieldText = fieldValue.toString();
				}
				row.cell(pd.getName()).cell(fieldText)
						.style("whitespace:pre-wrap");
			}
			JobContext.get().getJob().setLargeResult(doc.fullToString());
			logger.info("Details output to job.largeResult");
		}
	}
}

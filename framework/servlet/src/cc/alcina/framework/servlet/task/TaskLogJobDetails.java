package cc.alcina.framework.servlet.task;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobContext;

public class TaskLogJobDetails extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			JobContext.info("Job {} does not exist", jobId);
		} else {
			DomDoc doc = DomDoc.basicHtmlDoc();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Field").cell("Value");
			List<PropertyDescriptor> pds = SEUtilities
					.getPropertyDescriptorsSortedByField(job.entityClass());
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
			JobContext.get().getJob().setLargeResult(doc.prettyToString());
			slf4jLogger.info("Details output to job.largeResult");
		}
	}
}

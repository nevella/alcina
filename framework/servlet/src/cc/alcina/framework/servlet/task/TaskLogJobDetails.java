package cc.alcina.framework.servlet.task;

import java.beans.PropertyDescriptor;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableRowBuilder;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskLogJobDetails extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			slf4jLogger.info("Job {} does not exist", jobId);
		} else {
			DomDoc doc = DomDoc.basicHtmlDoc();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Field").cell("Value");
			List<PropertyDescriptor> pds = SEUtilities
					.getPropertyDescriptorsSortedByField(job.entityClass());
			for (PropertyDescriptor pd : pds) {
				DomNodeHtmlTableRowBuilder row = builder.row();
				row.cell(pd.getName())
						.cell(pd.getReadMethod().invoke(job, new Object[0]))
						.style("whitespace:pre-wrap");
			}
			slf4jLogger.info(doc.prettyToString());
		}
	}
}

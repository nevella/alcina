package cc.alcina.framework.servlet.task;

import java.util.List;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job2.JobRegistry;
import cc.alcina.framework.servlet.job2.JobRegistry.PendingStat;
import cc.alcina.framework.servlet.job2.JobRegistry.QueueStat;

public class TaskLogJobs extends AbstractTaskPerformer {
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
					.cell("Total");
			queues.forEach(queue -> builder.row().cell(queue.name)
					.cell(queue.active).cell(queue.pending).cell(queue.total));
		}
		{
			List<PendingStat> pending = JobRegistry.get()
					.getPendingQueueStats();
			doc.html().body().builder().tag("h2").text("Pending queues")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Name").cell("Run at").cell("Task");
			pending.forEach(stat -> builder.row().cell(stat.name)
					.cell(stat.runAt).cell(stat.taskName));
		}
		{
			doc.html().body().builder().tag("h2").text("Recently completed")
					.append();
			DomNodeHtmlTableBuilder builder = doc.html().body().html()
					.tableBuilder();
			builder.row().cell("Id").cell("Name").cell("Started")
					.cell("Finished").cell("Performer");
			DomainDescriptorJob.get().getRecentlyCompletedJobs().limit(30)
					.forEach(job -> {
						builder.row().cell(String.valueOf(job.getId()))
								.cell(job.provideName()).cell(job.getStart())
								.cell(job.getFinish())
								.cell(job.getPerformer() == null ? "(null)"
										: job.getPerformer()
												.getAuthenticationSession()
												.getUser().toIdNameString());
					});
		}
		slf4jLogger.info(doc.prettyToString());
	}
}

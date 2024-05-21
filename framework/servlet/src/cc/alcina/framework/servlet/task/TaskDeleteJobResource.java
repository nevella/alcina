package cc.alcina.framework.servlet.task;

import java.util.Optional;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ResourceRecord;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskDeleteJobResource extends PerformerTask.Fields {
	public long jobId;

	public String resourcePath;

	public String resourceClass;

	@Override
	public void run() throws Exception {
		Job job = Job.byId(jobId);
		if (job == null) {
			logger.warn("No job - %s", jobId);
			return;
		}
		Optional<ResourceRecord> optRecord = job.getProcessState()
				.getResources().stream()
				.filter(r -> CommonUtils.equals(resourcePath, r.getPath(),
						resourceClass, r.getClassName()))
				.findFirst();
		if (optRecord.isEmpty()) {
			logger.warn("No resource - %s %s %s", jobId, resourceClass,
					resourcePath);
			return;
		}
		ResourceRecord resourceRecord = optRecord.get();
		Registry.impl(DeleteResource.class,
				Reflections.forName(resourceRecord.getClassName()))
				.deleteResource(job, resourceRecord);
	}

	public TaskDeleteJobResource withJobId(long jobId) {
		this.jobId = jobId;
		return this;
	}

	public TaskDeleteJobResource withResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
		return this;
	}

	public TaskDeleteJobResource withResourceClass(String resourceClass) {
		this.resourceClass = resourceClass;
		return this;
	}

	public interface DeleteResource {
		void deleteResource(Job job, ResourceRecord resourceRecord);
	}
}

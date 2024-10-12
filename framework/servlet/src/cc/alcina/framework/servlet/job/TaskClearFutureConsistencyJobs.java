package cc.alcina.framework.servlet.job;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * <p>
 * Clears future consistency jobs of the specified task type
 *
 */
@Bean(PropertySource.FIELDS)
public class TaskClearFutureConsistencyJobs extends PerformerTask
		implements Task.RemotePerformable {
	public Class<? extends Task> taskClass;

	public TaskClearFutureConsistencyJobs
			withTaskClass(Class<? extends Task> taskClass) {
		this.taskClass = taskClass;
		return this;
	}

	@Override
	public void run() throws Exception {
		JobRegistry.get().withJobMetadataLock(
				TowardsAMoreDesirableSituation.getFutureConsistencyLockPath(),
				() -> {
					Stream<Job> stream = JobDomain.get()
							.getFutureConsistencyJobs();
					List<Job> list = stream
							.filter(j -> j.provideTaskClass() == taskClass)
							.collect(Collectors.toList());
					logger.info("To delete: {} jobs", list.size());
					AtomicInteger deleted = new AtomicInteger();
					list.forEach(job -> {
						job.delete();
						Transaction.commitIfTransformCount(5000);
						deleted.incrementAndGet();
						if (deleted.get() % 1000 == 0) {
							logger.info("Deleted: {}", deleted.get());
						}
					});
					Transaction.commit();
					logger.info("Deleted: {}", deleted.get());
				});
	}
}

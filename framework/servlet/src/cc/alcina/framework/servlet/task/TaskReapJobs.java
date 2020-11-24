package cc.alcina.framework.servlet.task;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobScheduler.RetentionPolicy;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.job.JobScheduler.ScheduleProvider;
import cc.alcina.framework.servlet.schedule.ServerTask;

@RegistryLocation(registryPoint = ScheduleProvider.class, targetClass = TaskReapJobs.class, implementationType = ImplementationType.INSTANCE)
public class TaskReapJobs extends ServerTask<TaskReapJobs>
		implements ScheduleProvider<TaskReapJobs> {
	@Override
	public Schedule getSchedule(Class<? extends TaskReapJobs> taskClass,
			boolean onAppplicationStart) {
		return new JobSchedule();
	}

	@Override
	protected void performAction0(TaskReapJobs task) throws Exception {
		Stream<? extends Job> jobs = DomainDescriptorJob.get().getAllJobs();
		List<? extends Job> reap = jobs.filter(job -> {
			if (!job.provideCanDeserializeTask()) {
				return true;
			}
			RetentionPolicy policy = Registry.impl(RetentionPolicy.class,
					job.getTask().getClass());
			return !policy.retain(job);
		}).collect(Collectors.toList());
		logger.info("Reaping {} jobs", reap.size());
		for (Job job : reap) {
			job.delete();
			Transaction.commitIfTransformCount(5000);
		}
		Transaction.commit();
		logger.info("Reaped {} jobs", reap.size());
	}

	public static class JobSchedule extends Schedule {
		public JobSchedule() {
			withClustered(true).withTimewiseLimited(true)
					.withQueueMaxConcurrentJobs(1).withNext(LocalDateTime.now()
							.truncatedTo(ChronoUnit.HOURS).plusHours(1));
		}
	}
}

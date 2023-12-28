package cc.alcina.framework.servlet.task;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStore.IgnoredLocalIdLocatorResolution;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobScheduler.RetentionPolicy;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.schedule.StandardSchedules.HourlyScheduleFactory;

public class TaskReapJobs extends PerformerTask {
	class IgnoredLocalIdLocatorResolutionObserver
			implements ProcessObserver<IgnoredLocalIdLocatorResolution> {
		@Override
		public void topicPublished(IgnoredLocalIdLocatorResolution observable) {
			Ax.out("IgnoredLocalIdLocatorResolution: %s %s %s %s",
					observable.locator, currentJob.toLocator(),
					currentJob.getTaskClassName(),
					currentJob.getTaskSerialized());
		}
	}

	transient Job currentJob;

	@Override
	public void run() throws Exception {
		if (!Configuration.is("enabled")) {
			return;
		}
		LooseContext.setTrue(
				ThreadlocalTransformManager.CONTEXT_TRACE_RECONSTITUTE_ENTITY_MAP);
		LooseContext
				.setTrue(DomainStore.CONTEXT_DO_NOT_POPULATE_LOCAL_ID_LOCATORS);
		ProcessObservers.context()
				.observe(new IgnoredLocalIdLocatorResolutionObserver());
		Stream<? extends Job> jobs = JobDomain.get().getAllJobs();
		AtomicInteger counter = new AtomicInteger(0);
		AtomicInteger reaped = new AtomicInteger(0);
		AtomicInteger exceptions = new AtomicInteger(0);
		boolean removeAllUndeserializableJobs = Configuration
				.is("removeAllUndeserializableJobs");
		jobs.forEach(job -> {
			this.currentJob = job;
			boolean delete = false;
			if (!job.provideCanDeserializeTask()) {
				if (job.getState() != null
						&& TimeConstants.withinDays(job.getCreationDate(), 1)
						&& !removeAllUndeserializableJobs) {
				} else {
					Date date = job.resolveCompletionDate();
					if (date == null) {
						// invalid job, clear
						delete = true;
					} else {
						// allow for tmp classes loaded into other vms
						delete = System.currentTimeMillis()
								- date.getTime() > TimeConstants.ONE_DAY_MS;
					}
				}
			} else {
				try {
					RetentionPolicy policy = Registry.impl(
							RetentionPolicy.class, job.getTask().getClass());
					delete = !policy.retain(job);
				} catch (Exception e) {
					if (exceptions.incrementAndGet() < 20) {
						e.printStackTrace();
					}
				}
			}
			/*
			 * This may hit a reconstitute entity map + timeout
			 */
			if (delete) {
				try {
					reaped.incrementAndGet();
					job.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (counter.incrementAndGet() % 100000 == 0
					|| TransformManager.get().getTransforms().size() > 500) {
				logger.info(
						"Reaping jobs: counter {} - transforms {} - jobs {}",
						counter.get(),
						TransformManager.get().getTransforms().size(),
						Domain.size(
								PersistentImpl.getImplementation(Job.class)));
				Transaction.commit();
				Transaction.endAndBeginNew();
			}
			JobContext.checkCancelled();
		});
		Transaction.commit();
		logger.info("Reaped {} jobs", reaped.get());
	}

	@Registration(
		value = { Schedule.class, TaskReapJobs.class },
		implementation = Registration.Implementation.FACTORY)
	public static class ScheduleFactory extends HourlyScheduleFactory {
	}
}

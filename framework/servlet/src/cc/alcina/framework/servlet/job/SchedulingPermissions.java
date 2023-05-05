package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;

class SchedulingPermissions {
	static boolean canAllocate(AllocationQueue queue) {
		/*
		 * can we allocate? if it's (a) a clustered child allocation or (b)
		 * we're the creator or (c) it's a schedule event and we're the cluster
		 * leader
		 */
		if (ExecutionConstraints.forQueue(queue).isClusteredChildAllocation()) {
			return true;
		}
		Job job = queue.job;
		if (job.provideParent().isPresent()
				&& ExecutionConstraints.forQueue(queue.ensureParentQueue())
						.isClusteredChildAllocation()) {
			return true;
		}
		return canAllocate(job);
	}

	static boolean canAllocate(Job job) {
		if (job.getPerformer() == ClientInstance.self()) {
			return true;
		}
		if (job.getRunAt() == null) {
			return job.getCreator() == ClientInstance.self();
		}
		if (Schedule.forTaskClass(job.provideTaskClass()).isVmLocal()) {
			return job.getCreator() == ClientInstance.self();
		} else {
			return isCurrentScheduledJobExecutor();
		}
	}

	static boolean canCreateFuture(Schedule schedule) {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		if (!JobRegistry.get().getEnvironment().canCreateFutures()) {
			return false;
		}
		boolean production = EntityLayerUtils.isProduction();
		boolean scheduleClusterJobs = production
				|| Configuration.is(JobScheduler.class, "testSchedules");
		boolean scheduleVmLocalJobs = production
				|| Configuration.is(JobScheduler.class, "testVmLocalSchedules");
		return (schedule.isVmLocal() && scheduleVmLocalJobs)
				|| (isCurrentScheduledJobExecutor() && scheduleClusterJobs);
	}

	static boolean canFutureToPending() {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		if (EntityLayerUtils.isProduction()) {
			return Configuration.is(JobScheduler.class, "canFuturesToPending");
		} else {
			return Configuration.is(JobScheduler.class, "testFuturesToPending");
		}
	}

	static boolean canModifyFuture(Job job) {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		Schedule schedule = Schedule.forTaskClass(job.provideTaskClass());
		if (schedule == null) {
			return false;
		}
		if (schedule.isVmLocal()) {
			return job.getCreator() == ClientInstance.self();
		} else {
			return isCurrentScheduledJobExecutor();
		}
	}

	static boolean canProcessOrphans() {
		if (!JobRegistry.get().getEnvironment().isPersistent()) {
			return false;
		}
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		return JobRegistry.get().jobExecutors.isCurrentOrphanage();
	}

	static boolean isCurrentScheduledJobExecutor() {
		return JobRegistry.get().jobExecutors.isCurrentScheduledJobExecutor()
				&& JobRegistry.get().jobExecutors
						.isHighestBuildNumberInCluster()
				&& Configuration.is(JobScheduler.class, "scheduleClusterJobs")
				&& !AppPersistenceBase.isInstanceReadOnly();
	}
}
package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.servlet.Sx;
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
		boolean production = Sx.isProduction();
		boolean scheduleClusterJobs = production
				|| ResourceUtilities.is(JobScheduler.class, "testSchedules");
		boolean scheduleVmLocalJobs = production || ResourceUtilities
				.is(JobScheduler.class, "testVmLocalSchedules");
		return (schedule.isVmLocal() && scheduleVmLocalJobs)
				|| (isCurrentScheduledJobExecutor() && scheduleClusterJobs);
	}

	static boolean canFutureToPending() {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		if (Sx.isProduction()) {
			return ResourceUtilities.is(JobScheduler.class,
					"canFuturesToPending");
		} else {
			return ResourceUtilities.is(JobScheduler.class,
					"testFuturesToPending");
		}
	}

	static boolean canModifyFuture(Job job) {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		if (Schedule.forTaskClass(job.provideTaskClass()).isVmLocal()) {
			return job.getCreator() == ClientInstance.self();
		} else {
			return isCurrentScheduledJobExecutor();
		}
	}

	static boolean canProcessOrphans() {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return false;
		}
		return JobRegistry.get().jobExecutors.isCurrentOrphanage();
	}

	static boolean isCurrentScheduledJobExecutor() {
		return JobRegistry.get().jobExecutors.isCurrentScheduledJobExecutor()
				&& JobRegistry.get().jobExecutors
						.isHighestBuildNumberInCluster()
				&& ResourceUtilities.is(JobScheduler.class,
						"scheduleClusterJobs")
				&& !AppPersistenceBase.isInstanceReadOnly();
	}
}
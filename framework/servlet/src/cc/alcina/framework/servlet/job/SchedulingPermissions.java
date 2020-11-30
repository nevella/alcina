package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.ResourceUtilities;
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
		boolean production = !(Sx.isTestServer() || Sx.isTest());
		boolean scheduleClusterJobs = production
				|| ResourceUtilities.is(JobScheduler.class, "testSchedules");
		boolean scheduleVmLocalJobs = production || ResourceUtilities
				.is(JobScheduler.class, "testVmLocalSchedules");
		return (schedule.isVmLocal() && scheduleVmLocalJobs)
				|| (isCurrentScheduledJobExecutor() && scheduleClusterJobs);
	}

	static boolean canModifyFuture(Job job) {
		if (Schedule.forTaskClass(job.provideTaskClass()).isVmLocal()) {
			return job.getCreator() == ClientInstance.self();
		} else {
			return isCurrentScheduledJobExecutor();
		}
	}

	static boolean canProcessOrphans() {
		return isCurrentScheduledJobExecutor() || ResourceUtilities
				.is(JobScheduler.class, "forceProcessOrphans");
	}

	static boolean isCurrentScheduledJobExecutor() {
		return JobRegistry.get().jobExecutors.isCurrentScheduledJobExecutor()
				&& ResourceUtilities.is(JobScheduler.class,
						"scheduleClusterJobs");
	}
}
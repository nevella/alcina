package cc.alcina.framework.entity.persistence.cache.descriptor;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ClientInstanceLoadOracle;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.DomainStoreDescriptor;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

@RegistryLocation(registryPoint = DomainDescriptorJob.class, implementationType = ImplementationType.SINGLETON)
public class DomainDescriptorJob {
	public static DomainDescriptorJob get() {
		return Registry.impl(DomainDescriptorJob.class);
	}

	public Topic<String> queueChanged = Topic.local();

	private Class<? extends Job> jobImplClass;

	private DomainTransformPersistenceListener queueNotifier = new DomainTransformPersistenceListener() {
		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			if (event
					.getPersistenceEventType() == DomainTransformPersistenceEventType.COMMIT_OK) {
				AdjunctTransformCollation collation = event
						.getTransformPersistenceToken().getTransformCollation();
				if (collation.has(jobImplClass)) {
					collation.query(jobImplClass).withFilter(transform -> {
						if ("statusMessage"
								.equals(transform.getPropertyName())) {
							return false;
						} else {
							return true;
						}
					}).stream().map(qr -> (Job) qr.getObject())
							.map(Job::getQueue).distinct()
							.forEach(queueChanged::publish);
				}
			} else if (event
					.getPersistenceEventType() == DomainTransformPersistenceEventType.PRE_COMMIT) {
				AdjunctTransformCollation collation = event
						.getTransformPersistenceToken().getTransformCollation();
				if (collation.has(jobImplClass)) {
					collation.ensureApplied();
					List<Job> invalidJobs = collation.query(jobImplClass)
							.stream().map(qr -> (Job) qr.getObject())
							.filter(job -> job.getQueue() == null
									|| job.getTaskClassName() == null)
							.distinct().collect(Collectors.toList());
					if (invalidJobs.size() > 0) {
						throw Ax.runtimeException(
								"Persisting jobs with null queue/task",
								invalidJobs);
					}
				}
			}
		}
	};

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		jobImplClass = AlcinaPersistentEntityImpl.getImplementation(Job.class);
		descriptor.addClassDescriptor(jobImplClass, "queue", "taskClassName");
		descriptor.addClassDescriptor(AlcinaPersistentEntityImpl
				.getImplementation(JobRelation.class));
	}

	public int getActiveJobCount(String queueName) {
		return (int) Domain.query(jobImplClass).filter("queue", queueName)
				.filter("state", JobState.PROCESSING).stream().count();
	}

	public Stream<? extends Job> getAllocatedIncompleteJobs() {
		// FIXME - mvcc.jobs - make a projection
		Predicate<Job> allocatedIncompletePredicate = job -> job
				.getPerformer() != null && !job.provideIsComplete();
		return Domain.query(jobImplClass).filter(allocatedIncompletePredicate)
				.stream();
	}

	public Job getJob(String id) {
		return getMostRecentJobForQueue(id);
	}

	public Stream<? extends Job> getJobsForQueue(String queueName) {
		return Domain.query(jobImplClass).filter("queue", queueName).stream();
	}

	public Stream<? extends Job> getJobsForTask(Task action) {
		return Domain.query(jobImplClass)
				.filter("taskClassName", action.getClass().getName()).stream();
	}

	public Stream<? extends Job> getPendingNonClusteredJobs() {
		// FIXME - mvcc.jobs - make a projection
		Predicate<Job> pendingNonClusteredPredicate = job -> job
				.getPerformer() == null && job.provideIsPending()
				&& !job.isClustered();
		return Domain.query(jobImplClass).filter(pendingNonClusteredPredicate)
				.stream();
	}

	public Stream<? extends Job> getRecentlyCompletedJobs() {
		return Domain.stream(jobImplClass)
				.sorted(EntityComparator.REVERSED_INSTANCE)
				.filter(Job::provideIsComplete);
	}

	public Stream<? extends Job> getUnallocatedJobsForQueue(String queueName,
			boolean performableByThisVm) {
		Predicate<Job> pendingPredicate = Job::provideIsPending;
		Predicate<Job> performerPredicate = job -> job.provideCanBePerformedBy(
				EntityLayerObjects.get().getServerAsClientInstance());
		DomainQuery<? extends Job> query = Domain.query(jobImplClass)
				.filter("queue", queueName).filter(pendingPredicate);
		if (performableByThisVm) {
			query = query.filter(performerPredicate);
		}
		return query.filter("performer", null).stream();
	}

	public void onWarmupComplete(DomainStore domainStore) {
		domainStore.getPersistenceEvents()
				.addDomainTransformPersistenceListener(queueNotifier, true);
	}

	private Job getMostRecentJobForQueue(String id) {
		{
			Predicate<Job> predicate = Job::provideIsActive;
			Job job = Domain.query(jobImplClass).filter("queue", id)
					.filter(predicate).find();
			if (job != null) {
				return job;
			}
		}
		{
			Predicate<Job> predicate = Job::provideIsPending;
			Job job = Domain.query(jobImplClass).filter("queue", id)
					.filter(predicate).find();
			if (job != null) {
				return job;
			}
		}
		{
			Predicate<Job> predicate = Job::provideIsComplete;
			Job job = Domain.query(jobImplClass).filter("queue", id)
					.filter(predicate).stream().reduce(Ax.last()).orElse(null);
			return job;
		}
	}

	@RegistryLocation(registryPoint = ClientInstanceLoadOracle.class, implementationType = ImplementationType.SINGLETON)
	public static class ClientInstanceLoadOracleImpl
			extends ClientInstanceLoadOracle {
		@Override
		public boolean shouldLoad(Job job, boolean duringWarmup) {
			if (duringWarmup) {
				return !job.provideIsComplete();
			} else {
				return true;
			}
		}
	}
}

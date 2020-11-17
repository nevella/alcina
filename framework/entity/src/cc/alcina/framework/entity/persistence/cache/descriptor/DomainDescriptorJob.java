package cc.alcina.framework.entity.persistence.cache.descriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.StreamConcatenation;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ClientInstanceLoadOracle;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.ResourceUtilities;
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

	public Topic<Entry<String, List<Job>>> queueChanged = Topic.local();

	public Topic<RelatedJobCompletion> relatedJobCompletionChanged = Topic
			.local();

	private Class<? extends Job> jobImplClass;

	private Map<Thread, Exception> inFlightTransformRequests = new WeakHashMap<>();

	private Logger logger = LoggerFactory.getLogger(getClass());

	private DomainTransformPersistenceListener jobLogger = new DomainTransformPersistenceListener() {
		@Override
		public boolean isPreBarrierListener() {
			return true;
		}

		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			AdjunctTransformCollation collation = event
					.getTransformPersistenceToken().getTransformCollation();
			if (!collation.has(jobImplClass)) {
				return;
			}
			Thread currentThread = Thread.currentThread();
			switch (event.getPersistenceEventType()) {
			case COMMIT_OK:
				inFlightTransformRequests.remove(currentThread);
				break;
			case COMMIT_ERROR:
				Exception ex = inFlightTransformRequests.remove(currentThread);
				logger.warn("Issue with job transform commit", ex);
				logger.info("Issue with job transform details:\n{}",
						event.getTransformPersistenceToken().getRequest());
				break;
			case PRE_FLUSH:
				inFlightTransformRequests.put(currentThread, new Exception());
				logger.info("Flushing job transform - ids: {}",
						collation.query(jobImplClass).stream()
								.map(qr -> qr.entityCollation.getId())
								.collect(Collectors.toSet()));
				break;
			}
		}
	};

	private DomainTransformPersistenceListener queueNotifier = new DomainTransformPersistenceListener() {
		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			if (event
					.getPersistenceEventType() == DomainTransformPersistenceEventType.COMMIT_OK) {
				AdjunctTransformCollation collation = event
						.getTransformPersistenceToken().getTransformCollation();
				if (collation.has(jobImplClass)) {
					Multimap<String, List<Job>> perQueueJobs = collation
							.query(jobImplClass).withFilter(transform -> {
								if ("statusMessage"
										.equals(transform.getPropertyName())) {
									return false;
								} else {
									return true;
								}
							}).stream().map(qr -> (Job) qr.getObject())
							.collect(AlcinaCollectors
									.toKeyMultimap(Job::getQueue));
					perQueueJobs.entrySet().forEach(queueChanged::publish);
					Multimap<Optional<Job>, List<Job>> byParent = collation
							.query(jobImplClass).stream()
							.map(qr -> (Job) qr.getObject())
							.filter(Job::provideIsComplete)
							.filter(Job::provideIsLastInSequence)
							.map(Job::provideFirstInSequence)
							// if job has no parent, observe its own cancelled
							// state (hence parentOrSelf
							.collect(AlcinaCollectors
									.toKeyMultimap(Job::provideParentOrSelf));
					byParent.entrySet().stream()
							.filter(e -> e.getKey().isPresent()).forEach(e -> {
								relatedJobCompletionChanged
										.publish(new RelatedJobCompletion(
												e.getKey().get(),
												e.getValue()));
							});
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

	private JobDescriptor jobDescriptor;

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		jobImplClass = AlcinaPersistentEntityImpl.getImplementation(Job.class);
		jobDescriptor = new JobDescriptor();
		descriptor.addClassDescriptor(jobDescriptor);
		descriptor.addClassDescriptor(AlcinaPersistentEntityImpl
				.getImplementation(JobRelation.class));
	}

	public int getActiveJobCount(String queueName) {
		return jobDescriptor.getJobsForQueue(queueName, JobState.PROCESSING)
				.size();
	}

	public Stream<? extends Job> getAllocatedIncompleteJobs() {
		return jobDescriptor.getJobsForState(JobState.ALLOCATED);
	}

	public int getCompletedJobCountForActiveQueue(String name) {
		return Arrays.stream(JobState.values()).filter(JobState::isComplete)
				.map(state -> jobDescriptor.getJobsForQueue(name, state).size())
				.collect(Collectors.summingInt(i -> i));
	}

	public int getIncompleteJobCountForActiveQueue(String name) {
		return Arrays.stream(JobState.values()).filter(s -> !s.isComplete())
				.map(state -> jobDescriptor.getJobsForQueue(name, state).size())
				.collect(Collectors.summingInt(i -> i));
	}

	public int getJobCountForActiveQueue(String name) {
		return Arrays.stream(JobState.values())
				.map(state -> jobDescriptor.getJobsForQueue(name, state).size())
				.collect(Collectors.summingInt(i -> i));
	}

	public int getJobCountForActiveQueue(String name, JobState state) {
		return jobDescriptor.getJobsForQueue(name, state).size();
	}

	public Stream<? extends Job> getJobsForTask(Task action) {
		return Domain.query(jobImplClass)
				.filter("taskClassName", action.getClass().getName()).stream();
	}

	public Stream<? extends Job> getNotCompletedJobs() {
		return jobDescriptor.getAllActiveJobs()
				.sorted(EntityComparator.REVERSED_INSTANCE)
				.filter(Job::provideIsNotComplete);
	}

	public Stream<? extends Job> getPendingJobsWithInactiveCreator(
			List<ClientInstance> activeInstances) {
		return jobDescriptor.getPendingJobsWithInactiveCreator(activeInstances);
	}

	public Stream<? extends Job> getRecentlyCompletedJobs() {
		return Domain.stream(jobImplClass)
				.sorted(EntityComparator.REVERSED_INSTANCE)
				.filter(Job::provideIsComplete);
	}

	public int getUnallocatedJobCountForQueue(String name) {
		return (int) getUnallocatedJobsForQueue(name, true).count();
	}

	public Stream<? extends Job> getUnallocatedJobsForQueue(String queueName,
			boolean performableByThisVm) {
		Predicate<Job> performerPredicate = job -> !job.provideHasPerformer()
				&& job.provideCanBePerformedBy(
						EntityLayerObjects.get().getServerAsClientInstance());
		Stream<Job> stream = jobDescriptor
				.getJobsForQueue(queueName, JobState.PENDING).stream();
		if (performableByThisVm) {
			stream = stream.filter(performerPredicate);
		}
		return stream;
	}

	public void onWarmupComplete(DomainStore domainStore) {
		domainStore.getPersistenceEvents()
				.addDomainTransformPersistenceListener(queueNotifier, true);
		if (ResourceUtilities.is("logTransforms")) {
			domainStore.getPersistenceEvents()
					.addDomainTransformPersistenceListener(jobLogger, false);
		}
	}

	/*
	 * Old completed jobs will have a wide spread of client instances - which we
	 * don't need (only needed pre-completion for execution constraints). So
	 * filter appropriately
	 */
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

	public static class RelatedJobCompletion {
		public Job job;

		public List<Job> related;

		public RelatedJobCompletion(Job job, List<Job> related) {
			this.job = job;
			this.related = related;
		}
	}

	class ActiveQueueProjection extends BaseProjection<Job> {
		public ActiveQueueProjection() {
			super(String.class, JobState.class, jobImplClass);
		}

		@Override
		public Class<? extends Job> getListenedClass() {
			return jobImplClass;
		}

		@Override
		public void insert(Job t) {
			if (t.provideIsComplete()) {
				return;
			}
			super.insert(t);
		}

		@Override
		protected int getDepth() {
			return 3;
		}

		@Override
		protected Object[] project(Job job) {
			return new Object[] { job.getQueue(), job.resolveState(), job,
					job };
		}
	}

	class ByCreatorIncompleteProjection extends BaseProjection<Job> {
		public ByCreatorIncompleteProjection() {
			super(ClientInstance.class, jobImplClass);
		}

		@Override
		public Class<? extends Job> getListenedClass() {
			return jobImplClass;
		}

		@Override
		public void insert(Job t) {
			if (t.provideIsComplete()) {
				return;
			}
			super.insert(t);
		}

		@Override
		protected int getDepth() {
			return 2;
		}

		@Override
		protected Object[] project(Job job) {
			return new Object[] { job.getCreator(), job, job };
		}
	}

	class JobDescriptor extends DomainClassDescriptor {
		private ActiveQueueProjection activeQueueProjection;

		private ByCreatorIncompleteProjection byCreatorIncompleteProjection;

		public JobDescriptor() {
			super(jobImplClass, "taskClassName");
		}

		public Stream<Job> getAllActiveJobs() {
			return Arrays.stream(JobState.values()).filter(s -> !s.isComplete())
					.flatMap(this::getJobsForState);
		}

		public Set<Job> getJobsForQueue(String queueName, JobState state) {
			MultikeyMap map = activeQueueProjection.getLookup().asMap(queueName,
					state);
			if (map == null) {
				return Collections.emptySet();
			} else {
				return map.keySet();
			}
		}

		public Stream<Job> getJobsForState(JobState state) {
			List<Stream<Job>> streamList = activeQueueProjection.getLookup()
					.typedKeySet(String.class).stream()
					.map(key -> activeQueueProjection.getLookup().asMap(key,
							state))
					.filter(Objects::nonNull)
					.map(mkm -> (Set<Job>) mkm.typedKeySet(Job.class))
					.map(Collection::stream).collect(Collectors.toList());
			Stream<Job>[] streams = (Stream<Job>[]) streamList
					.toArray(new Stream[streamList.size()]);
			return StreamConcatenation.concat(streams);
		}

		public Stream<? extends Job> getPendingJobsWithInactiveCreator(
				List<ClientInstance> activeCreators) {
			MultikeyMap<Job> lookup = byCreatorIncompleteProjection.getLookup();
			return lookup.typedKeySet(ClientInstance.class).stream()
					.filter(ci -> !activeCreators.contains(ci))
					.map(ci -> (Set<Job>) lookup.asMap(ci)
							.typedKeySet(Job.class))
					.flatMap(Collection::stream);
		}

		@Override
		public void initialise() {
			super.initialise();
			activeQueueProjection = new ActiveQueueProjection();
			projections.add(activeQueueProjection);
			byCreatorIncompleteProjection = new ByCreatorIncompleteProjection();
			projections.add(byCreatorIncompleteProjection);
		}

		ActiveQueueProjection getDisabledAtCourtRequestProjection() {
			return activeQueueProjection;
		}
	}
}

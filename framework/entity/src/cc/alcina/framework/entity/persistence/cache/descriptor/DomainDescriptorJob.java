package cc.alcina.framework.entity.persistence.cache.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ClientInstanceLoadOracle;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.DomainStoreDescriptor;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalMultiset;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalSet;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

@RegistryLocation(registryPoint = DomainDescriptorJob.class, implementationType = ImplementationType.SINGLETON)
public class DomainDescriptorJob {
	public static DomainDescriptorJob get() {
		return Registry.impl(DomainDescriptorJob.class);
	}

	private Class<? extends Job> jobImplClass;

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
			case COMMIT_OK: {
				Set<Long> ids = collation.query(jobImplClass).stream()
						.map(qr -> qr.entityCollation.getId())
						.collect(Collectors.toSet());
				logger.info("Post-process job transform - rq: {}, ids: {}",
						event.getPersistedRequestIds(),
						CommonUtils.toLimitedCollectionString(ids, 50));
				break;
			}
			case COMMIT_ERROR:
				logger.info("Issue with job transform details:\n{}",
						event.getTransformPersistenceToken().getRequest());
				break;
			case PRE_FLUSH:
				Set<Long> ids = collation.query(jobImplClass).stream()
						.map(qr -> qr.entityCollation.getId())
						.collect(Collectors.toSet());
				logger.info("Flushing job transform - rq: {}, ids: {}",
						event.getPersistedRequestIds(),
						CommonUtils.toLimitedCollectionString(ids, 50));
				break;
			}
		}
	};

	private JobDescriptor jobDescriptor;

	boolean warmupComplete = false;

	/*
	 * *Not* a transactional map - we want the same queue for all tx phases.
	 * 
	 * The queue subfields are essentially immutable or tx-safe - exception is
	 * phase, but that's modified by the single-threaded allocator
	 */
	private Map<Job, AllocationQueue> queues = new ConcurrentHashMap<>();

	public Topic<AllocationQueue.Event> queueEvents = Topic.local();

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		jobImplClass = AlcinaPersistentEntityImpl.getImplementation(Job.class);
		jobDescriptor = new JobDescriptor();
		descriptor.addClassDescriptor(jobDescriptor);
		descriptor.addClassDescriptor(AlcinaPersistentEntityImpl
				.getImplementation(JobRelation.class));
	}

	public Optional<Job> earliestFuture(Class<? extends Task> key,
			boolean createdBySelf) {
		return jobDescriptor.allocationQueueProjection.earliestFuture(key,
				createdBySelf);
	}

	public void fireInitialAllocatorQueueCreationEvents() {
		queues.values().forEach(AllocationQueue::fireInitialCreationEvents);
	}

	public Stream<? extends Job> getActiveJobs() {
		// subjobs are reachable from two allocationqueues, hence 'distinct'
		return queues.values().stream().flatMap(AllocationQueue::getActiveJobs)
				.distinct()
				.sorted(Comparator.comparing(Job::getStartTime).reversed());
	}

	public Stream<Job> getAllFutureJobs() {
		return jobDescriptor.allocationQueueProjection.futuresByTask.allItems()
				.stream();
	}

	public Stream<? extends Job> getAllJobs() {
		return Domain.stream(jobImplClass);
	}

	public Stream<AllocationQueue> getAllocationQueues() {
		cleanupQueues();
		return queues.values().stream();
	}

	public Stream<Job> getIncompleteJobs() {
		cleanupQueues();
		return queues.values().stream()
				.flatMap(AllocationQueue::getIncompleteJobs);
	}

	public Stream<? extends Job> getJobsForTask(Task task) {
		return Domain.query(jobImplClass)
				.filter("taskClassName", task.getClass().getName()).stream();
	}

	public Stream<? extends Job>
			getRecentlyCompletedJobs(Predicate<Job> predicate, int limit) {
		Predicate<Job> complete = Job::provideIsComplete;
		return Domain.query(jobImplClass).filter(complete).filter(predicate)
				.limit(limit)
				.sorted(Comparator.comparing(
						Job::resolveCompletionDateOrLastModificationDate)
						.reversed())
				.stream();
	}

	public Stream<Job> getUndeserializableJobs() {
		return jobDescriptor.allocationQueueProjection.undeserializableJobs
				.stream();
	}

	public void onWarmupComplete(DomainStore domainStore) {
		if (ResourceUtilities.is("logTransforms")) {
			domainStore.getPersistenceEvents()
					.addDomainTransformPersistenceListener(jobLogger);
		}
		warmupComplete = true;
	}

	private void cleanupQueues() {
		queues.entrySet().removeIf(e -> e.getValue().job
				.resolveState() == JobState.ABORTED
				|| e.getValue().job.resolveState() == JobState.CANCELLED);
	}

	public class AllocationQueue {
		public Job job;

		public Topic<Event> events = Topic.local();

		SubqueueProjection subqueues = new SubqueueProjection();

		public SubqueuePhase phase = SubqueuePhase.Self;

		List<JobState> incompleteAllocatableStates = Arrays.asList(
				JobState.PENDING, JobState.ALLOCATED, JobState.PROCESSING);

		List<JobState> incompleteAllocatableStatesChild = Arrays.asList(
				JobState.PENDING, JobState.ALLOCATED, JobState.PROCESSING,
				JobState.COMPLETED);

		private boolean firedToProcessing;

		public AllocationQueue(Job job) {
			this.job = job;
		}

		public QueueStat asQueueStat() {
			QueueStat stat = new QueueStat();
			stat.active = (int) perStateJobs(JobState.PROCESSING).count();
			stat.completed = (int) (perStateJobs(JobState.COMPLETED).count()
					+ perStateJobs(JobState.SEQUENCE_COMPLETE).count());
			stat.pending = (int) (perStateJobs(JobState.PENDING).count()
					+ perStateJobs(JobState.ALLOCATED).count());
			stat.total = stat.active + stat.pending + stat.completed;
			stat.name = job.toDisplayName();
			stat.startTime = job.getStartTime() != null ? job.getStartTime()
					: job.getCreationDate();
			return stat;
		}

		public void checkComplete() {
			if (isComplete()) {
				queues.remove(job);
				publish(EventType.DELETED);
				phase = SubqueuePhase.Complete;
			}
		}

		public void clearIncompleteAllocatedJobs() {
			Stream.of(JobState.ALLOCATED, JobState.PROCESSING)
					.forEach(state -> {
						Set<? extends Job> jobs = subQueueJobs(phase, state);
						Iterator<? extends Job> itr = jobs.iterator();
						while (itr.hasNext()) {
							Job next = itr.next();
							logger.info("Removing from subQueue {}/{} - {}",
									phase, state, next);
							itr.remove();
						}
					});
		}

		public Stream<Job> getActiveJobs() {
			return perStateJobs(JobState.PROCESSING);
		}

		public long getCompletedJobCount() {
			return perPhaseJobCount(JobState.COMPLETED,
					JobState.SEQUENCE_COMPLETE);
		}

		public long getIncompleteAllocatedJobCountForCurrentPhase() {
			return getIncompleteJobCountForCurrentPhase() - perPhaseJobCount(
					Collections.singletonList(JobState.PENDING));
		}

		public long getIncompleteJobCountForCurrentPhase() {
			return perPhaseJobCount(incompleteStates(phase));
		}

		public Stream<Job> getIncompleteJobs() {
			return perStateJobs(incompleteAllocatableStatesChild);
		}

		public long getTotalJobCount() {
			return perPhaseJobCount(JobState.values());
		}

		public long getUnallocatedJobCount() {
			return perPhaseJobCount(
					Collections.singletonList(JobState.PENDING));
		}

		public Stream<Job> getUnallocatedJobs() {
			return (Stream) perPhaseJobs(
					Collections.singletonList(JobState.PENDING));
		}

		public void incrementPhase() {
			phase = SubqueuePhase.values()[phase.ordinal() + 1];
		}

		public void insert(Job job) {
			logger.info("subqueue/insert - {} - {} - {}",
					this.job.toStringEntity(), subqueues.project(job), job);
			subqueues.insert(job);
			publish(EventType.RELATED_MODIFICATION);
			checkFireToProcessing(job);
		}

		public boolean isNoPendingJobsInPhase() {
			if (getIncompleteJobCountForCurrentPhase() == 0) {
				// double-check ? for the moment should be no need since
				// persisting (on performer) before reaching here
				return true;
			}
			return false;
		}

		public SubqueuePhase provideAllocationKey(Job job) {
			if (job == this.job) {
				return SubqueuePhase.Self;
			}
			if (job.provideFirstInSequence() == this.job) {
				return SubqueuePhase.Sequence;
			}
			if (job.provideParentOrSelf() == this.job) {
				return SubqueuePhase.Child;
			}
			throw new UnsupportedOperationException();
		}

		public void publish(EventType type) {
			Event event = new Event(type);
			events.publish(event);
			if (type.isPublishToGlobalQueue()) {
				queueEvents.publish(event);
			}
		}

		public void remove(Job job) {
			logger.info("subqueue/remove - {} - {} - {}",
					this.job.toStringEntity(), subqueues.project(job), job);
			subqueues.remove(job);
		}

		public String toDisplayName() {
			return job == null ? "<no-job>" : job.toDisplayName();
		}

		@Override
		public String toString() {
			List<String> phaseStates = new ArrayList<>();
			for (SubqueuePhase type : SubqueuePhase.values()) {
				phaseStates.add(Ax.format("%s - (%s)", type,
						incompleteAllocatableStatesChild.stream()
								.map(state -> subQueueJobs(type, state).size())
								.map(String::valueOf)
								.collect(Collectors.joining("/"))));
			}
			return Ax.format("Allocation Queue - %s - %s\n\t%s", job, phase,
					CommonUtils.joinWithNewlineTab(phaseStates));
		}

		private void checkFireToProcessing(Job job) {
			if (job == this.job && !firedToProcessing) {
				switch (job.resolveState()) {
				case PROCESSING:
				case COMPLETED:
					firedToProcessing = true;
					publish(EventType.TO_PROCESSING);
					break;
				}
			}
		}

		/*
		 * Child completion must wait for any sequential jobs triggered by the
		 * child
		 */
		private List<JobState> incompleteStates(SubqueuePhase phase) {
			switch (phase) {
			case Child:
				return incompleteAllocatableStatesChild;
			default:
				return incompleteAllocatableStates;
			}
		}

		private boolean isComplete() {
			for (SubqueuePhase type : SubqueuePhase.values()) {
				for (JobState state : incompleteStates(type)) {
					if (subQueueJobs(type, state).size() > 0) {
						return false;
					}
				}
			}
			return true;
		}

		private long perPhaseJobCount(JobState... states) {
			return perPhaseJobCount(Arrays.asList(states));
		}

		private long perPhaseJobCount(List<JobState> states) {
			return Stream.of(phase)
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state)))
					.collect(Collectors.summingInt(Collection::size));
		}

		private Stream<Job> perPhaseJobs(List<JobState> states) {
			return (Stream) Stream.of(phase)
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state))
							.flatMap(Collection::stream));
		}

		private Stream<Job> perStateJobs(JobState... states) {
			return perStateJobs(Arrays.asList(states));
		}

		private Stream<Job> perStateJobs(List<JobState> states) {
			return (Stream) Arrays.stream(SubqueuePhase.values())
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state))
							.flatMap(Collection::stream));
		}

		private Set<? extends Job> subQueueJobs(SubqueuePhase type,
				JobState state) {
			MultikeyMap<Job> map = subqueues.getLookup().asMapEnsure(false,
					type, state);
			return map == null ? Collections.emptySet()
					: map.typedKeySet(Job.class);
		}

		void fireInitialCreationEvents() {
			publish(EventType.CREATED);
			checkFireToProcessing(job);
		}

		public class Event {
			public AllocationQueue queue;

			public EventType type;

			public Transaction transaction;

			public Event(EventType type) {
				this.type = type;
				this.queue = AllocationQueue.this;
				this.transaction = Transaction.current();
			}

			@Override
			public String toString() {
				return type.toString();
			}
		}

		public class QueueStat {
			public int active;

			public int pending;

			public int total;

			public String name;

			public Date startTime;

			public int completed;
		}

		/*
		 * not registered - insert/remove handled by AllocationQueueProjection
		 * 
		 */
		class SubqueueProjection extends BaseProjection<Job> {
			public SubqueueProjection() {
				super(SubqueuePhase.class, JobState.class, jobImplClass);
			}

			@Override
			public Class<? extends Job> getListenedClass() {
				return jobImplClass;
			}

			@Override
			protected int getDepth() {
				return 3;
			}

			@Override
			protected Object[] project(Job job) {
				return new Object[] { provideAllocationKey(job),
						job.resolveState(), job, job };
			}
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

	public enum EventType {
		CREATED, DELETED, RELATED_MODIFICATION, WAKEUP, TO_AWAITING_CHILDREN,
		TO_PROCESSING;
		public boolean isPublishToGlobalQueue() {
			switch (this) {
			case CREATED:
			case DELETED:
				return true;
			default:
				return false;
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

	public static enum SubqueuePhase {
		Self, Child, Sequence, Complete;
	}

	class AllocationQueueProjection implements DomainProjection<Job> {
		private TransactionalMultiset<Class, Job> futuresByTask = new TransactionalMultiset(
				Class.class,
				AlcinaPersistentEntityImpl.getImplementation(Job.class));

		private TransactionalMultiset<Class, Job> incompleteTopLevelByTask = new TransactionalMultiset(
				Class.class,
				AlcinaPersistentEntityImpl.getImplementation(Job.class));

		private TransactionalSet<Job> undeserializableJobs = new TransactionalSet(
				AlcinaPersistentEntityImpl.getImplementation(Job.class));

		public AllocationQueueProjection() {
		}

		public Optional<Job> earliestFuture(Class<? extends Task> key,
				boolean createdBySelf) {
			return futuresByTask.getAndEnsure(key).stream()
					.filter(j -> j.getRunAt() != null)
					.filter(j -> !createdBySelf
							|| j.getCreator() == ClientInstance.self())
					.sorted(new Job.RunAtComparator()).findFirst();
		}

		@Override
		public Class<? extends Job> getListenedClass() {
			return jobImplClass;
		}

		@Override
		public void insert(Job job) {
			/*
			 * avoid deserializing if possible - hence the try/catch
			 */
			try {
				insert0(job);
			} catch (RuntimeException e) {
				if (!job.provideCanDeserializeTask()) {
					undeserializableJobs.add(job);
					return;
				} else {
					throw e;
				}
			}
		}

		@Override
		public boolean isCommitOnly() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public void remove(Job job) {
			/*
			 * avoid deserializing if possible - hence the try/catch
			 */
			try {
				remove0(job);
			} catch (RuntimeException e) {
				if (!job.provideCanDeserializeTask()) {
					undeserializableJobs.add(job);
					return;
				} else {
					throw e;
				}
			}
		}

		@Override
		public void setEnabled(boolean enabled) {
		}

		private AllocationQueue ensureQueue(Job job, AllocationQueue queue) {
			if (queue != null) {
				return queue;
			} else {
				// rather than compute-if-absent - put before event
				synchronized (queues) {
					if (queues.containsKey(job)) {
						return queues.get(job);
					}
					queue = new AllocationQueue(job);
					queues.put(job, queue);
					queue.publish(EventType.CREATED);
				}
				return queue;
			}
		}

		private void insert0(Job job) {
			if (job.getTaskClassName() == null) {
				return;
			}
			AllocationQueue queue = queues.get(job);
			if (job.provideIsFuture()) {
				if (!job.provideCanDeserializeTask()) {
					return;
				}
				futuresByTask.add(job.provideTaskClass(), job);
			} else if (job.provideIsComplete()) {
				if (queue != null) {
					queue.insert(job);
					queue.checkComplete();
				} else {
				}
			} else {
				if (!job.provideCanDeserializeTask()) {
					return;
				}
				queue = ensureQueue(job, queue);
				queue.insert(job);
				if (job.provideIsTopLevel()) {
					incompleteTopLevelByTask.add(job.provideTaskClass(), job);
				}
			}
			Job relatedQueueOwner = job.provideRelatedSubqueueOwner();
			if (relatedQueueOwner == job) {
				return;
			}
			queue = queues.get(relatedQueueOwner);
			/*
			 * 
			 */
			if (relatedQueueOwner.provideIsComplete() && queue == null) {
				return;
			}
			if (!job.provideCanDeserializeTask()) {
				return;
			}
			queue = ensureQueue(relatedQueueOwner, queue);
			queue.insert(job);
		}

		private void remove0(Job job) {
			if (job.getTaskClassName() == null) {
				return;
			}
			AllocationQueue queue = queues.get(job);
			if (job.provideIsFuture()) {
				if (!job.provideCanDeserializeTask()) {
					return;
				}
				futuresByTask.remove(job.provideTaskClass(), job);
			} else if (job.provideIsComplete()) {
				if (queue != null) {
					queue.remove(job);
				} else {
				}
			} else {
				if (queue != null) {
					queue.remove(job);
				}
				if (!job.provideCanDeserializeTask()) {
					return;
				}
				if (job.provideIsTopLevel()) {
					incompleteTopLevelByTask.remove(job.provideTaskClass(),
							job);
				}
			}
			Job relatedQueueOwner = job.provideRelatedSubqueueOwner();
			if (relatedQueueOwner == job) {
				return;
			}
			queue = queues.get(relatedQueueOwner);
			/*
			 * 
			 */
			if (relatedQueueOwner.provideIsComplete() && queue == null) {
				return;
			}
			if (!job.provideCanDeserializeTask()) {
				return;
			}
			queue = ensureQueue(relatedQueueOwner, queue);
			queue.remove(job);
		}
	}

	class JobDescriptor extends DomainClassDescriptor<Job> {
		private AllocationQueueProjection allocationQueueProjection;

		public JobDescriptor() {
			super((Class<Job>) jobImplClass, "taskClassName");
		}

		@Override
		public void initialise() {
			super.initialise();
			allocationQueueProjection = new AllocationQueueProjection();
			projections.add(allocationQueueProjection);
		}
	}
}

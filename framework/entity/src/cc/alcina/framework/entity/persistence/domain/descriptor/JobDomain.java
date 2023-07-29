package cc.alcina.framework.entity.persistence.domain.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.domain.ReverseDateProjection;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ClientInstanceLoadOracle;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.JobStateMessage;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LazyPropertyLoadTask;
import cc.alcina.framework.entity.persistence.mvcc.BaseProjectionSupportMvcc.TreeMapCreatorImpl;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

/**
 * <p>
 * FIXME - mvcc.4 - any non-transactional refs (particularly collections) to
 * mvcc objects should filter by Domain.notRemoved - in fact, just don't have
 * non-transactional refs/collections (if possible) - the sketch factor is just
 * too high.
 * 
 * <p>
 * WIP - this can be backed by a non-DomainStore entitystore
 */
@Registration.Singleton
public class JobDomain {
	public static JobDomain get() {
		return Registry.impl(JobDomain.class);
	}

	private Class<? extends Job> jobImplClass;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private DomainTransformPersistenceListener jobLogger = new DomainTransformPersistenceListener() {
		@Override
		public boolean isAllVmEventsListener() {
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
				logger.trace("Post-process job transform - rq: {}, ids: {}",
						event.getPersistedRequestIds(), ids);
				break;
			}
			case COMMIT_ERROR:
				logger.trace("Issue with job transform details:\n{}",
						event.getTransformPersistenceToken().getRequest());
				break;
			case PRE_COMMIT:
				Set<Long> ids = collation.query(jobImplClass).stream()
						.map(qr -> qr.entityCollation.getId())
						.collect(Collectors.toSet());
				logger.trace("Flushing job transform - rq: {}, ids: {}",
						event.getPersistedRequestIds(), ids);
				break;
			}
		}
	};

	private DomainTransformPersistenceListener bufferedEventFiringListener = new DomainTransformPersistenceListener() {
		@Override
		public boolean isAllVmEventsListener() {
			return true;
		}

		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			switch (event.getPersistenceEventType()) {
			case COMMIT_OK: {
				jobDescriptor.allocationQueueProjection
						.releaseModificationLocks();
				for (AllocationQueue queue : queuesWithBufferedEvents) {
					queue.flushBufferedEvents();
				}
				queuesWithBufferedEvents.clear();
			}
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
	 *
	 * But this does mean that all access should check that the job exists
	 */
	private Map<Job, AllocationQueue> queues = new ConcurrentHashMap<>();

	public Topic<AllocationQueue.Event> queueEvents = Topic.create();

	public Topic<Void> futureConsistencyEvents = Topic.create();

	public Topic<List<JobStateMessage>> stateMessageEvents = Topic.create();

	private Class<? extends JobRelation> jobRelationImplClass;

	private Class<? extends JobStateMessage> jobStateMessageImplClass;

	private DomainTransformPersistenceListener stacktraceRequestListener = new DomainTransformPersistenceListener() {
		@Override
		public boolean isAllVmEventsListener() {
			return true;
		}

		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			AdjunctTransformCollation collation = event
					.getTransformPersistenceToken().getTransformCollation();
			if (!collation.has(jobStateMessageImplClass) || event
					.getPersistenceEventType() != DomainTransformPersistenceEventType.COMMIT_OK) {
				return;
			}
			List<JobStateMessage> stateMessageEvents = collation
					.query(jobStateMessageImplClass).stream()
					.map(qr -> (JobStateMessage) qr.entityCollation.getEntity())
					.collect(Collectors.toList());
			if (stateMessageEvents.size() > 0) {
				/*
				 * Fire off-thread (the handlers will want to commit()
				 * transforms)
				 */
				stateMessageEventQueue.add(stateMessageEvents);
			}
		}
	};

	private StateMessageEventHandler stateMessageEventHandler = new StateMessageEventHandler();

	BlockingQueue<List<JobStateMessage>> stateMessageEventQueue = new LinkedBlockingQueue<>();

	private Set<AllocationQueue> queuesWithBufferedEvents = Collections
			.synchronizedSet(new LinkedHashSet<>());

	public void configureDescriptor(DomainDescriptor descriptor) {
		jobImplClass = PersistentImpl.getImplementation(Job.class);
		jobDescriptor = new JobDescriptor();
		descriptor.addClassDescriptor(jobDescriptor);
		jobRelationImplClass = PersistentImpl
				.getImplementation(JobRelation.class);
		descriptor.addClassDescriptor(jobRelationImplClass);
		jobStateMessageImplClass = PersistentImpl
				.getImplementation(JobStateMessage.class);
		descriptor.addClassDescriptor(jobStateMessageImplClass);
	}

	public void fireInitialAllocatorQueueCreationEvents() {
		queues.values().forEach(AllocationQueue::fireInitialCreationEvents);
	}

	public Stream<? extends Job> getActiveJobs() {
		// subjobs are reachable from two allocationqueues, hence 'distinct'
		cleanupQueues();
		return getVisibleQueues().flatMap(AllocationQueue::getActiveJobs)
				.distinct()
				// FIXME - mvcc.4 - propagation/vacuum issue?
				.peek(j -> {
					if (j.getStartTime() == null) {
						logger.warn("Active job with null start time - {} {}",
								j.getId(), j.getTaskClassName());
					}
				}).filter(j -> j.getStartTime() != null)
				.sorted(Comparator.comparing(Job::getStartTime).reversed());
	}

	public Stream<Job> getAllFutureJobs() {
		return jobDescriptor.allocationQueueProjection.futuresByTask.allItems()
				.stream();
	}

	public Stream<? extends Job> getAllJobs() {
		return Domain.stream(jobImplClass);
	}

	public AllocationQueue getAllocationQueue(Job job) {
		return queues.get(job);
	}

	public Stream<AllocationQueue> getAllocationQueues() {
		cleanupQueues();
		return getVisibleQueues();
	}

	public Optional<Job> getEarliestFuture(Class<? extends Task> key,
			boolean createdBySelf) {
		return jobDescriptor.allocationQueueProjection.earliestFuture(key,
				createdBySelf);
	}

	public Optional<Job> getEarliestIncompleteScheduled(
			Class<? extends Task> key, boolean createdBySelf) {
		return jobDescriptor.allocationQueueProjection.earliestIncomplete(key,
				createdBySelf);
	}

	public Optional<Job> getFutureConsistencyJob(Task task) {
		return jobDescriptor.futureConsistencyTaskProjection
				.getExistingConsistencyJobForTask(task);
	}

	public Stream<Job> getFutureConsistencyJobs() {
		return jobDescriptor.futureConsistencyPriorityProjection.getJobs();
	}

	public Stream<Job> getFutureConsistencyJobs(String consistencyPriority) {
		return jobDescriptor.futureConsistencyPriorityProjection
				.getJobs(consistencyPriority);
	}

	public long getFutureConsistencyJobsCount() {
		return jobDescriptor.futureConsistencyPriorityProjection.getJobsCount();
	}

	public long getFutureConsistencyJobsCount(String consistencyPriority) {
		return jobDescriptor.futureConsistencyPriorityProjection
				.getJobsCount(consistencyPriority);
	}

	public Stream<Job> getFutureConsistencyJobsEquivalentTo(Job job) {
		return jobDescriptor.futureConsistencyTaskProjection
				.getEquivalentTo(job.getTask()).filter(j -> j != job);
	}

	public Map<Class<? extends Task>, Integer>
			getFutureConsistencyTaskCountByTaskClass() {
		return jobDescriptor.futureConsistencyTaskProjection
				.taskCountByTaskClass();
	}

	public Stream<Job> getIncompleteJobs() {
		cleanupQueues();
		return getVisibleQueues().flatMap(AllocationQueue::getIncompleteJobs);
	}

	public Optional<AllocationQueue> getIncompleteQueueContaining(Job job) {
		cleanupQueues();
		return getVisibleQueues()
				.filter(q -> q.getIncompleteJobs().anyMatch(j -> j == job))
				.findFirst();
	}

	public Stream<? extends Job>
			getJobsForTask(Class<? extends Task> taskClass) {
		return getJobsForTask(taskClass, false);
	}

	public Stream<? extends Job> getJobsForTask(Class<? extends Task> taskClass,
			boolean loadAllProperties) {
		DomainQuery query = Domain.query(jobImplClass);
		if (loadAllProperties) {
			query.contextTrue(
					LazyPropertyLoadTask.CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES);
		}
		return query.filter("taskClassName", taskClass.getName()).stream();
	}

	public Stream<? extends Job> getRecentlyCompletedJobs(boolean topLevel) {
		return jobDescriptor.getReverseCompletedJobs(topLevel);
	}

	public Stream<Job> getUndeserializableJobs() {
		return jobDescriptor.allocationQueueProjection.undeserializableJobs
				.stream();
	}

	public void onAppShutdown() {
		stateMessageEventHandler.finished = true;
		stateMessageEventQueue.add(new ArrayList<>());
	}

	public void onDomainWarmupComplete(
			DomainTransformPersistenceListener.Has hasPersistenceListeners) {
		if (Configuration.is("logTransforms")) {
			hasPersistenceListeners
					.addDomainTransformPersistenceListener(jobLogger);
		}
		hasPersistenceListeners.addDomainTransformPersistenceListener(
				stacktraceRequestListener);
		hasPersistenceListeners.addDomainTransformPersistenceListener(
				bufferedEventFiringListener);
		warmupComplete = true;
		if (TransactionEnvironment.get().isMultiple()) {
			Thread stateMessageEventThread = new Thread(
					stateMessageEventHandler,
					"DomainDescriptorJob-stateMessageEventHandler");
			stateMessageEventThread.start();
		}
	}

	public void removeAllocationQueue(Job job) {
		AllocationQueue queue = queues.remove(job);
	}

	private void cleanupQueues() {
		queues.entrySet().removeIf(e -> e.getValue().job.domain().wasRemoved()
				|| e.getValue().job.resolveState() == JobState.ABORTED
				|| e.getValue().job.resolveState() == JobState.CANCELLED);
	}

	private Stream<AllocationQueue> getVisibleQueues() {
		return queues.values().stream()
				.filter(q -> !q.job.domain().wasRemoved());
	}

	/*
	 * This class is essentially a view over the contained SubqueueProjection.
	 * Because the projection is not transactional, access is locked via a
	 * readwrite lock - write lock taken before first modification is processed
	 */
	public class AllocationQueue {
		public Job job;

		public Topic<Event> events = Topic.create();

		SubqueueProjection subqueues = new SubqueueProjection();

		public SubqueuePhase currentPhase = SubqueuePhase.Self;

		List<JobState> incompleteAllocatableStates = Arrays.asList(
				JobState.PENDING, JobState.ALLOCATED, JobState.PROCESSING);

		List<JobState> incompleteAllocatableStatesChild = Arrays.asList(
				JobState.PENDING, JobState.ALLOCATED, JobState.PROCESSING,
				JobState.COMPLETED);

		private boolean firedToProcessing;

		List<Event> bufferedEvents = new ArrayList<>();

		private AllocationQueue parentQueue;

		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

		public AllocationQueue(Job job) {
			this.job = job;
		}

		public QueueStat asQueueStat() {
			QueueStat stat = new QueueStat();
			stat.active = perStateJobCount(JobState.PROCESSING);
			stat.completed = perStateJobCount(JobState.COMPLETED,
					JobState.SEQUENCE_COMPLETE);
			stat.pending = perStateJobCount(JobState.PENDING,
					JobState.ALLOCATED);
			stat.total = stat.active + stat.pending + stat.completed;
			stat.name = job.toDisplayName();
			stat.jobId = String.valueOf(job.getId());
			stat.startTime = job.getStartTime() != null ? job.getStartTime()
					: job.getCreationDate();
			if (stat.startTime == null) {
				// not yet committed
				stat.startTime = new Date();
			}
			return stat;
		}

		public void cancelIncompleteAllocatedJobs() {
			/*
			 * Possible issue with backendtransformqueue
			 *
			 */
			Stream.of(JobState.ALLOCATED, JobState.PROCESSING)
					.forEach(state -> {
						Set<? extends Job> jobs = subQueueJobs(currentPhase,
								state);
						Iterator<? extends Job> itr = jobs.iterator();
						while (itr.hasNext()) {
							Job next = itr.next();
							logger.info("Cancelling from subQueue {}/{} - {}",
									currentPhase, state, next);
							next.cancel();
						}
					});
			Transaction.commit();
		}

		public void checkComplete() {
			if (isComplete()) {
				queues.remove(job);
				publish(EventType.DELETED);
				currentPhase = SubqueuePhase.Complete;
			}
		}

		public void clearIncompleteAllocatedJobs() {
			/*
			 * Deliberately throws an exception (because the sets have no
			 * itr.remove) - this is a response to an upstream problem
			 *
			 * FIXME - mvcc.jobs.2 - review no calls; remove
			 *
			 */
			Stream.of(JobState.ALLOCATED, JobState.PROCESSING)
					.forEach(state -> {
						Set<? extends Job> jobs = subQueueJobs(currentPhase,
								state);
						Iterator<? extends Job> itr = jobs.iterator();
						while (itr.hasNext()) {
							Job next = itr.next();
							logger.info("Removing from subQueue {}/{} - {}",
									currentPhase, state, next);
							itr.remove();
						}
					});
		}

		public AllocationQueue ensureParentQueue() {
			if (parentQueue == null) {
				parentQueue = queues.get(job.provideParent().get());
			}
			return parentQueue;
		}

		public void flushBufferedEvents() {
			bufferedEvents.forEach(this::publish0);
			bufferedEvents.clear();
		}

		public Stream<Job> getActiveJobs() {
			return perStateJobs(JobState.PROCESSING);
		}

		public long getCompletedJobCount() {
			return perPhaseJobCount(JobState.SEQUENCE_COMPLETE);
		}

		public long getIncompleteAllocatedJobCountForCurrentPhaseThisVm() {
			return perPhaseJobs(incompleteStates(currentPhase))
					.filter(j -> j.getPerformer() == ClientInstance.self())
					.count();
		}

		public long getIncompleteJobCountForCurrentPhase() {
			return perPhaseJobCount(incompleteStates(currentPhase));
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

		public boolean hasActive() {
			return perStateJobCount(JobState.PROCESSING) > 0;
		}

		@Override
		public int hashCode() {
			return job.hashCode();
		}

		public void incrementPhase() {
			currentPhase = SubqueuePhase.values()[currentPhase.ordinal() + 1];
		}

		public void insert(Job job) {
			// logger.info("subqueue/insert - {} - {} - {}",
			// this.job.toStringEntity(), subqueues.project(job), job);
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

		// / ahhhhhh....we need to buffer events if in "todomaincommitting",
		// / otherwise we may hit the allocators before the commit is finished
		public void publish(EventType type) {
			Event event = new Event(type);
			if (TransactionEnvironment.get().isToDomainCommitting()) {
				bufferedEvents.add(event);
				queuesWithBufferedEvents.add(this);
			} else {
				publish0(event);
			}
		}

		public void refreshProjection() {
			// TODO Auto-generated method stub
		}

		public void remove(Job job) {
			// logger.info("subqueue/remove - {} - {} - {}",
			// this.job.toStringEntity(), subqueues.project(job), job);
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
			return Ax.format("Allocation Queue - %s - %s\n\t%s", job,
					currentPhase, CommonUtils.joinWithNewlineTab(phaseStates));
		}

		private void checkFireToProcessing(Job job) {
			if (job == this.job && !firedToProcessing) {
				try {
					switch (job.resolveState()) {
					case PROCESSING:
					case COMPLETED:
						firedToProcessing = true;
						publish(EventType.TO_PROCESSING);
						break;
					}
				} catch (Exception e) {
					logger.warn(
							"DEVEX-0 - Fire to processing of (probably) non-visible job - {}",
							job.getId());
					e.printStackTrace();
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
			for (SubqueuePhase subqueuePhase : SubqueuePhase.values()) {
				for (JobState state : incompleteStates(subqueuePhase)) {
					if (subQueueJobs(subqueuePhase, state).size() > 0) {
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
			return Stream.of(currentPhase)
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state)))
					.collect(Collectors.summingInt(Collection::size));
		}

		private Stream<Job> perPhaseJobs(List<JobState> states) {
			return (Stream) Stream.of(currentPhase)
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state))
							.flatMap(Collection::stream));
		}

		private int perStateJobCount(JobState... states) {
			return perStateJobCount(Arrays.asList(states));
		}

		private int perStateJobCount(List<JobState> states) {
			return Arrays.stream(SubqueuePhase.values())
					.flatMap(type -> states.stream()
							.map(state -> subQueueJobs(type, state)))
					.collect(Collectors.summingInt(Collection::size));
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
			try {
				lock.readLock().lock();
				MultikeyMap<Job> map = subqueues.getLookup().asMapEnsure(false,
						type, state);
				return map == null ? Collections.emptySet()
						: map.typedKeySet(Job.class);
			} finally {
				lock.readLock().unlock();
			}
		}

		void fireInitialCreationEvents() {
			publish(EventType.CREATED);
			checkFireToProcessing(job);
		}

		void publish0(Event event) {
			events.publish(event);
			if (event.type.isPublishToGlobalQueue()) {
				queueEvents.publish(event);
			}
		}

		public class Event {
			public AllocationQueue queue;

			public EventType type;

			public TransactionId transactionId;

			public Event(EventType type) {
				this.type = type;
				this.queue = AllocationQueue.this;
				this.transactionId = TransactionEnvironment.get()
						.getCurrentTxId();
			}

			@Override
			public String toString() {
				return Ax.format("job - %s :: %s", queue.job.getId(), type);
			}
		}

		public class QueueStat {
			public int active;

			public int pending;

			public int total;

			public String name;

			public Date startTime;

			public int completed;

			public String jobId;
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

	@Registration.Singleton(ClientInstanceLoadOracle.class)
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

	public enum DefaultConsistencyPriorities {
		high, medium, _default, low
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
		Self, Child, Sequence, Complete
	}

	private final class StateMessageEventHandler implements Runnable {
		volatile boolean finished = false;

		@Override
		public void run() {
			while (!finished) {
				try {
					List<JobStateMessage> messages = stateMessageEventQueue
							.take();
					if (!messages.isEmpty()) {
						Transaction.ensureBegun();
						stateMessageEvents.publish(messages);
						Transaction.commit();
					}
				} catch (Throwable t) {
					t.printStackTrace();
				} finally {
					Transaction.ensureEnded();
				}
			}
		}
	}

	class AllocationQueueProjection implements DomainProjection<Job> {
		private Multiset<Class, Set<Job>> futuresByTask = Registry
				.impl(CollectionCreators.MultisetCreator.class)
				.create(Class.class,
						PersistentImpl.getImplementation(Job.class));

		private Multiset<Class, Set<Job>> incompleteTopLevelByTask = Registry
				.impl(CollectionCreators.MultisetCreator.class)
				.create(Class.class,
						PersistentImpl.getImplementation(Job.class));

		private Set<Job> undeserializableJobs = Registry
				.impl(CollectionCreators.TransactionalSetCreator.class)
				.create(PersistentImpl.getImplementation(Job.class));

		private Set<AllocationQueue> ensuredQueues = new LinkedHashSet<>();

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

		public Optional<Job> earliestIncomplete(Class<? extends Task> key,
				boolean createdBySelf) {
			return incompleteTopLevelByTask.getAndEnsure(key).stream()
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
		public /*
				 * Doesn't try to track result of insertion (i.e. returns null
				 * whether or not the job is present in the projection)
				 */
		void insert(Job job) {
			if (LazyPropertyLoadTask.inLazyPropertyLoad()) {
				// will not affect allocation queues (and wreaks havoc with
				// locking)
				return;
			}
			/*
			 * avoid deserializing if possible - hence the try/catch
			 */
			try {
				insert0(job);
			} catch (RuntimeException e) {
				if (!job.provideCanDeserializeTask()) {
					undeserializableJobs.add(job);
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

		public void releaseModificationLocks() {
			ensuredQueues.forEach(queue -> queue.lock.writeLock().unlock());
			ensuredQueues.clear();
		}

		@Override
		public /*
				 * Doesn't try to track result of removal (i.e. returns null
				 * whether or not the job is present in the projection)
				 */
		void remove(Job job) {
			if (LazyPropertyLoadTask.inLazyPropertyLoad()) {
				// will not affect allocation queues (and wreaks havoc with
				// locking)
				return;
			}
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
			queue = ensureQueue0(job, queue);
			if (TransactionEnvironment.get()
					.isInNonSingleThreadedProjectionState()) {
				if (ensuredQueues.add(queue)) {
					Preconditions.checkState(LooseContext
							.is(DomainStore.CONTEXT_IN_POST_PROCESS));
					queue.lock.writeLock().lock();
				}
			}
			return queue;
		}

		private AllocationQueue ensureQueue0(Job job, AllocationQueue queue) {
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
			if (job.getState() == JobState.FUTURE_CONSISTENCY) {
				return;
			}
			AllocationQueue queue = queues.get(job);
			if (job.provideIsFuture()) {
				if (!job.provideCanDeserializeTask()) {
					logger.warn("Cannot deserialize future task: {}", job);
					logger.info("Task data: {}", job.getTaskSerialized());
					try {
						job.getTask();
					} catch (Exception e) {
						logger.error("Issue", e);
					}
					return;
				} else {
					logger.trace("Adding future: {} {}",
							job.provideTaskClass().getSimpleName(), job);
					futuresByTask.add(job.provideTaskClass(), job);
				}
			} else if (job.provideIsComplete()) {
				if (queue != null) {
					// modification tracking
					ensureQueue(job, queue);
					queue.insert(job);
					// FIXME - at end
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
			if (job.getState() == JobState.FUTURE_CONSISTENCY) {
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
					ensureQueue(job, queue);
					queue.remove(job);
				} else {
				}
			} else {
				if (queue != null) {
					ensureQueue(job, queue);
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

		private CompletedReverseDateProjection reverseDateCompletedTopLevelProjection;

		private FutureConsistencyPriorityProjection futureConsistencyPriorityProjection;

		private FutureConsistencyTaskProjection futureConsistencyTaskProjection;

		private CompletedReverseDateProjection reverseDateCompletedChildProjection;

		public JobDescriptor() {
			super((Class<Job>) jobImplClass, "taskClassName");
		}

		public Stream<Job> getReverseCompletedJobs(boolean topLevel) {
			CompletedReverseDateProjection projection = topLevel
					? reverseDateCompletedTopLevelProjection
					: reverseDateCompletedChildProjection;
			return (Stream<Job>) projection.getLookup().delegate().values()
					.stream();
		}

		@Override
		public void initialise() {
			super.initialise();
			allocationQueueProjection = new AllocationQueueProjection();
			projections.add(allocationQueueProjection);
			reverseDateCompletedTopLevelProjection = new CompletedReverseDateProjection(
					true);
			projections.add(reverseDateCompletedTopLevelProjection);
			reverseDateCompletedChildProjection = new CompletedReverseDateProjection(
					false);
			projections.add(reverseDateCompletedChildProjection);
			futureConsistencyPriorityProjection = new FutureConsistencyPriorityProjection();
			projections.add(futureConsistencyPriorityProjection);
			futureConsistencyTaskProjection = new FutureConsistencyTaskProjection();
			projections.add(futureConsistencyTaskProjection);
		}

		private class CompletedReverseDateProjection
				extends ReverseDateProjection<Job> {
			private boolean topLevel;

			private CompletedReverseDateProjection(boolean topLevel) {
				super(Date.class, new Class[] { (Class<Job>) jobImplClass });
				this.topLevel = topLevel;
			}

			@Override
			public Class<? extends Job> getListenedClass() {
				return jobImplClass;
			}

			@Override
			public void insert(Job t) {
				if (!t.provideIsComplete()) {
					return;
				}
				if (t.provideIsTopLevel() ^ topLevel) {
					return;
				}
				if (t.getEndTime() == null) {
					return;
				}
				if (new Date().getTime() - t.getEndTime().getTime() > 2
						* TimeConstants.ONE_DAY_MS) {
					return;
				}
				super.insert(t);
			}

			@Override
			public void remove(Job t) {
				if (!t.provideIsComplete()) {
					return;
				}
				if (t.provideIsTopLevel() ^ topLevel) {
					return;
				}
				if (t.getEndTime() == null) {
					return;
				}
				super.remove(t);
			}

			@Override
			protected Date getDate(Job job) {
				return job.getEndTime();
			}
		}

		/**
		 * <p>
		 * FIXME - mvcc.5
		 * <p>
		 * Note that this collection is slightly degenerate - *all* the
		 * processed jobs are right at the front of the sorted-by-id list, so as
		 * they're processed they're marked as removed (but not removed since
		 * generally they'll be in the txmap base) - which means iteration has
		 * to traverse a bunch before getting to the first
		 *
		 * <p>
		 * So... rather than doing some fairly hard work on
		 * transactionalmap/iterator, mandate that the second map (in
		 * createLookup) be a pure-concurrent map
		 *
		 * 
		 *
		 */
		private class FutureConsistencyPriorityProjection
				extends BaseProjection<Job> {
			long projectionStart;

			private FutureConsistencyPriorityProjection() {
				super(String.class,
						new Class[] { Long.class, (Class<Job>) jobImplClass });
			}

			public Stream<Job> getJobs() {
				return valueCollections().flatMap(Collection::stream);
			}

			public Stream<Job> getJobs(String consistencyPriority) {
				return valueCollection(consistencyPriority).stream();
			}

			public long getJobsCount() {
				return valueCollections()
						.collect(Collectors.summingInt(Collection::size));
			}

			public long getJobsCount(String consistencyPriority) {
				return valueCollection(consistencyPriority).size();
			}

			@Override
			public Class<? extends Job> getListenedClass() {
				return jobImplClass;
			}

			@Override
			public void insert(Job t) {
				if (t.getState() != JobState.FUTURE_CONSISTENCY) {
					return;
				}
				super.insert(t);
				futureConsistencyEvents.publish(null);
			}

			@Override
			public boolean isCommitOnly() {
				return true;
			}

			@Override
			public void onAddValues(boolean post) {
				Transaction.current().setPopulatingPureTransactional(!post);
				if (post && !EntityLayerUtils.isTestOrTestServer()) {
					Ax.out("Future projection load :: %s ms",
							System.currentTimeMillis() - projectionStart);
				} else {
					projectionStart = System.currentTimeMillis();
				}
			}

			private Collection valueCollection(String consistencyPriority) {
				return getLookup().asMapEnsure(true, consistencyPriority)
						.delegate().values();
			}

			private Stream<Collection> valueCollections() {
				return getLookup().typedKeySet(String.class).stream()
						.sorted(new QueuePriorityComparator())
						.map(s -> getLookup().asMap(s).delegate().values());
			}

			@Override
			protected MultikeyMap<Job> createLookup() {
				return new BaseProjectionLookupBuilder(this)
						.withMapCreators(new CollectionCreators.MapCreator[] {
								Registry.impl(
										CollectionCreators.TreeMapCreator.class)
										.withTypes(Arrays.asList(String.class,
												Object.class)),
								new TreeMapCreatorImpl()
										.withPureTransactional(true)
										.withTypes(Arrays.asList(Long.class,
												Object.class)) })
						.createMultikeyMap();
			}

			@Override
			protected int getDepth() {
				return 2;
			}

			@Override
			protected Object[] project(Job job) {
				return new Object[] { job.provideConsistencyPriority(),
						job.getId(), job };
			}

			class QueuePriorityComparator implements Comparator<String> {
				List<String> ordered = Arrays.asList(Configuration
						.get(JobDomain.class, "consistencyPriorityOrder")
						.split(","));

				@Override
				public int compare(String o1, String o2) {
					int idx1 = ordered.indexOf(o1);
					int idx2 = ordered.indexOf(o2);
					if (idx1 == -1) {
						if (idx2 == -1) {
							return o1.compareTo(o2);
						} else {
							return 1;
						}
					} else {
						if (idx2 == -1) {
							return -1;
						} else {
							return CommonUtils.compareInts(idx1, idx2);
						}
					}
				}
			}
		}

		private class FutureConsistencyTaskProjection
				extends BaseProjection<Job> {
			private FutureConsistencyTaskProjection() {
				super(String.class, new Class[] { String.class, Long.class,
						(Class<Job>) jobImplClass });
			}

			public Stream<Job> getEquivalentTo(Task task) {
				MultikeyMap<Job> map = getLookup().asMapEnsure(false,
						task.getClass().getName(), TransformManager.Serializer
								.get().serialize(task, true));
				return map == null ? Stream.empty()
						: ((Map<Long, Job>) map.delegate()).values().stream()
								.filter(j -> task != j);
			}

			public Optional<Job> getExistingConsistencyJobForTask(Task task) {
				MultikeyMap<Job> map = getLookup().asMapEnsure(false,
						task.getClass().getName(), TransformManager.Serializer
								.get().serialize(task, true));
				return map == null ? Optional.empty()
						: ((Map<Long, Job>) map.delegate()).values().stream()
								.findFirst();
			}

			@Override
			public Class<? extends Job> getListenedClass() {
				return jobImplClass;
			}

			@Override
			public void insert(Job t) {
				if (t.getState() != JobState.FUTURE_CONSISTENCY) {
					return;
				}
				super.insert(t);
			}

			@Override
			public boolean isCommitOnly() {
				return true;
			}

			@Override
			protected int getDepth() {
				return 3;
			}

			@Override
			protected Object[] project(Job job) {
				return new Object[] { job.getTaskClassName(),
						job.getTaskSerialized(), job.getId(), job };
			}

			Map<Class<? extends Task>, Integer> taskCountByTaskClass() {
				Map<Class<? extends Task>, Integer> result = new LinkedHashMap<>();
				getLookup().typedKeySet(String.class).stream().sorted()
						.forEach(className -> {
							Class<? extends Task> taskClass = Reflections
									.forName(className);
							result.put(taskClass,
									getLookup().keys(className).size());
						});
				return result;
			}
		}
	}
}

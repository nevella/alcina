package cc.alcina.framework.servlet.job;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.Job.ResourceRecord;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.JobStateMessage;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.job.TransientFieldTask;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LazyPropertyLoadTask;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.QueueStat;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.ThreadedPmClientInstanceResolverImpl;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutorServiceProvider;

/**
 * <h2>Overview</h2>
 * <p>
 * The alcina jobs system provides scheduling and execution control for
 * clustered and non-clustered jobs.
 * </p>
 * <p>
 * A 'job' persistent object models execution of a Task. Jobs are executed on
 * JobQueues, which are uniquely defined by queue name - one-off queues for
 * arbitrary jobs, and "schedule" queues for jobs which are constrained by a
 * JobScheduler.Schedule. Note the difference between Job and Task - a Task and
 * a Schedule cause the execution of a Job.
 * </p>
 * <p>
 * Important properties related to the execution a job are 'creator' and
 * 'performer' - the clientInstance of the server that created/is allocated the
 * job respectively.
 * </p>
 * <p>
 * JobScheduler.Schedule could well be named 'JobExecutionConstraints", but it's
 * a bit wordy. Currently modelled constraints are:
 * </p>
 * <ul>
 * <li><b>exexcutorServiceProvider</b> - either 'current thread' for an ad-hoc
 * task or an executor service. For example, default recurrent tasks run on the
 * RecurrentJobsExecutorServiceProvider executorService, which is a 4-thread
 * pool by default.
 * <li><b>queueMaxConcurrentJobs</b> - a cluster-level constraint, either
 * MAX_INT or 1 normally (1 results in 'execute in job id order' - for jobs that
 * should logically never run simultaneously because modfiying some shared
 * cluster state)
 * <li><b>next</b> - for timewise limited schedules, return the next run time of
 * the task.
 * <li><b>queueName</b> - concurrent execution scope
 * <li><b>retryPolicy</b> - may be renamed to 'reschedule' policy - handles
 * re-running or -allocating of clustered jobs when the allocated clientInstance
 * is terminated.
 * <li><b>queueName</b> - concurrent execution scope
 * <li><b>clustered</b> - is the schedule vm-local or clustered?
 * <li><b>timewiseLimited</b> - as per JobQueue.allocateJobs (the primary queue
 * loop): <blockquote><code>
 *
 * jobs either time-wise scheduled (limited)
 * maxConcurrent/executor-limited. If time-wise scheduled, only the
 * cluster leader should perform
 * </code></blockquote>
 *
 * </ul>
 *
 * <h2>TODO (doc)</h2> Describe the logic in JobQueue.allocateJobs and
 * JobScheduler a little more fully.
 *
 * <p>
 * FIXME - jobs - activeJobs is a non-transactional view - but possibly should
 * not be, since there are no guarantees that the job exists/has a visible
 * version for a given tx
 *
 * <p>
 * Implementation note - the Job system exists in a JobEnvironment - the default
 * environment is within an Alcina mvcc domain, but other (e.g.
 * non-transactional) environments exist, supporting use on non-db-backed
 * systems (e.g. Android)
 *
 *
 */
@Registrations({
		@Registration(
			value = JobRegistry.class,
			implementation = Registration.Implementation.SINGLETON),
		@Registration(ClearStaticFieldsOnAppShutdown.class) })
public class JobRegistry {
	public static final String CONTEXT_NO_ACTION_LOG = JobRegistry.class
			.getName() + ".CONTEXT_NO_ACTION_LOG";

	public static final String CONTEXT_LAUNCHED_FROM_CONTROL_SERVLET = JobRegistry.class
			.getName() + ".CONTEXT_LAUNCHED_FROM_CONTROL_SERVLET";

	public static final String CONTEXT_DEFAULT_FUTURE_CONSISTENCY_PRIORITY = JobRegistry.class
			.getName() + ".CONTEXT_DEFAULT_FUTURE_CONSISTENCY_PRIORITY";

	public static final String TRANSFORM_QUEUE_NAME = JobRegistry.class
			.getName();

	private static JobRegistry instance = null;

	static Logger logger = LoggerFactory.getLogger(JobRegistry.class);

	public static final int MAX_CAUSE_LENGTH = 240;

	enum LatchType {
		POST_CHILD_COMPLETION, SEQUENCE_COMPLETION
	}

	static void awaitLatch(CountDownLatch latch, LatchType latchType)
			throws InterruptedException {
		long timeout = latchType == LatchType.POST_CHILD_COMPLETION ? 60
				: Configuration.getLong("jobAllocatorSequenceTimeout");
		if (!latch.await(timeout, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Latch timed out - %s seconds");
		}
	}

	private static void checkAnnotatedPermissions(Object o) {
		WebMethod annotation = o.getClass().getAnnotation(WebMethod.class);
		if (annotation != null) {
			if (!PermissionsManager.get().isPermitted(o,
					new AnnotatedPermissible(annotation.customPermission()))) {
				RuntimeException e = new RuntimeException(
						"Permission denied for action " + o);
				EntityLayerLogging.log(LogMessageType.TRANSFORM_EXCEPTION,
						"Domain transform permissions exception", e);
				throw e;
			}
		}
	}

	/*
	 * This info is also observable via JobDomain, but simpler here
	 */
	public static class JobStateChangeObservable implements ProcessObservable {
		public Job job;

		public JobStateChangeObservable(Job job) {
			this.job = job;
		}
	}

	public static Builder createBuilder() {
		return new Builder();
	}

	public static JobRegistry get() {
		if (instance == null) {
			instance = Registry.impl(JobRegistry.class);
		}
		return instance;
	}

	public static boolean isActiveInstance(ClientInstance instance) {
		int retryCount = 0;
		while (retryCount++ < 5) {
			List<ClientInstance> activeInstances = get().jobExecutors
					.getActiveServers();
			boolean contains = activeInstances.contains(instance);
			if (contains) {
				return true;
			}
			if (activeInstances.contains(ClientInstance.self())) {
				return false;
			}
			int minimumVisibleInstancesForOrphanProcessing = Configuration
					.getInt(JobScheduler.class,
							"minimumVisibleInstancesForOrphanProcessing");
			if (activeInstances
					.size() < minimumVisibleInstancesForOrphanProcessing) {
				try {
					Thread.sleep((int) (500 * Math.pow(2, retryCount)));
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			} else {
				return false;
			}
		}
		throw Ax.runtimeException(
				"Unable to determine active instance (timeout): %s",
				instance.toLocator());
	}

	public static boolean isInitialised() {
		return instance != null;
	}

	public static void logLargeResult(Job job) {
		Job populated = job.domain().ensurePopulated();
		Io.log().toFile(populated.getLargeResult().toString());
	}

	public static Job scheduleConsistency(Task task) {
		return createBuilder().withTask(task).ensureConsistency(
				JobDomain.DefaultConsistencyPriorities._default);
	}

	private ConcurrentHashMap<Job, InMemoryResult> inMemoryResults = new ConcurrentHashMap<>();

	private Map<Job, JobContext> activeJobs = new ConcurrentHashMap<>();

	private Map<Job, List<JobResource>> jobResources = new ConcurrentHashMap<>();

	JobScheduler scheduler;

	JobExecutors jobExecutors;

	private boolean stopped;

	private AtomicInteger extJobSystemIdCounter = new AtomicInteger();

	Map<Job, ContextAwaiter> contextAwaiters = new ConcurrentHashMap<>();

	private Job launchedFromControlServlet;

	private AtomicInteger consoleJobIdCounter = new AtomicInteger();

	JobEnvironment environment = new JobEnvironmentTx();

	public JobRegistry() {
	}

	// Directly acquire a resource (not via TaskPerformer.getResources)
	public void acquireResource(Job forJob, JobResource resource) {
		acquireResources(forJob, Collections.singletonList(resource));
	}

	private void acquireResources(Job forJob, List<JobResource> resources) {
		List<JobResource> acquired = jobResources.getOrDefault(forJob,
				new ArrayList<>());
		for (JobResource resource : resources) {
			/*
			 * attempt to acquire from antecedent
			 */
			Optional<JobResource> antecedentAcquired = getAcquiredResource(
					forJob, resource);
			/*
			 * FIXME - domain -
			 *
			 *
			 */
			ProcessState processState = Optional
					.ofNullable(forJob.getProcessState())
					.orElse(new ProcessState()).clone();
			ResourceRecord record = processState.addResourceRecord(resource);
			if (antecedentAcquired.isPresent()) {
				record.setAcquired(true);
				record.setAcquiredFromAntecedent(true);
				forJob.setProcessState(processState);
				Transaction.commit();
			} else {
				forJob.setProcessState(processState);
				Transaction.commit();
				MethodContext.instance().withExecuteOutsideTransaction(true)
						.run(resource::acquire);
				try {
					// ensure lazy (process state) field.
					forJob = forJob.domain().ensurePopulated();
					processState = forJob.getProcessState().clone();
					processState.provideRecord(record).setAcquired(true);
					forJob.setProcessState(processState);
					Transaction.commit();
					acquired.add(resource);
				} catch (Exception e) {
					logger.error("Exception acquiring resource for job {}: {}",
							resource.getPath(), e);
					resource.release();
					throw new WrappedRuntimeException(e);
				}
			}
		}
		if (acquired.size() > 0) {
			jobResources.put(forJob, acquired);
		}
	}

	public Job await(Job job) throws InterruptedException {
		return await(job, 0);
	}

	public Job await(Job job, long maxTime) throws InterruptedException {
		if (JobContext.has()) {
			Job contextJob = JobContext.get().getJob();
			if (contextJob.provideDescendants().noneMatch(j -> j == job)) {
				TransactionEnvironment.withDomain(() -> {
					TransactionEnvironment.get().ensureBegun();
					contextJob.createRelation(job, JobRelationType.AWAITED);
					TransactionEnvironment.get().commit();
				});
			}
		}
		long start = System.currentTimeMillis();
		ContextAwaiter awaiter = ensureAwaiter(job);
		TransactionEnvironment.get().commit();
		awaiter.await(maxTime);
		JobContext jobContext = activeJobs.get(job);
		contextAwaiters.remove(job);
		if (maxTime != 0 && System.currentTimeMillis() - start > maxTime) {
			TransactionEnvironment.withDomain(() -> {
				job.cancel();
				Transaction.commit();
			});
			TransactionEnvironment.withDomain(() -> {
				JobContext.checkCancelled();
			});
		}
		jobContext.awaitSequenceCompletion();
		DomainStore.waitUntilCurrentRequestsProcessed();
		return TransactionEnvironment
				.withDomain(() -> job.domain().ensurePopulated());
	}

	public String dumpActiveJobsThisInstance() {
		return MethodContext.instance().withWrappingTransaction()
				.call(() -> activeJobs.keySet().stream()
						.filter(Objects::nonNull).map(j -> {
							return Ax.format("%s\n\t%s", j.toString(),
									j.getTask());
						}).collect(Collectors.joining("\n")));
	}

	public ContextAwaiter ensureAwaiter(Job job) {
		return contextAwaiters.computeIfAbsent(job, ContextAwaiter::new);
	}

	/*
	 * Ensure there is exactly one pending job (which takes care of changes
	 * affecting the model which will be missed by an in-flight)
	 */
	public Job ensureScheduled(Task task, boolean scheduleAfterInFlight) {
		Ref<Job> jobRef = new Ref<>();
		String serialized = TransformManager.serialize(task, true);
		withJobMetadataLock(task.getClass().getName(), () -> {
			Optional<? extends Job> pending = JobDomain.get()
					.getJobsForTask(task.getClass())
					.filter(j -> j.getState() == JobState.PENDING)
					.filter(j -> Objects.equals(serialized,
							j.getTaskSerialized()))
					.findFirst();
			if (pending.isPresent()) {
				jobRef.set(pending.get());
				return;
			}
			Job job = task.schedule();
			if (scheduleAfterInFlight) {
				Optional<? extends Job> inFlight = JobDomain.get()
						.getJobsForTask(task.getClass())
						.filter(j -> j.getState() == JobState.ALLOCATED
								|| j.getState() == JobState.PROCESSING)
						.filter(j -> Objects.equals(serialized,
								j.getTaskSerialized()))
						.findFirst();
				if (inFlight.isPresent()) {
					inFlight.get().createRelation(job,
							JobRelationType.SEQUENCE);
				}
			}
			jobRef.set(job);
		});
		return jobRef.get();
	}

	<JR extends JobResource> Optional<JR> getAcquiredResource(Job forJob,
			JR resource) {
		return forJob.provideSelfAndAntecedents()
				.map(j -> getResource(j, resource, forJob))
				.filter(Objects::nonNull).findFirst();
	}

	public Stream<? extends Job> getActiveConsistencyJobs() {
		return scheduler.aMoreDesirableSituation == null ? Stream.of()
				: scheduler.aMoreDesirableSituation.getActiveJobs();
	}

	/*
	 * PROCESSING or COMPLETE (not SEQUENCE_COMPLETE), this vm
	 */
	public int getActiveJobCount() {
		// FIXME - mvcc.jobs - both (a) remove on transform, not here and (b)
		// track why not removed in this-vm process (i.e. finally of performJob0
		// not completing).
		// How to track: put logging here (DEVEX)
		activeJobs.keySet().removeIf(job -> {
			try {
				// this is more a guard against exceptions rather than logically
				// correct - correctness would be a txview
				return Mvcc.isVisible(job)
						&& job.getState() == JobState.ABORTED;
			} catch (Exception e) {
				// FIXME - devex - presumably mvcc-deleted
				e.printStackTrace();
				return false;
			}
		});
		return activeJobs.size();
	}

	public Stream<QueueStat> getActiveQueueStats() {
		return JobDomain.get().getAllocationQueues()
				.filter(AllocationQueue::hasActive)
				.map(AllocationQueue::asQueueStat).sorted(Comparator
						.comparing(stat -> -stat.startTime.getTime()));
	}

	JobContext getContext(Job job) {
		return activeJobs.get(job);
	}

	public JobEnvironment getEnvironment() {
		return this.environment;
	}

	public String getExJobSystemNextJobId(Class<?> clazz) {
		return Ax.format("%s::%s::%s::%s", clazz.getName(),
				EntityLayerUtils.getLocalHostName(),
				EntityLayerObjects.get().getServerAsClientInstance().getId(),
				extJobSystemIdCounter.incrementAndGet());
	}

	public Stream<FutureStat> getFutureQueueStats() {
		return JobDomain.get().getAllFutureJobs().map(FutureStat::new);
	}

	public Timestamp getJobMetadataLockTimestamp(String path) {
		return jobExecutors.getJobMetadataLockTimestamp(path);
	}

	public String getLargeResult(Job job) {
		return inMemoryResults.remove(job).result;
	}

	public Job getLaunchedFromControlServlet() {
		return this.launchedFromControlServlet;
	}

	public List<JobTracker> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		Set<Long> ids = JobDomain.get().getJobsForTask(action.getClass(), false)
				.sorted(EntityComparator.REVERSED_INSTANCE).limit(count)
				.collect(EntityHelper.toIdSet());
		return Domain.query(PersistentImpl.getImplementation(Job.class))
				.contextTrue(
						LazyPropertyLoadTask.CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES)
				.filterByIds(ids).stream().map(Job::asJobTracker)
				.collect(Collectors.toList());
	}

	public String getPerformerThreadName(Job job) {
		return Optional.ofNullable(activeJobs.get(job)).map(jc -> jc.thread)
				.map(Thread::getName).orElse(null);
	}

	<JR extends JobResource> JR getResource(Job acquiredByJob, JR matching,
			Job forJob) {
		List<JobResource> resources = jobResources.get(acquiredByJob);
		if (resources != null) {
			Optional<JR> resource = resources.stream()
					.filter(r -> r.equals(matching)).map(r -> (JR) r)
					.findFirst();
			if (resource.isPresent()) {
				if (acquiredByJob.provideIsSibling(forJob)
						&& resource.get().isSharedWithSubsequents()) {
					return resource.get();
				}
				if (resource.get().isSharedWithChildren()) {
					return resource.get();
				}
			}
		}
		return null;
	}

	public Object getResourceOwner() {
		return JobContext.has()
				? JobContext.get().getJob().provideFirstInSequence()
				: Thread.currentThread();
	}

	protected TaskPerformer getTaskPerformer(Job job) {
		Task task = job.getTask();
		TaskPerformer performer = null;
		if (task instanceof TaskPerformer) {
			performer = (TaskPerformer) task;
		} else {
			Optional<TaskPerformer> o_performer = Registry
					.optional(TaskPerformer.class, task.getClass());
			if (o_performer.isPresent()) {
				performer = o_performer.get();
			} else {
				performer = new MissingPerformerPerformer();
			}
		}
		if (performer instanceof HasRoutingPerformer) {
			performer = ((HasRoutingPerformer) performer).routingPerformer()
					.route(performer);
		}
		return performer;
	}

	public List<Job> getThreadData(Job job) {
		ThreadDataWaiter threadDataWaiter = new ThreadDataWaiter(job);
		threadDataWaiter.await();
		return threadDataWaiter.queriedJobs;
	}

	public void init() {
		// max 200, since we want the first job status message to be max 200ms
		// from job start (and it's debounced)
		TransformCommit.get()
				.setBackendTransformQueueMaxDelay(TRANSFORM_QUEUE_NAME, 200);
		jobExecutors = Registry.impl(JobExecutors.class);
		jobExecutors.addScheduledJobExecutorChangeConsumer(leader -> {
			if (leader && scheduler != null) {
				scheduler.enqueueLeaderChangedEvent();
			}
		});
		scheduler = new JobScheduler(this);
		JobDomain.get().stateMessageEvents.add(messages -> {
			for (JobStateMessage message : messages) {
				if (message == null) {
					// FIXME - mvcc.5 - should not be
					continue;
				}
				if (message.getProcessState() == null
						&& activeJobs.containsKey(message.getJob())) {
					updateThreadData(message);
				}
			}
		});
	}

	public boolean isActiveCreator(Job job) {
		return jobExecutors.getActiveServers().contains(job.getCreator());
	}

	/*
	 * Awaits completion of the task and any sequential (cascaded) tasks
	 */
	public Job perform(Task task) {
		return perform(task, 0L);
	}

	/*
	 * Awaits completion of the task and any sequential (cascaded) tasks
	 */
	public Job perform(Task task, long timeoutMillis) {
		try {
			/*
			 * Check that the current job (if any) allows this concurrent task
			 * (a deadlock check, essentially)
			 */
			if (JobContext.has()) {
				if (!JobContext.get().getPerformer()
						.checkCanPerformConcurrently(task)) {
					throw Ax.runtimeException(
							"(Deadlock prevention) Task %s cannot be performed from Job %s",
							task, JobContext.get().getJob());
				}
			}
			Job job = TransactionEnvironment.withDomain(() -> createBuilder()
					.withTask(task).withAwaiter().create());
			if (LooseContext.has(CONTEXT_LAUNCHED_FROM_CONTROL_SERVLET)) {
				// FIXME - use job creation/completion topics
				launchedFromControlServlet = job;
				LooseContext.remove(CONTEXT_LAUNCHED_FROM_CONTROL_SERVLET);
			}
			return await(job, timeoutMillis);
		} catch (Exception e) {
			e.printStackTrace();
			throw WrappedRuntimeException.wrap(e);
		}
	}

	/*
	 * Jobs are always run in new (or job-only) threads
	 *
	 * FIXME - mvcc.jobs.1a - launcherthreadstate should go away
	 */
	void performJob(Job job, boolean queueJobPersistence,
			LauncherThreadState launcherThreadState,
			ExecutorServiceProvider executorServiceProvider,
			ExecutorService executorService) {
		try {
			if (environment.isInTransactionMultipleTxEnvironment()) {
				logger.warn(
						"DEVEX::0 - JobRegistry.performJobInTx - begin with open transaction "
								+ " - {}\nuncommitted transforms:\n{}",
						job, TransformManager.get().getTransforms());
				try {
					Transaction.commit();
				} catch (Exception e) {
					logger.warn("DEVEX::0 - JobRegistry.performJob", e);
					e.printStackTrace();
				}
				Transaction.ensureEnded();
			}
			LooseContext.push();
			LooseContext.set(
					ThreadedPmClientInstanceResolverImpl.CONTEXT_CLIENT_INSTANCE,
					EntityLayerObjects.get().getServerAsClientInstance());
			DomainTransformPersistenceEvents
					.setLocalCommitTimeout(120 * TimeConstants.ONE_SECOND_MS);
			Thread.currentThread().setContextClassLoader(
					launcherThreadState.contextClassLoader);
			launcherThreadState.copyContext
					.forEach((k, v) -> LooseContext.set(k, v));
			TransactionEnvironment.get().begin();
			environment.prepareUserContext(job);
			performJob0(job, queueJobPersistence, launcherThreadState);
			logger.info("Job complete - {}", job);
		} catch (RuntimeException e) {
			if (stopped) {
				// app shutdown - various services will not be available -
				// ignore
			} else {
				// will generally be close to the top of a thread - so log, even
				// if there's logging higher
				logger.warn(Ax.format("DEVEX::0 - JobRegistry.performJob - %s",
						job), e);
				e.printStackTrace();
			}
		} finally {
			PermissionsManager.get().popUser();
			TransactionEnvironment.get().end();
			LooseContext.pop();
			LooseContext.confirmDepth(0);
			executorServiceProvider.onServiceComplete(executorService);
		}
	}

	// FIXME - jobs - clean
	//
	// first - move to a jobperformer class
	//
	// then - have performer phases
	//
	// then - manage persistence/threads better
	//
	// then - allocator
	private <T extends Task> void performJob0(Job job,
			boolean queueJobPersistence,
			LauncherThreadState launcherThreadState) {
		if (trackInternalMetrics()) {
			InternalMetrics.get().startTracker(job,
					() -> job.getTaskSerialized(), InternalMetricTypeAlcina.job,
					job.toDisplayName(), () -> true);
		}
		TaskPerformer<T> performer = getTaskPerformer(job);
		JobContext context = new JobContext(job, performer, launcherThreadState,
				scheduler.awaitAllocator(job));
		activeJobs.put(job, context);
		if (contextAwaiters.containsKey(job)) {
			ContextAwaiter contextAwaiter = contextAwaiters.get(job);
			contextAwaiter.copyContext
					.forEach((k, v) -> LooseContext.set(k, v));
			contextAwaiter.latch.countDown();
		}
		boolean taskEnabled = !Configuration.properties
				.has(Ax.format("%s.disabled", job.getTaskClassName()))
				&& !Configuration.is("allJobsDisabled");
		boolean deferMetadataPersistence = performer
				.deferMetadataPersistence(job);
		try {
			LooseContext.push();
			context.start();
			withDomain(deferMetadataPersistence, () -> context.persistStart());
			context.beginLogBuffer();
			if (taskEnabled) {
				LooseContextInstance contextSnapshot = LooseContext.getContext()
						.snapshot();
				try {
					acquireResources(job, performer.getResources());
					performer.performAction((T) job.getTask());
				} finally {
					// handle any unbalanced stack issues (which would generally
					// be fatal to the job handling, causing indeterminate
					// effects )
					LooseContext.restore(contextSnapshot);
				}
			} else {
				logger.info("Not performing {} (disabled)", job);
			}
			withDomain(deferMetadataPersistence, () -> {
				context.persistMetadata();
				context.toAwaitingChildren();
			});
			context.awaitChildCompletion();
			performer.onChildCompletion();
		} catch (Throwable t) {
			Exception e = (Exception) ((t instanceof Exception) ? t
					: new WrappedRuntimeException(t));
			e.printStackTrace();
			/*
			 * May have arrived here as a result of a failed transform commit
			 */
			Transaction.ensureEnded();
			Transaction.begin();
			withDomain(deferMetadataPersistence, () -> {
				context.onJobException(e);
				if (CommonUtils.extractCauseOfClass(e,
						CancelledException.class) != null) {
				} else {
					logger.warn(Ax.format("Job exception in job %s", job), t);
					if (!Ax.isTest()) {
						EntityLayerLogging.persistentLog(
								LogMessageType.TASK_EXCEPTION, e);
					}
				}
			});
		} finally {
			context.endLogBuffer();
			context.restoreThreadName();
			/*
			 * key - handle tx timeouts/aborts
			 */
			withDomain(deferMetadataPersistence, () -> {
				try {
					LooseContext.remove(
							ThreadlocalTransformManager.CONTEXT_THROW_ON_RESET_TLTM);
					Transaction.ensureBegun();
					performer.onBeforeEnd();
					context.end();
					performer.onAfterEnd();
					releaseResources(job, false);
					context.persistMetadata();
					activeJobs.remove(job);
					context.clearRefs();
					context.remove();
					if (trackInternalMetrics()) {
						InternalMetrics.get().endTracker(job);
					}
					new JobStateChangeObservable(job).publish();
				} catch (Throwable e) {
					logger.warn("DEVEX::0 - JobRegistry.performJob0.finally",
							e);
					e.printStackTrace();
				}
			});
			LooseContext.pop();
		}
	}

	public void processOrphans() {
		// manual because auto-scheduling generally disabled for dev
		// server/consoles
		Preconditions.checkState(Ax.isTest());
		scheduler.processOrphans();
	}

	private void releaseResources(Job job, boolean inSequenceRelease) {
		List<JobResource> resources = jobResources.get(job);
		if (resources != null) {
			Iterator<JobResource> itr = resources.iterator();
			while (itr.hasNext()) {
				JobResource resource = itr.next();
				boolean release = true;
				if (job.provideHasIncompleteSubsequent()
						&& resource.isSharedWithSubsequents()) {
					release = false;
				}
				if (release) {
					resource.release();
					itr.remove();
				}
			}
			if (resources.isEmpty()) {
				jobResources.remove(job);
			}
		}
		if (!inSequenceRelease && !job.provideHasIncompleteSubsequent()) {
			// at end of sequence, re-release resources for all jobs in the
			// sequence
			List<Job> relatedSequential = job.provideRelatedSequential();
			for (Job related : relatedSequential) {
				releaseResources(related, true);
			}
		}
	}

	public void setEnvironment(JobEnvironment environment) {
		this.environment = environment;
	}

	// for tooling
	public void startNonPersistentJobContext(Task task) {
		Job job = Reflections
				.newInstance(PersistentImpl.getImplementation(Job.class));
		job.setId(consoleJobIdCounter.decrementAndGet());
		job.setState(JobState.PENDING);
		job.setTask(task);
		TaskPerformer performer = getTaskPerformer(job);
		JobContext jobContext = new JobContext(job, performer, null, null);
		jobContext.start();
		jobContext.beginLogBuffer();
	}

	public void stopService() {
		stopped = true;
		scheduler.stopService();
	}

	protected boolean trackInternalMetrics() {
		return Configuration.is("trackInternalMetrics");
	}

	private void updateThreadData(JobStateMessage message) {
		logger.info("Checking thread data for job {}", message.getJob());
		JobContext context = activeJobs.get(message.getJob());
		if (context != null) {
			logger.info("Populating thread data for job {}", message.getJob());
			ProcessState processState = message.ensureProcessState();
			context.updateProcessState(processState);
			message.persistProcessState();
		}
	}

	public void wakeupScheduler() {
		scheduler.fireWakeup();
	}

	private void withDomain(boolean deferMetadataPersistence,
			Runnable runnable) {
		try {
			LooseContext.push();
			if (deferMetadataPersistence) {
				LooseContext.setTrue(
						ThreadlocalTransformManager.CONTEXT_THROW_ON_RESET_TLTM);
			}
			TransactionEnvironment.withDomain(runnable);
		} finally {
			LooseContext.pop();
		}
	}

	public Object withJobMetadataLock(Job job, Runnable runnable) {
		return withJobMetadataLock(job.toLocator().toRecoverableNumericString(),
				runnable);
	}

	public Object withJobMetadataLock(String path, Runnable runnable) {
		if (runnable == null) {
			return null;
		}
		try {
			Object lock = jobExecutors.allocationLock(path, true);
			runnable.run();
			return lock;
		} finally {
			jobExecutors.allocationLock(path, false);
		}
	}

	@Registration.Singleton
	public static class ActionPerformerTrackMetrics
			implements Supplier<Boolean> {
		@Override
		public Boolean get() {
			return true;
		}
	}

	public static class Builder {
		private Task task;

		private LocalDateTime runAt;

		private Job related;

		private JobRelationType relationType;

		private JobState initialState = JobState.PENDING;

		private Job lastCreated = null;

		private boolean awaiter;

		private String cause;

		public Job addSibling(Task task) {
			// only run to add a sibling to a child job - otherwise use
			// followWith()
			Preconditions.checkState(JobContext.has());
			this.task = task;
			withContextParent();
			return create();
		}

		/**
		 * This should be the only route for the creation of persistent jobs
		 * (i.e. don't call {@code PersistentImpl.create(Job.class) elsewhere}
		 */
		public Job create() {
			checkAnnotatedPermissions(task);
			Job job = PersistentImpl.create(Job.class);
			job.setUser(PermissionsManager.get().getUser());
			job.setState(initialState);
			try {
				LooseContext.push();
				AlcinaTransient.Support
						.setTransienceContexts(TransienceContext.JOB);
				job.setTask(task);
			} finally {
				LooseContext.pop();
			}
			JobEnvironment environment = JobRegistry.get().environment;
			// NOTE - not (!!) ClientInstance.self(), which would be the browser
			// instance if the job call were initiated remotely. The job system
			// cares about the server creator/performer, not the client.
			ClientInstance serverInstance = environment.getPerformerInstance();
			job.setCreator(serverInstance);
			// very useful for job cascade/trigger debugging
			job.setUuid(Ax.format("%s.%s", serverInstance.getId(),
					job.getLocalId()));
			job.setCause(cause);
			if (runAt != null) {
				Preconditions.checkArgument(initialState == JobState.FUTURE);
			}
			if (initialState == JobState.FUTURE_CONSISTENCY) {
				String consistencyPriority = JobDomain.DefaultConsistencyPriorities._default
						.toString();
				if (LooseContext
						.has(CONTEXT_DEFAULT_FUTURE_CONSISTENCY_PRIORITY)) {
					consistencyPriority = LooseContext
							.get(CONTEXT_DEFAULT_FUTURE_CONSISTENCY_PRIORITY)
							.toString();
				}
				if (!PermissionsManager.isSystemUser()) {
					// raise the priority of jobs that are directly caused by
					// non-system users
					consistencyPriority = JobDomain.DefaultConsistencyPriorities.high
							.toString();
				}
				// ensure there's always a string value. This enables later
				// consumers of job metadata to know the job was a 'consistency
				// job', irrespective of JobState
				job.setConsistencyPriority(consistencyPriority);
			}
			job.setRunAt(SEUtilities.toOldDate(runAt));
			if (related != null) {
				related.createRelation(job, relationType);
			}
			/*
			 * To elaborate - but essentially a top-level job *must* have its
			 * performer defined early if it's not a FUTURE
			 */
			if (initialState == JobState.PENDING && (related == null
					|| relationType == JobRelationType.RESUBMIT)) {
				job.setPerformer(serverInstance);
			}
			if (awaiter) {
				JobRegistry.get().ensureAwaiter(job);
			}
			if (task instanceof TransientFieldTask) {
				TransientFieldTasks.get().registerTask(job, task);
			}
			lastCreated = job;
			task.onJobCreate(job);
			environment.onJobCreated(job);
			LogCreation logCreation = LogCreation.valueOf(
					Configuration.get(JobRegistry.class, "logJobCreation"));
			switch (logCreation) {
			case NONE:
				break;
			case JOB:
				logger.info("Job created: {}", job);
				break;
			case STACK:
				logger.info("Job created: {}", job);
				logger.info("Creation thread: \n{}\n\n",
						SEUtilities.getFullStacktrace(Thread.currentThread()));
				break;
			default:
				throw new UnsupportedOperationException();
			}
			new JobStateChangeObservable(job).publish();
			return job;
		}

		public Builder createReturnBuilder() {
			create();
			return this;
		}

		/**
		 * @return the job if job is created in this method call, otherwise null
		 */
		public Job ensureConsistency(Object futureConsistencyPriority) {
			Optional<Job> existing = JobDomain.get()
					.getFutureConsistencyJob(task);
			if (existing.isPresent()) {
				if (futureConsistencyPriority != JobDomain.DefaultConsistencyPriorities._default) {
					existing.get().setConsistencyPriority(
							futureConsistencyPriority.toString());
				}
				return null;
			} else {
				Job job = withInitialState(JobState.FUTURE_CONSISTENCY)
						.create();
				job.setConsistencyPriority(
						futureConsistencyPriority.toString());
				logger.info("created-future-consistency - {}", job);
				return job;
			}
		}

		public Job followWith(Task task) {
			this.task = task;
			relationType = JobRelationType.SEQUENCE;
			Preconditions.checkArgument(lastCreated != null);
			related = lastCreated;
			create();
			return lastCreated;
		}

		public void pruneCompletedResubmittedChildren() {
			Preconditions.checkNotNull(lastCreated);
			Job cursor = lastCreated.root();
			List<Job> completed = new ArrayList<>();
			while (cursor != null) {
				cursor = cursor.getToRelations().stream().filter(
						rel -> rel.getType() == JobRelationType.RESUBMIT)
						.findFirst().map(JobRelation::getFrom).orElse(null);
				if (cursor != null) {
					cursor.provideChildren()
							.filter(Job::provideIsCompletedNormally)
							.forEach(completed::add);
				}
			}
			while (true) {
				Job hasEquivalentCompleted = lastCreated.root()
						.provideChildren().sorted(EntityComparator.INSTANCE)
						.filter(job -> completed.stream()
								.anyMatch(job::provideEquivalentTask))
						.findFirst().orElse(null);
				if (hasEquivalentCompleted != null) {
					Job equivalentCompleted = completed.stream().filter(
							hasEquivalentCompleted::provideEquivalentTask)
							.findFirst().get();
					logger.info("pruneCompletedResubmittedChildren - "
							+ "root {} - job {} - equivalent completed child: {}",
							lastCreated.root(), hasEquivalentCompleted,
							equivalentCompleted);
					hasEquivalentCompleted.deleteEnsuringSequence();
				} else {
					break;
				}
			}
		}

		public Builder withAwaiter() {
			awaiter = true;
			return this;
		}

		public Builder withCause(String cause) {
			// will be persisted to a varchar
			if (cause.length() > MAX_CAUSE_LENGTH) {
				logger.warn("Truncating job cause: {}", cause);
				cause = Ax.trim(cause, MAX_CAUSE_LENGTH);
			}
			this.cause = cause;
			return this;
		}

		public Builder withContextParent() {
			this.related = JobContext.get().getJob();
			Preconditions.checkNotNull(related);
			this.relationType = JobRelationType.PARENT_CHILD;
			return this;
		}

		public Builder withContextPrevious() {
			this.related = JobContext.get().getJob();
			Preconditions.checkNotNull(related);
			this.relationType = JobRelationType.SEQUENCE;
			return this;
		}

		public Builder withInitialState(JobState initialState) {
			this.initialState = initialState;
			return this;
		}

		public Builder withRelated(Job related) {
			this.related = related;
			return this;
		}

		public Builder withRelationType(JobRelationType relationType) {
			this.relationType = relationType;
			return this;
		}

		public Builder withRunAt(LocalDateTime runAt) {
			this.runAt = runAt;
			if (runAt != null) {
				this.initialState = JobState.FUTURE;
			}
			return this;
		}

		public Builder withTask(Task task) {
			this.task = task;
			return this;
		}

		/*
		 * Ensures a job of the task class is scheduled (the method will only
		 * create a Job if there's no scheduled job of this task class)
		 */
		public Job ensureScheduled() {
			Preconditions.checkState(runAt != null);
			Optional<Job> earliestFuture = JobDomain.get()
					.getEarliestFuture(task.getClass(), false);
			if (earliestFuture.isPresent()) {
				return null;
			} else {
				return create();
			}
		}
	}

	static class ContextAwaiter {
		CountDownLatch latch = new CountDownLatch(1);

		Map<String, Object> copyContext = new LinkedHashMap<>();

		private Job job;

		public ContextAwaiter(Job job) {
			this.job = job;
			// we don't copy permissions manager/user - since often the launcher
			// will be triggered by a system user context (because triggered by
			// a transaction event)
			//
			// instead, all tasks for which runAsRoot returns false must
			// implement iuser
			copyContext.putAll(LooseContext.getContext().getProperties());
		}

		public void await(long maxTime) {
			try {
				maxTime = maxTime == 0 ? TimeConstants.ONE_HOUR_MS : maxTime;
				long start = System.currentTimeMillis();
				// do as a loop (rather than a simple wait) to avoid
				// synchronisation (no guarantee job.provideIsComplete is
				// doesn't change before latch.await)
				while (maxTime == 0
						|| System.currentTimeMillis() - start < maxTime) {
					if (job.provideIsComplete()) {
						break;
					}
					Transaction.ensureEnded();
					long waitMillis = 1 * TimeConstants.ONE_SECOND_MS;
					if (maxTime != 0 && maxTime < waitMillis) {
						waitMillis = maxTime;
					}
					latch.await(waitMillis, TimeUnit.MILLISECONDS);
					Transaction.begin();
					if (job.provideIsComplete() || latch.getCount() <= 0) {
						break;
					}
					// if awaiting during job performance
					JobContext.checkCancelled();
					long seconds = (System.currentTimeMillis() - start) / 1000;
					// log on power-of-2 seconds
					OptionalInt log = IntStream.range(0, 16)
							.map(exp -> 1 << exp).filter(i -> i == seconds)
							.findFirst();
					if (log.isPresent()) {
						JobRegistry.logger.warn("Waiting for job ({} secs) {}",
								log.getAsInt(), job);
					}
				}
				if (latch.getCount() > 0) {
					JobRegistry.logger.warn(
							"DEVEX - 0 - Timed out waiting for job {}", job);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static class FutureStat {
		public String taskName;

		public Date runAt;

		public long jobId;

		FutureStat(Job job) {
			taskName = job.getTaskClassName();
			runAt = job.getRunAt();
			jobId = job.getId();
		}
	}

	static class InMemoryResult {
		String result;

		Job job;

		InMemoryResult(String result, Job job) {
			this.result = result;
			this.job = job;
		}

		void record() {
			get().inMemoryResults.put(job, this);
		}
	}

	@Registration(JobExecutors.class)
	public static class JobExecutorsSingle implements JobExecutors {
		@Override
		public void addScheduledJobExecutorChangeConsumer(
				Consumer<Boolean> consumer) {
		}

		@Override
		public Object allocationLock(String path, boolean acquire) {
			return new Object();
		}

		@Override
		public List<ClientInstance> getActiveServers() {
			return Arrays.asList(
					EntityLayerObjects.get().getServerAsClientInstance());
		}

		@Override
		public boolean isCurrentScheduledJobExecutor() {
			return true;
		}

		@Override
		public boolean isHighestBuildNumberInCluster() {
			return true;
		}
	}

	static class LauncherThreadState {
		ClassLoader contextClassLoader;

		Map<String, Object> copyContext = new LinkedHashMap<>();

		String launchingThreadName;

		long launchingThreadId;

		public LauncherThreadState() {
			// we don't copy permissions manager/user - since often the launcher
			// will be triggered by a system user context (because triggered by
			// a transaction event)
			//
			// instead, all tasks for which runAsRoot returns false must
			// implement iuser
			Thread currentThread = Thread.currentThread();
			launchingThreadId = currentThread.getId();
			launchingThreadName = currentThread.getName();
			copyContext.putAll(LooseContext.getContext().getProperties());
			contextClassLoader = currentThread.getContextClassLoader();
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}
	}

	public enum LogCreation {
		NONE, JOB, STACK
	}

	static class MissingPerformerPerformer implements TaskPerformer {
		@Override
		public void performAction(Task task) throws Exception {
			throw new Exception(Ax.format("No performer found for task %s",
					task.getClass().getName()));
		}
	}

	@Registration.Singleton(Task.Performer.class)
	public static class Performer implements Task.Performer {
		@Override
		public Job ensurePending(Task task) {
			return get().ensureScheduled(task, true);
		}

		@Override
		public Job perform(Task task) {
			return get().perform(task);
		}

		@Override
		public Job schedule(Task task) {
			return createBuilder().withTask(task).create();
		}
	}

	class ThreadDataWaiter {
		List<Job> queriedJobs = new ArrayList<>();

		private Job job;

		private CountDownLatch latch;

		private TopicListener<List<JobStateMessage>> listener = messages -> {
			for (JobStateMessage message : messages) {
				if (message.getProcessState() != null
						&& queriedJobs.contains(message.getJob())) {
					latch.countDown();
				}
			}
		};

		public ThreadDataWaiter(Job job) {
			this.job = job;
		}

		public void await() {
			Stream.concat(Stream.of(job), job.provideDescendants())
					.filter(j -> j.getState() == JobState.PROCESSING)
					.forEach(job -> {
						queriedJobs.add(job);
						JobStateMessage stateMessage = PersistentImpl
								.create(JobStateMessage.class);
						stateMessage.setJob(job);
					});
			try {
				JobDomain.get().stateMessageEvents.add(listener);
				latch = new CountDownLatch(queriedJobs.size());
				Transaction.commit();
				latch.await(1, TimeUnit.SECONDS);
				// FIXME - mvcc.jobs.2 - this is because all the population
				// threads will be ... elsewhere
				Thread.sleep(100);
				DomainStore.waitUntilCurrentRequestsProcessed();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				JobDomain.get().stateMessageEvents.remove(listener);
			}
		}
	}

	public String debugOrphanage(long jobId) {
		logger.info("Debug orphanage :: {}", jobId);
		return scheduler.debugOrphanage(jobId);
	}

	public boolean waitForZeroPendingOrInActiveJobs(long maxTime) {
		long start = System.currentTimeMillis();
		while (TimeConstants.within(start, maxTime)) {
			long incompleteOrPending = TransactionEnvironment.withDomain(() -> {
				try {
					TransactionEnvironment.get().ensureBegun();
					List<QueueStat> queueStats = JobRegistry.get()
							.getActiveQueueStats().collect(Collectors.toList());
					Long active = queueStats.stream()
							.collect(Collectors.summingLong(qs -> qs.active));
					Long pending = queueStats.stream()
							.collect(Collectors.summingLong(qs -> qs.pending));
					return active + pending;
				} finally {
					TransactionEnvironment.get().end();
				}
			});
			if (incompleteOrPending == 0) {
				return true;
			}
		}
		return false;
	}
}

package cc.alcina.framework.servlet.job;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.JobResource;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.Job.ResourceRecord;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobResult;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.JobStateMessage;
import cc.alcina.framework.common.client.job.NonRootTask;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue.QueueStat;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.servlet.ThreadedPmClientInstanceResolverImpl;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.control.WriterService;

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
 * 
 * </ul>
 * 
 * <h2>TODO (doc)</h2> Describe the logic in JobQueue.allocateJobs and
 * JobScheduler a little more fully.
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = JobRegistry.class, implementationType = ImplementationType.SINGLETON)
public class JobRegistry extends WriterService {
	public static final String CONTEXT_NO_ACTION_LOG = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_NO_ACTION_LOG";

	public static final String TRANSFORM_QUEUE_NAME = JobRegistry.class
			.getName();

	public static Builder createBuilder() {
		return new Builder();
	}

	public static JobRegistry get() {
		return Registry.impl(JobRegistry.class);
	}

	public static boolean isActiveInstance(ClientInstance instance) {
		return get().jobExecutors.getActiveServers().contains(instance);
	}

	private static void checkAnnotatedPermissions(Object o) {
		WebMethod annotation = o.getClass().getAnnotation(WebMethod.class);
		if (annotation != null) {
			if (!PermissionsManager.get().isPermissible(o,
					new AnnotatedPermissible(annotation.customPermission()))) {
				WrappedRuntimeException e = new WrappedRuntimeException(
						"Permission denied for action " + o,
						SuggestedAction.NOTIFY_WARNING);
				EntityLayerLogging.log(LogMessageType.TRANSFORM_EXCEPTION,
						"Domain transform permissions exception", e);
				throw e;
			}
		}
	}

	private Map<Job, JobContext> activeJobs = new ConcurrentHashMap<>();

	private Map<Job, List<JobResource>> jobResources = new ConcurrentHashMap<>();

	JobScheduler scheduler;

	Logger logger = LoggerFactory.getLogger(getClass());

	JobExecutors jobExecutors;

	private boolean stopped;

	private AtomicInteger extJobSystemIdCounter = new AtomicInteger();

	Map<Job, ContextAwaiter> contextAwaiters = new ConcurrentHashMap<>();

	public JobRegistry() {
		TransformCommit.get()
				.setBackendTransformQueueMaxDelay(TRANSFORM_QUEUE_NAME, 1000);
		jobExecutors = Registry.impl(JobExecutors.class);
		jobExecutors.addScheduledJobExecutorChangeConsumer(leader -> {
			if (leader) {
				scheduler.enqueueLeaderChangedEvent();
			}
		});
		scheduler = new JobScheduler(this);
		DomainDescriptorJob.get().stateMessageEvents.add((k, messages) -> {
			for (JobStateMessage message : messages) {
				if (message.getProcessState() == null
						&& activeJobs.containsKey(message.getJob())) {
					updateThreadData(message);
				}
			}
		});
	}

	// Directly acquire a resource (not via TaskPerformer.getResources)
	public void acquireResource(Job forJob, JobResource resource) {
		acquireResources(forJob, Collections.singletonList(resource));
	}

	public Job await(Job job) throws InterruptedException {
		ContextAwaiter awaiter = contextAwaiters.get(job);
		Transaction.commit();
		awaiter.await();
		JobContext jobContext = activeJobs.get(job);
		contextAwaiters.remove(job);
		jobContext.awaitSequenceCompletion();
		DomainStore.waitUntilCurrentRequestsProcessed();
		// FIXME - mvcc.jobs.1a - *really* make these lazy, mr annotation
		// you
		return Domain.find(job);
	}

	public Stream<QueueStat> getActiveQueueStats() {
		return DomainDescriptorJob.get().getAllocationQueues()
				.map(AllocationQueue::asQueueStat).sorted(Comparator
						// aka reversed date order
						.comparing(stat -> -stat.startTime.getTime()));
	}

	public String getExJobSystemNextJobId(Class<?> clazz) {
		return Ax.format("%s::%s::%s::%s", clazz.getName(),
				EntityLayerUtils.getLocalHostName(),
				EntityLayerObjects.get().getServerAsClientInstance().getId(),
				extJobSystemIdCounter.incrementAndGet());
	}

	public Stream<FutureStat> getFutureQueueStats() {
		return DomainDescriptorJob.get().getAllFutureJobs()
				.map(FutureStat::new);
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return DomainDescriptorJob.get().getJobsForTask(action)
				.sorted(EntityComparator.REVERSED_INSTANCE)
				.map(Job::asJobResult).map(JobResult::getActionLogItem)
				.limit(count).collect(Collectors.toList());
	}

	public String getPerformerThreadName(Job job) {
		return Optional.ofNullable(activeJobs.get(job)).map(jc -> jc.thread)
				.map(Thread::getName).orElse(null);
	}

	public Object getResourceOwner() {
		return JobContext.has()
				? JobContext.get().getJob().provideFirstInSequence()
				: Thread.currentThread();
	}

	public List<Job> getThreadData(Job job) {
		ThreadDataWaiter threadDataWaiter = new ThreadDataWaiter(job);
		threadDataWaiter.await();
		return threadDataWaiter.queriedJobs;
	}

	/*
	 * Awaits completion of the task and any sequential (cascaded) tasks
	 */
	public Job perform(Task task) {
		try {
			Job job = createBuilder().withTask(task).withAwaiter().create();
			return await(job);
		} catch (Exception e) {
			e.printStackTrace();
			throw WrappedRuntimeException.wrapIfNotRuntime(e);
		}
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
		stopped = true;
		scheduler.stopService();
	}

	public void wakeupScheduler() {
		scheduler.fireWakeup();
	}

	public void withJobMetadataLock(Job job, Runnable runnable) {
		withJobMetadataLock(job.toLocator().toRecoverableNumericString(),
				runnable);
	}

	public void withJobMetadataLock(String path, Runnable runnable) {
		if (runnable == null) {
			return;
		}
		try {
			jobExecutors.allocationLock(path, true);
			runnable.run();
		} finally {
			jobExecutors.allocationLock(path, false);
		}
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
			ProcessState processState = forJob.ensureProcessState();
			ResourceRecord record = processState.addResourceRecord(resource);
			if (antecedentAcquired.isPresent()) {
				record.setAcquired(true);
				record.setAcquiredFromAncestor(true);
				forJob.persistProcessState();
				Transaction.commit();
			} else {
				forJob.persistProcessState();
				Transaction.commit();
				resource.acquire();
				forJob.ensureProcessState().provideRecord(record)
						.setAcquired(true);
				forJob.persistProcessState();
				Transaction.commit();
				acquired.add(resource);
			}
		}
		if (acquired.size() > 0) {
			jobResources.put(forJob, acquired);
		}
	}

	private <T extends Task> void performJob0(Job job,
			boolean queueJobPersistence,
			LauncherThreadState launcherThreadState) {
		if (trackInternalMetrics()) {
			InternalMetrics.get().startTracker(job,
					() -> job.getTaskSerialized(), InternalMetricTypeAlcina.job,
					job.toDisplayName(), () -> true);
		}
		TaskPerformer<T> performer = getTaskPerformer(job);
		JobContext context = new JobContext(job, performer,
				launcherThreadState);
		activeJobs.put(job, context);
		if (contextAwaiters.containsKey(job)) {
			ContextAwaiter contextAwaiter = contextAwaiters.get(job);
			contextAwaiter.copyContext
					.forEach((k, v) -> LooseContext.set(k, v));
			contextAwaiter.latch.countDown();
		}
		boolean taskEnabled = !ResourceUtilities
				.isDefined(Ax.format("%s.disabled", job.getTaskClassName()))
				&& !ResourceUtilities.is("allJobsDisabled");
		try {
			LooseContext.push();
			if (performer.deferMetadataPersistence(job)) {
				LooseContext.setTrue(
						ThreadlocalTransformManager.CONTEXT_THROW_ON_RESET_TLTM);
			}
			context.start();
			if (taskEnabled) {
				acquireResources(job, performer.getResources());
				performer.performAction((T) job.getTask());
			} else {
				logger.info("Not performing {} (disabled)", job);
			}
			context.persistMetadata();
			context.toAwaitingChildren();
			context.awaitChildCompletion();
			performer.onChildCompletion();
		} catch (Throwable t) {
			Exception e = (Exception) ((t instanceof Exception) ? t
					: new WrappedRuntimeException(t));
			/*
			 * May have arrived here as a result of a failed transform commit
			 */
			Transaction.ensureEnded();
			Transaction.begin();
			context.onJobException(e);
			if (CommonUtils.extractCauseOfClass(e,
					CancelledException.class) != null) {
			} else {
				logger.warn(Ax.format("Job exception in job %s", job), t);
				EntityLayerLogging.persistentLog(LogMessageType.TASK_EXCEPTION,
						e);
			}
		} finally {
			/*
			 * key - handle tx timeouts/aborts
			 */
			Transaction.ensureBegun();
			try {
				releaseResources(job, false);
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("DEVEX::7 - JobRegistry.releaseResources", e);
			}
			LooseContext.remove(
					ThreadlocalTransformManager.CONTEXT_THROW_ON_RESET_TLTM);
			performer.onBeforeEnd();
			context.end();
			performer.onAfterEnd();
			context.persistMetadata();
			LooseContext.pop();
			activeJobs.remove(job);
			context.remove();
			if (trackInternalMetrics()) {
				InternalMetrics.get().endTracker(job);
			}
		}
	}

	private void releaseResources(Job job, boolean inSequenceRelease) {
		List<JobResource> resources = jobResources.get(job);
		if (resources == null) {
			return;
		}
		Iterator<JobResource> itr = resources.iterator();
		while (itr.hasNext()) {
			JobResource resource = itr.next();
			boolean release = true;
			if (job.provideHasIncompleteSubsequent()
					&& resource.isSharedWithSubsequents()) {
				// FIXME - mvcc.1 - tmp remove
				// release = false;
			}
			if (release) {
				resource.release();
				itr.remove();
			}
		}
		if (resources.isEmpty()) {
			jobResources.remove(job);
		}
		if (!inSequenceRelease) {
			List<Job> relatedSequential = job.provideRelatedSequential();
			for (Job related : relatedSequential) {
				releaseResources(job, true);
			}
		}
	}

	private void updateThreadData(JobStateMessage message) {
		JobContext context = activeJobs.get(message.getJob());
		if (context != null) {
			ProcessState processState = message.ensureProcessState();
			context.updateProcessState(processState);
			message.persistProcessState();
		}
	}

	protected TaskPerformer getTaskPerformer(Job job) {
		Task task = job.getTask();
		if (task instanceof TaskPerformer) {
			return (TaskPerformer) task;
		}
		Optional<TaskPerformer> performer = Registry
				.optional(TaskPerformer.class, task.getClass());
		if (performer.isPresent()) {
			return performer.get();
		} else {
			return new MissingPerformerPerformer();
		}
	}

	protected boolean trackInternalMetrics() {
		return ResourceUtilities.is("trackInternalMetrics");
	}

	<JR extends JobResource> Optional<JR> getAcquiredResource(Job forJob,
			JR resource) {
		return forJob.provideSelfAndAntecedents()
				.map(j -> getResource(j, resource, forJob))
				.filter(Objects::nonNull).findFirst();
	}

	JobContext getContext(Job job) {
		return activeJobs.get(job);
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

	/*
	 * Jobs are always run in new (or job-only) threads
	 * 
	 * FIXME - mvcc.jobs.1a - launcherthreadstate should go away
	 */
	void performJob(Job job, boolean queueJobPersistence,
			LauncherThreadState launcherThreadState) {
		try {
			if (Transaction.isInTransaction()) {
				logger.warn(Ax.format(
						"DEVEX::4 - JobRegistry.performJob - begin with open transaction "
								+ " - {}\nuncommitted transforms:\n{}",
						job), TransformManager.get().getTransforms());
				try {
					Transaction.commit();
				} catch (Exception e) {
					logger.warn("DEVEX::4 - JobRegistry.performJob", e);
				}
				Transaction.ensureEnded();
			}
			LooseContext.push();
			LooseContext.set(
					ThreadedPmClientInstanceResolverImpl.CONTEXT_CLIENT_INSTANCE,
					EntityLayerObjects.get().getServerAsClientInstance());
			Thread.currentThread().setContextClassLoader(
					launcherThreadState.contextClassLoader);
			launcherThreadState.copyContext
					.forEach((k, v) -> LooseContext.set(k, v));
			Transaction.begin();
			Task task = job.getTask();
			if (task instanceof NonRootTask) {
				ThreadedPermissionsManager.cast().pushUser(
						((NonRootTask) task).provideIUser(),
						LoginState.LOGGED_IN);
			} else {
				ThreadedPermissionsManager.cast().pushSystemUser();
			}
			performJob0(job, queueJobPersistence, launcherThreadState);
			logger.info("Job complete - {}", job);
		} catch (RuntimeException e) {
			if (stopped) {
				// app shutdown - various services will not be available -
				// ignore
			} else {
				// will generally be close to the top of a thread - so log, even
				// if there's logging higher
				e.printStackTrace();
				logger.warn(Ax.format("DEVEX::3 - JobRegistry.performJob - %s",
						job), e);
			}
		} finally {
			PermissionsManager.get().popUser();
			Transaction.end();
			LooseContext.pop();
			LooseContext.confirmDepth(0);
		}
	}

	@RegistryLocation(registryPoint = ActionPerformerTrackMetrics.class, implementationType = ImplementationType.SINGLETON)
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

		public void addSibling(Task task) {
			this.task = task;
			withContextParent();
			create();
		}

		public Job create() {
			checkAnnotatedPermissions(task);
			Job job = AlcinaPersistentEntityImpl.create(Job.class);
			job.setUser(PermissionsManager.get().getUser());
			job.setState(initialState);
			job.setTask(task);
			job.setTaskClassName(task.getClass().getName());
			job.setCreator(
					EntityLayerObjects.get().getServerAsClientInstance());
			if (runAt != null) {
				Preconditions.checkArgument(initialState == JobState.FUTURE);
			}
			job.setRunAt(SEUtilities.toOldDate(runAt));
			if (related != null) {
				related.createRelation(job, relationType);
			}
			/*
			 * To elaborate - but essentially a top-level job *must* have its
			 * performer defined early if it's not a FUTURE
			 */
			if (initialState == JobState.PENDING && related == null) {
				job.setPerformer(
						EntityLayerObjects.get().getServerAsClientInstance());
			}
			if (awaiter) {
				JobRegistry.get().contextAwaiters.put(job,
						new ContextAwaiter());
			}
			task.onJobCreate(job);
			lastCreated = job;
			return job;
		}

		public Builder createReturnBuilder() {
			create();
			return this;
		}

		public void followWith(Task task) {
			this.task = task;
			relationType = JobRelationType.SEQUENCE;
			Preconditions.checkArgument(lastCreated != null);
			related = lastCreated;
			create();
		}

		public Builder withAwaiter() {
			awaiter = true;
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
	}

	public static class FutureStat {
		public String taskName;

		public Date runAt;

		public String jobId;

		FutureStat(Job job) {
			taskName = job.getTaskClassName();
			runAt = job.getRunAt();
			jobId = String.valueOf(job.getId());
		}
	}

	@RegistryLocation(registryPoint = JobExecutors.class, implementationType = ImplementationType.INSTANCE)
	public static class JobExecutorsSingle implements JobExecutors {
		@Override
		public void addScheduledJobExecutorChangeConsumer(
				Consumer<Boolean> consumer) {
		}

		@Override
		public void allocationLock(String path, boolean acquire) {
			// noop
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
			return false;
		}
	}

	@RegistryLocation(registryPoint = Task.Performer.class, implementationType = ImplementationType.SINGLETON)
	public static class Performer implements Task.Performer {
		@Override
		public Job perform(Task task) {
			return get().perform(task);
		}

		@Override
		public Job schedule(Task task) {
			return createBuilder().withTask(task).create();
		}
	}

	static class ContextAwaiter {
		CountDownLatch latch = new CountDownLatch(1);

		Map<String, Object> copyContext = new LinkedHashMap<>();

		public ContextAwaiter() {
			// we don't copy permissions manager/user - since often the launcher
			// will be triggered by a system user context (because triggered by
			// a transaction event)
			//
			// instead, all tasks for which runAsRoot returns false must
			// implement iuser
			copyContext.putAll(LooseContext.getContext().properties);
		}

		public void await() {
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
			copyContext.putAll(LooseContext.getContext().properties);
			contextClassLoader = currentThread.getContextClassLoader();
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}
	}

	static class MissingPerformerPerformer implements TaskPerformer {
		@Override
		public void performAction(Task task) throws Exception {
			throw new Exception(Ax.format("No performer found for task %s",
					task.getClass().getName()));
		}
	}

	class SequenceCompletionLatch {
		private CountDownLatch latch = new CountDownLatch(1);

		JobContext context;

		void await() {
			try {
				// FIXME - mvcc.jobs - remove timeout
				while (true) {
					try {
						Transaction.ensureEnded();
						if (latch.await(10, TimeUnit.SECONDS)) {
							break;
						}
						int debug = 3;
					} finally {
						Transaction.begin();
						if (context != null) {
							context.checkCancelled0(true);
						}
					}
				}
				context.awaitSequenceCompletion();
			} catch (InterruptedException e) {
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			}
		}

		void onChildJobsCompleted() {
			latch.countDown();
		}
	}

	class ThreadDataWaiter {
		List<Job> queriedJobs = new ArrayList<>();

		private Job job;

		private CountDownLatch latch;

		private TopicListener<List<JobStateMessage>> listener = (k,
				messages) -> {
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
						JobStateMessage stateMessage = AlcinaPersistentEntityImpl
								.create(JobStateMessage.class);
						stateMessage.setJob(job);
					});
			try {
				DomainDescriptorJob.get().stateMessageEvents.add(listener);
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
				DomainDescriptorJob.get().stateMessageEvents.remove(listener);
			}
		}
	}
}

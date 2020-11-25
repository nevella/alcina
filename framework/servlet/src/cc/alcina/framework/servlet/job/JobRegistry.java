package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.JobResource;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobResult;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.NonRootTask;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.job.Task.HasClusteredRunParameter;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
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
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.servlet.ThreadedPmClientInstanceResolverImpl;
import cc.alcina.framework.servlet.job.JobContext.PersistenceType;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;
import cc.alcina.framework.servlet.job.JobScheduler.ScheduleEvent;
import cc.alcina.framework.servlet.job.JobScheduler.ScheduleProvider;
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

	static final String TRANSFORM_QUEUE_NAME = JobRegistry.class.getName();

	public static JobRegistry get() {
		return Registry.impl(JobRegistry.class);
	}

	private Map<Job, JobContext> activeJobs = new ConcurrentHashMap<>();

	private Map<Job, List<JobResource>> jobResources = new ConcurrentHashMap<>();

	private Map<String, JobQueue> activeQueues = new ConcurrentHashMap<>();

	Map<Job, SequenceCompletionLatch> completionLatches = new ConcurrentHashMap<>();

	private JobScheduler scheduler;

	Logger logger = LoggerFactory.getLogger(getClass());

	private TopicListener<Entry<String, List<Job>>> queueNotifier = (k, e) -> {
		String name = e.getKey();
		logger.debug("Metadata changed on queue {}", name);
		JobQueue queue = activeQueues.get(name);
		/*
		 * When scheduling subjobs on a different queue, this...? (lost comment)
		 */
		Job firstJob = e.getValue().get(0);
		if (!firstJob.provideCanDeserializeTask()) {
			return;
		}
		if (queue == null) {
			// FIXME - mvcc.jobs.1a - there's some doubling up here with
			Schedule schedule = scheduler.getScheduleForQueueEventAndOrJob(name,
					firstJob);
			if (schedule != null) {
				queue = ensureQueue(schedule);
			}
		}
		if (queue != null) {
			queue.onMetadataChanged();
		}
		scheduler.onScheduleEvent(new ScheduleEvent(name, firstJob));
	};

	JobExecutors jobExecutors;

	private boolean stopped;

	private AtomicInteger extJobSystemIdCounter = new AtomicInteger();

	public JobRegistry() {
		TransformCommit.get()
				.setBackendTransformQueueMaxDelay(TRANSFORM_QUEUE_NAME, 1000);
		DomainDescriptorJob.get().queueChanged.add(queueNotifier);
		scheduler = new JobScheduler(this);
		jobExecutors = Registry.impl(JobExecutors.class);
		jobExecutors.addScheduledJobExecutorChangeConsumer(leader -> {
			if (leader) {
				scheduler.enqueueLeaderChangedEvent();
			}
		});
		JobExecutors.topicRescheduleJobs.add((k, v) -> {
			if (v) {
				scheduleJobs(false);
			}
		});
	}

	// Directly acquire a resource (not via TaskPerformer.getResources)
	public void acquireResource(Job forJob, JobResource resource) {
		acquireResources(forJob, Collections.singletonList(resource));
	}

	public Job createJob(Task task, Schedule schedule, Date runAt,
			boolean ignoreCreationContext) {
		checkAnnotatedPermissions(task);
		Job job = AlcinaPersistentEntityImpl.create(Job.class);
		job.setUser(PermissionsManager.get().getUser());
		job.setState(JobState.PENDING);
		job.setTask(task);
		job.setTaskClassName(task.getClass().getName());
		job.setCreator(EntityLayerObjects.get().getServerAsClientInstance());
		if (schedule == null) {
			if (task instanceof HasClusteredRunParameter) {
				job.setClustered(((HasClusteredRunParameter) task)
						.provideIsRunClustered());
			}
		} else {
			job.setQueue(schedule.getQueueName());
			job.setClustered(schedule.isClustered());
		}
		JobContext creationContext = JobContext.get();
		if (creationContext != null && runAt == null
				&& !ignoreCreationContext) {
			Job creatingJob = creationContext.getJob();
			/*
			 * If scheduling from&to a job in a max-1 queue, sequential
			 * execution is required
			 */
			boolean scheduleSubsequent = schedule != null
					&& Objects.equals(schedule.getQueueName(),
							creatingJob.getQueue())
					&& (schedule.getQueueMaxConcurrentJobs() == 1
							|| schedule.isRunSameQueueScheduledAsSubsequent());
			if (scheduleSubsequent) {
				creatingJob.followWith(job);
			} else {
				creatingJob.createRelation(job, JobRelationType.parent_child);
			}
		}
		job.setRunAt(runAt);
		task.onJobCreate(job);
		return job;
	}

	public List<QueueStat> getActiveQueueStats() {
		return activeQueues.values().stream().map(JobQueue::asQueueStat)
				.collect(Collectors.toList());
	}

	public String getExJobSystemNextJobId(Class<?> clazz) {
		return Ax.format("%s::%s::%s::%s", clazz.getName(),
				EntityLayerUtils.getLocalHostName(),
				EntityLayerObjects.get().getServerAsClientInstance().getId(),
				extJobSystemIdCounter.incrementAndGet());
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return DomainDescriptorJob.get().getJobsForTask(action)
				.sorted(EntityComparator.REVERSED_INSTANCE)
				.map(Job::asJobResult).map(JobResult::getActionLogItem)
				.limit(count).collect(Collectors.toList());
	}

	public List<PendingStat> getPendingQueueStats() {
		return scheduler.getScheduledJobs().stream().map(PendingStat::new)
				.collect(Collectors.toList());
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

	public Schedule getScheduleForQueue(String queueName) {
		return activeQueues.get(queueName).getSchedule();
	}

	public void onJobQueueTerminated(JobQueue jobQueue) {
		synchronized (activeQueues) {
			activeQueues.remove(jobQueue.getName());
		}
		logger.info("Job queue terminated: {}", jobQueue.getName());
	}

	/*
	 * Awaits completion of the task and any sequential (cascaded) tasks
	 */
	public JobResult perform(Task task) {
		try {
			SequenceCompletionLatch completionLatch = new SequenceCompletionLatch();
			Job job = start(task, completionLatch);
			completionLatch.await();
			DomainStore.waitUntilCurrentRequestsProcessed();
			return job.asJobResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw WrappedRuntimeException.wrapIfNotRuntime(e);
		}
	}

	public Job schedule(Task task) {
		return schedule(task, scheduler.getSchedule(task.getClass(), false));
	}

	/*
	 * Does *not* use schedule.getNext() - it's called by various utility
	 * methods so if you want a custom time, add it in code (JobScheduler will
	 * add the future date if sequenced if it)
	 */
	public Job schedule(Task task, Schedule schedule) {
		Schedule providerSchedule = scheduler.getSchedule(task);
		// to ensure we respect non-timebased task execution constraints, use
		// the provider
		// schedule if it exists && different to incoming
		// schedule queue
		if (providerSchedule != null) {
			if (schedule == null
					|| !Objects.equals(providerSchedule.getQueueName(),
							schedule.getQueueName())) {
				schedule = providerSchedule;
			}
		}
		try {
			if (schedule == null) {
				return start(task, null);
			} else {
				Job job = createJob(task, schedule, null, false);
				job.setClustered(schedule.isClustered());
				ensureQueue(schedule);
				return job;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw WrappedRuntimeException.wrapIfNotRuntime(e);
		}
	}

	public void scheduleJobs(boolean applicationStartup) {
		scheduler.enqueueInitialScheduleEvent();
	}

	/*
	 * start job immediately. Uses non-time-based constraints from schedule
	 * (e.g. queue) if the task has one, but not time (i.e. runAt)
	 */
	public Job start(Task task, SequenceCompletionLatch latch) {
		/*
		 * (doc) - ensure that, if a schedule is defined for a task (which also
		 * indicates that the task has no parameters), a one-off execution of
		 * the task runs on that queue
		 */
		Optional<ScheduleProvider> scheduleProvider = Registry
				.optional(ScheduleProvider.class, task.getClass());
		Schedule schedule = scheduleProvider.map(sp -> sp.getSchedule(task))
				.orElse(null);
		Job job = createJob(task, schedule, null, false);
		if (latch != null) {
			completionLatches.put(job, latch);
		}
		JobQueue queue = schedule != null ? ensureQueue(schedule)
				: createPerJobQueue(job);
		job.setQueue(queue.getName());
		Transaction.commit();
		return job;
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
		stopped = true;
		activeQueues.values().forEach(queue -> {
			try {
				queue.cancel();
			} catch (Exception e) {
				//
			}
		});
		scheduler.stopService();
	}

	public void waitForQueue(String queueName) {
		JobQueue jobQueue = activeQueues.get(queueName);
		if (jobQueue != null) {
			jobQueue.awaitEmpty();
		}
	}

	public void withJobMetadataLock(String queueName, boolean clustered,
			Runnable runnable) {
		if (runnable == null) {
			return;
		}
		try {
			jobExecutors.allocationLock(queueName, clustered, true);
			runnable.run();
		} finally {
			jobExecutors.allocationLock(queueName, clustered, false);
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
			if (antecedentAcquired.isPresent()) {
			} else {
				resource.acquire();
				acquired.add(resource);
			}
		}
		if (acquired.size() > 0) {
			jobResources.put(forJob, acquired);
		}
	}

	private JobQueue createPerJobQueue(Job job) {
		JobQueue queue = new JobQueue(job, Executors.newSingleThreadExecutor(
				new NamedThreadFactory("ad-hoc-jobs-pool")), 1, false);
		queue.shutdownExecutorOnExit = true;
		activeQueues.put(queue.getName(), queue);
		return queue;
	}

	private <T extends Task> void performJob0(Job job,
			boolean queueJobPersistence,
			LauncherThreadState launcherThreadState) {
		TaskPerformer<T> performer = getTaskPerformer(job);
		PersistenceType persistenceType = PersistenceType.Immediate;
		if (queueJobPersistence) {
			persistenceType = PersistenceType.Queued;
		}
		if ((Ax.isTest() && !ResourceUtilities.is("persistConsoleJobs"))) {
			persistenceType = PersistenceType.None;
		}
		JobContext context = new JobContext(job, persistenceType, performer);
		SequenceCompletionLatch completionLatch = completionLatches.get(job);
		if (completionLatch != null) {
			completionLatch.context = context;
		}
		activeJobs.put(job, context);
		boolean taskEnabled = !ResourceUtilities
				.isDefined(Ax.format("%s.disabled", job.getTaskClassName()))
				&& !ResourceUtilities.is("allJobsDisabled");
		try {
			LooseContext.push();
			context.start();
			if (taskEnabled) {
				acquireResources(job, performer.getResources());
				performer.performAction((T) job.getTask());
			} else {
				logger.info("Not performing {} (disabled)", job);
			}
			context.awaitChildCompletion();
		} catch (Throwable t) {
			Exception e = (Exception) ((t instanceof Exception) ? t
					: new WrappedRuntimeException(t));
			context.onJobException(e);
			if (CommonUtils.extractCauseOfClass(e,
					CancelledException.class) != null) {
			} else {
				EntityLayerLogging.persistentLog(LogMessageType.TASK_EXCEPTION,
						e);
			}
			throw WrappedRuntimeException.wrapIfNotRuntime(e);
		} finally {
			releaseResources(job, false);
			context.end();
			LooseContext.pop();
			activeJobs.remove(job);
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

	protected void checkAnnotatedPermissions(Object o) {
		WebMethod annotation = o.getClass().getAnnotation(WebMethod.class);
		if (annotation != null) {
			if (!PermissionsManager.get().isPermissible(
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

	JobQueue ensureQueue(Schedule schedule) {
		synchronized (activeQueues) {
			if (activeQueues.containsKey(schedule.getQueueName())) {
				return activeQueues.get(schedule.getQueueName());
			}
			JobQueue queue = new JobQueue(schedule);
			activeQueues.put(queue.getName(), queue);
			return queue;
		}
	}

	<JR extends JobResource> Optional<JR> getAcquiredResource(Job forJob,
			JR resource) {
		return Stream.concat(forJob.provideAntecedents(), Stream.of(forJob))
				.map(j -> getResource(j, resource, forJob))
				.filter(Objects::nonNull).findFirst();
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
	 */
	void performJob(Job job, boolean queueJobPersistence,
			LauncherThreadState launcherThreadState) {
		try {
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
		} catch (RuntimeException e) {
			if (stopped) {
				// app shutdown - various services will not be available -
				// ignore
			} else {
				// will generally be close to the top of a thread - so log, even
				// if there's logging higher
				logger.warn("DEVEX::3 - JobRegistry.performJob", e);
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

	@RegistryLocation(registryPoint = JobExecutors.class, implementationType = ImplementationType.INSTANCE)
	public static class JobExecutorsSingle implements JobExecutors {
		@Override
		public void addScheduledJobExecutorChangeConsumer(
				Consumer<Boolean> consumer) {
		}

		@Override
		public void allocationLock(String name, boolean clustered,
				boolean acquire) {
			// Preconditions.checkState(!clustered);
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
	}

	public static class PendingStat {
		public String taskName;

		public Date runAt;

		public String name;

		public String jobId;

		PendingStat(Job job) {
			taskName = job.getTaskClassName();
			runAt = job.getRunAt();
			name = job.getQueue();
			jobId = String.valueOf(job.getId());
		}
	}

	@RegistryLocation(registryPoint = Task.Performer.class, implementationType = ImplementationType.SINGLETON)
	public static class Performer implements Task.Performer {
		@Override
		public JobResult perform(Task task) {
			return get().perform(task);
		}

		@Override
		public Job schedule(Task task) {
			return get().schedule(task);
		}
	}

	public static class QueueStat {
		public int active;

		public int pending;

		public int total;

		public String name;

		public int completed;
	}

	static class LauncherThreadState {
		ClassLoader contextClassLoader;

		Map<String, Object> copyContext = new LinkedHashMap<>();

		public LauncherThreadState() {
			// we don't copy permissions manager/user - since often the launcher
			// will be triggered by a system user context (because triggered by
			// a transaction event)
			//
			// instead, all tasks for which runAsRoot returns false must
			// implement iuser
			copyContext.putAll(LooseContext.getContext().properties);
			contextClassLoader = Thread.currentThread().getContextClassLoader();
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
}

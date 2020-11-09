package cc.alcina.framework.servlet.job2;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobResult;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
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
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.job.JobRegistry1;
import cc.alcina.framework.servlet.job2.JobContext.PersistenceType;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;
import cc.alcina.framework.servlet.job2.JobScheduler.ScheduleProvider;
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

	private Queue<JobContext> activeJobs = new ConcurrentLinkedQueue<>();

	private Map<String, JobQueue> activeQueues = new ConcurrentHashMap<>();

	private JobScheduler scheduler;

	Logger logger = LoggerFactory.getLogger(getClass());

	private TopicListener<String> queueNotifier = (k, name) -> {
		logger.debug("Metadata changed on queue {}", name);
		JobQueue queue = activeQueues.get(name);
		if (queue != null) {
			queue.onMetadataChanged();
		}
		scheduler.onQueueChanged(name);
	};

	JobExecutors jobExecutors;

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
	}

	public List<QueueStat> getActiveQueueStats() {
		return activeQueues.values().stream().map(JobQueue::asQueueStat)
				.collect(Collectors.toList());
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

	public void onJobQueueTerminated(JobQueue jobQueue) {
		synchronized (activeQueues) {
			activeQueues.remove(jobQueue.getName());
		}
		logger.info("Job queue terminated: {}", jobQueue.getName());
	}

	public JobResult perform(Task task) {
		Job job = start(task);
		JobQueue queue = activeQueues.get(job.getQueue());
		queue.awaitEmpty();
		DomainStore.waitUntilCurrentRequestsProcessed();
		return queue.initialJob.asJobResult();
	}

	public void schedule(Task task) {
		schedule(task, scheduler.getSchedule(task.getClass(), false));
	}

	public Job schedule(Task task, Schedule schedule) {
		if (schedule == null) {
			return start(task);
		} else {
			Job job = createJob(task);
			job.setRunAt(SEUtilities.toOldDate(schedule.getNext()));
			job.setClustered(schedule.isClustered());
			JobQueue queue = ensureQueue(schedule);
			job.setQueue(queue.getName());
			return job;
		}
	}

	public void scheduleJobs(boolean applicationStartup) {
		scheduler.enqueueInitialScheduleEvent();
	}

	public Job start(Task task) {
		Job job = createJob(task);
		JobContext creationContext = JobContext.get();
		if (creationContext != null) {
			creationContext.getJob().createRelation(job,
					JobRelationType.parent_child);
		}
		/*
		 * (doc) - ensure that, if a schedule is defined for a task (which also
		 * indicates that the task has no parameters), a one-off execution of
		 * the task runs on that queue
		 */
		Optional<ScheduleProvider> scheduleProvider = Registry
				.optional(ScheduleProvider.class, task.getClass());
		Optional<Schedule> schedule = scheduleProvider
				.map(sp -> sp.getSchedule(task.getClass(), false));
		JobQueue queue = schedule.isPresent() ? ensureQueue(schedule.get())
				: createPerJobQueue(job);
		Transaction.commit();
		queue.ensureAllocating();
		return job;
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
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

	private JobQueue createPerJobQueue(Job job) {
		JobQueue queue = new JobQueue(job, new CurrentThreadExecutorService(),
				1, false);
		activeQueues.put(queue.getName(), queue);
		return queue;
	}

	private void performJob0(Job job) {
		TaskPerformer performer = getTaskPerformer(job);
		PersistenceType persistenceType = PersistenceType.Queued;
		if (!UserlandProvider.get().isSystemUser(job.getUser())
				|| !job.provideParent().isPresent()) {
			persistenceType = PersistenceType.Immediate;
		}
		if (LooseContext.is(JobRegistry1.CONTEXT_NON_PERSISTENT) || (Ax.isTest()
				&& !ResourceUtilities.is("persistConsoleJobs"))) {
			persistenceType = PersistenceType.None;
		}
		JobContext context = new JobContext(job, persistenceType, performer);
		activeJobs.add(context);
		boolean taskEnabled = !ResourceUtilities
				.isDefined(Ax.format("%s.disabled", job.getTaskClassName()))
				&& !ResourceUtilities.is("allJobsDisabled");
		if (performer.getClass().getName().endsWith("VrDailies")
				|| performer.getClass().getName().endsWith("TaskLogJobs")
				|| performer instanceof MissingPerformerPerformer) {
			taskEnabled = true;
		}
		try {
			LooseContext.push();
			context.start();
			if (taskEnabled) {
				performer.performAction(job.getTask());
			} else {
				logger.info("Not performing {} (disabled)", job);
			}
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
			context.end();
			LooseContext.pop();
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

	Job createJob(Task task) {
		checkAnnotatedPermissions(task);
		Job job = AlcinaPersistentEntityImpl.create(Job.class);
		job.setUser(PermissionsManager.get().getUser());
		job.setState(JobState.PENDING);
		job.setTask(task);
		job.setTaskClassName(task.getClass().getName());
		job.setCreator(EntityLayerObjects.get().getServerAsClientInstance());
		return job;
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

	void performJob(Job job) {
		MethodContext.instance().withWrappingTransaction()
				.run(() -> performJob0(job));
	}

	@RegistryLocation(registryPoint = ActionPerformerTrackMetrics.class, implementationType = ImplementationType.SINGLETON)
	public static class ActionPerformerTrackMetrics
			implements Supplier<Boolean> {
		@Override
		public Boolean get() {
			return true;
		}
	}

	public interface JobExecutors {
		void addScheduledJobExecutorChangeConsumer(
				Consumer<Boolean> changeConsumer);

		void allocationLock(String name, boolean clustered, boolean acquire);

		List<ClientInstance> getActiveServers();

		boolean isCurrentScheduledJobExecutor();
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
		public void schedule(Task task) {
			get().schedule(task);
		}
	}

	public static class QueueStat {
		public int active;

		public int pending;

		public int total;

		public String name;
	}

	static class MissingPerformerPerformer implements TaskPerformer {
		@Override
		public void performAction(Task task) throws Exception {
			throw new Exception(Ax.format("No performer found for task %s",
					task.getClass().getName()));
		}
	}
}

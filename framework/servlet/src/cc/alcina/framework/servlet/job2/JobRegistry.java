package cc.alcina.framework.servlet.job2;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobResult;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
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
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.job.JobRegistry1;
import cc.alcina.framework.servlet.job2.JobContext.PersistenceType;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.control.WriterService;

@RegistryLocation(registryPoint = JobRegistry.class, implementationType = ImplementationType.SINGLETON)
/**
 * <h2>TODO</h2>
 * <ul>
 * 
 * </ul>
 * 
 * @author nick@alcina.cc
 *
 */
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
	}

	public List<QueueStat> getActiveQueueStats() {
		return activeQueues.values().stream().map(JobQueue::asQueueStat)
				.collect(Collectors.toList());
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return DomainDescriptorJob.get().getJobsForTask(action)
				.map(Job::asJobResult).map(JobResult::getActionLogItem)
				.collect(Collectors.toList());
	}

	public List<PendingStat> getPendingQueueStats() {
		return scheduler.getScheduledJobs().stream().map(PendingStat::new)
				.collect(Collectors.toList());
	}

	public void onJobQueueTerminated(JobQueue jobQueue) {
		// FIXME - mvcc.jobs - I can see a race with ensureQueue here...
		activeQueues.remove(jobQueue.getName());
		logger.info("Job queue terminated: {}", jobQueue.getName());
	}

	public JobResult perform(Task task) {
		JobQueue queue = start(task);
		queue.awaitEmpty();
		DomainStore.waitUntilCurrentRequestsProcessed();
		return queue.initialJob.asJobResult();
	}

	public void schedule(Task task) {
		schedule(task, scheduler.getSchedule(task.getClass(), false));
	}

	public JobQueue schedule(Task task, Schedule schedule) {
		if (schedule == null || schedule.getQueueName() == null) {
			return start(task);
		} else {
			Job job = createJob(task);
			JobQueue queue = ensureQueue(schedule);
			job.setQueue(queue.getName());
			return queue;
		}
	}

	public void scheduleJobs(boolean applicationStartup) {
		scheduler.enqueueInitialScheduleEvent();
	}

	public JobQueue start(Task task) {
		Job job = createJob(task);
		JobQueue queue = createPerJobQueue(job);
		Transaction.commit();
		queue.allocating = true;
		AlcinaChildRunnable.runInTransactionNewThread(queue.getName(),
				queue::loopAllocations);
		return queue;
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
		activeQueues.values().forEach(JobQueue::cancel);
		scheduler.stopService();
	}

	public void waitForQueue(String queueName) {
		JobQueue jobQueue = activeQueues.get(queueName);
		if (jobQueue != null) {
			jobQueue.awaitEmpty();
		}
	}

	private void createJobRelation(Job from, Job to, JobRelationType type) {
		JobRelation relation = AlcinaPersistentEntityImpl
				.create(JobRelation.class);
		relation.setFrom(from);
		relation.setTo(to);
		relation.setType(type);
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
		job.setKey(task.provideJobKey());
		job.setState(JobState.PENDING);
		job.setTask(task);
		job.setTaskClassName(task.getClass().getName());
		JobContext creationContext = JobContext.get();
		if (creationContext != null) {
			createJobRelation(creationContext.getJob(), job,
					JobRelationType.parent_child);
		}
		return job;
	}

	synchronized JobQueue ensureQueue(Schedule schedule) {
		if (activeQueues.containsKey(schedule.getQueueName())) {
			return activeQueues.get(schedule.getQueueName());
		}
		JobQueue queue = new JobQueue(schedule);
		activeQueues.put(queue.getName(), queue);
		return queue;
	}

	void performJob(Job job) {
		MethodContext.instance().withWrappingTransaction()
				.run(() -> performJob0(job));
	}

	void withJobMetadataLock(String queueName, boolean clustered,
			Runnable runnable) {
		try {
			jobExecutors.allocationLock(queueName, clustered, true);
			runnable.run();
		} finally {
			jobExecutors.allocationLock(queueName, clustered, false);
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
	public static class JobExceutorsSingle implements JobExecutors {
		@Override
		public void allocationLock(String name, boolean clustered,
				boolean acquire) {
			// Preconditions.checkState(!clustered);
		}

		@Override
		public boolean isCurrentScheduledJobExecutor() {
			return true;
		}
	}

	public interface JobExecutors {
		void allocationLock(String name, boolean clustered, boolean acquire);

		boolean isCurrentScheduledJobExecutor();
	}

	public static class PendingStat {
		public String taskName;

		public Date runAt;

		public String name;

		PendingStat(Job job) {
			taskName = job.getTaskClassName();
			runAt = job.getRunAt();
			name = job.getQueue();
		}
	}

	@RegistryLocation(registryPoint = Task.Performer.class, implementationType = ImplementationType.SINGLETON)
	public static class Performer implements Task.Performer {
		@Override
		public void perform(Task task) {
			get().perform(task);
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

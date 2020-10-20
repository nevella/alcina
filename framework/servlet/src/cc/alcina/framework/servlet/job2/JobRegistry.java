package cc.alcina.framework.servlet.job2;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.job.Job;
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
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.servlet.job.JobRegistry1;
import cc.alcina.framework.servlet.job2.JobContext.PersistenceType;
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

	private TopicListener<String> queueNotifier = (k, name) -> {
		JobQueue queue = activeQueues.get(name);
		if (queue != null) {
			queue.onMetadataChanged();
		}
	};

	public JobRegistry() {
		TransformCommit.get()
				.setBackendTransformQueueMaxDelay(TRANSFORM_QUEUE_NAME, 1000);
		DomainDescriptorJob.get().queueChanged.add(queueNotifier);
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return DomainDescriptorJob.get().getJobsForTask(action)
				.map(Job::asJobResult).map(JobResult::getActionLogItem)
				.collect(Collectors.toList());
	}

	public void onJobQueueTerminated(JobQueue jobQueue) {
		activeQueues.remove(jobQueue.getName());
	}

	public JobResult perform(Task task) {
		JobQueue queue = start(task);
		queue.awaitEmpty();
		return queue.initialJob.asJobResult();
	}

	public JobQueue start(Task task) {
		Job job = createJob(task);
		JobQueue queue = createPerJobQueue(job);
		Transaction.commit();
		AlcinaChildRunnable.runInTransactionNewThread(queue.getName(),
				queue::awaitEmpty);
		return queue;
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
		activeQueues.values().forEach(JobQueue::cancel);
	}

	private Job createJob(Task task) {
		checkAnnotatedPermissions(task);
		Job job = AlcinaPersistentEntityImpl.create(Job.class);
		job.setUser(PermissionsManager.get().getUser());
		job.setKey(task.provideJobKey());
		job.setState(JobState.PENDING);
		job.setTask(task);
		job.setTaskClassName(task.getClass().getName());
		return job;
	}

	private JobQueue createPerJobQueue(Job job) {
		JobQueue queue = new JobQueue(job, new CurrentThreadExecutorService(),
				1, false);
		activeQueues.put(queue.getName(), queue);
		return queue;
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
		return Registry.impl(TaskPerformer.class, task.getClass());
	}

	void performJob(Job job) {
		TaskPerformer performer = getTaskPerformer(job);
		PersistenceType persistenceType = PersistenceType.Queued;
		if (!UserlandProvider.get().isSystemUser(job.getUser())) {
			persistenceType = PersistenceType.Immediate;
		}
		if (LooseContext.is(JobRegistry1.CONTEXT_NON_PERSISTENT) || (Ax.isTest()
				&& !ResourceUtilities.is("persistConsoleJobs"))) {
			persistenceType = PersistenceType.None;
		}
		JobContext context = new JobContext(job, persistenceType, performer);
		activeJobs.add(context);
		try {
			LooseContext.push();
			context.start();
			performer.performAction(job.getTask());
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

	@RegistryLocation(registryPoint = ActionPerformerTrackMetrics.class, implementationType = ImplementationType.SINGLETON)
	public static class ActionPerformerTrackMetrics
			implements Supplier<Boolean> {
		@Override
		public Boolean get() {
			return true;
		}
	}

	@RegistryLocation(registryPoint = Task.Performer.class, implementationType = ImplementationType.SINGLETON)
	public static class Performer implements Task.Performer {
		@Override
		public void perform(Task task) {
			get().perform(task);
		}
	}

	class ActionLauncherAsync extends AlcinaChildRunnable {
		private CountDownLatch latch;

		private RemoteAction action;

		private TopicListener startListener;

		volatile JobTracker tracker;

		ActionLauncherAsync(String name, RemoteAction action) {
			super(name);
			this.latch = new CountDownLatch(2);
			this.action = action;
			this.startListener = new TopicListener<JobTracker>() {
				@Override
				public void topicPublished(String key, JobTracker tracker) {
					ActionLauncherAsync.this.tracker = tracker;
					latch.countDown();
				}
			};
		}

		public JobTracker launchAndWaitForTracker() {
			Thread thread = new Thread(this);
			thread.start();
			latch.countDown();
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return tracker;
		}

		@Override
		protected void run0() throws Exception {
			LooseContext.getContext().addTopicListener(
					JobRegistry1.TOPIC_JOB_STARTED, startListener);
			perform(this.action);
		}
	}
}

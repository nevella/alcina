package cc.alcina.framework.servlet.job2;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
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
 * <li>ensure job object - based on retention policy (either reuse or new)
 * <li>transition state to starting...housecleaning....running
 * <li>that sets up the jobcontext object (per thread). contexts are the jvm
 * tracking thing (holding logger etc), jobs are persistent (and GWT-friendly)
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

	private AtomicInteger actionCounter = new AtomicInteger();

	private Queue<JobContext> activeJobs = new ConcurrentLinkedQueue<>();

	public JobRegistry() {
		TransformCommit.get()
				.setBackendTransformQueueMaxDelay(TRANSFORM_QUEUE_NAME, 1000);
	}

	public JobResult getJobResult(Job job) {
		return null;
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence()
				.listLogItemsForClass(action.getClass().getName(), count);
	}

	public JobResult perform(Task task) {
		Job job = createJob(task);
		performJob(job);
		waitFor(job);
		return job.asJobResult();
	}

	public JobResult run(RemoteAction action) {
		// TODO Auto-generated method stub
		return null;
	}

	public Job start(RemoteAction action) {
		checkAnnotatedPermissions(action);
		TaskPerformer performer = Registry.impl(TaskPerformer.class,
				action.getClass());
		// because we're spawning the thread, we use this pattern to allow for
		// getting to the countDown() in the spawned thread before the await()
		// in the launcher
		ActionLauncherAsync async = new ActionLauncherAsync(
				performer.getClass().getSimpleName() + "-"
						+ (actionCounter.incrementAndGet()),
				action);
		JobTracker tracker = async.launchAndWaitForTracker();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startService() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
	}

	public void waitUntilComplete(Job job) {
		// TODO Auto-generated method stub
	}

	private Job createJob(Task task) {
		checkAnnotatedPermissions(task);
		Job job = AlcinaPersistentEntityImpl.create(Job.class);
		job.setUser(PermissionsManager.get().getUser());
		job.setKey(task.provideJobKey());
		job.setState(JobState.PENDING);
		job.setTask(task);
		return job;
	}

	private void performJob(Job job) {
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

	private void waitFor(Job job) {
		// TODO Auto-generated method stub
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

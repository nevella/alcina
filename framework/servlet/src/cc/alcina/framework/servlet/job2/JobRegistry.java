package cc.alcina.framework.servlet.job2;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionResult;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetricData;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit.TransformPriorityStd;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.job.JobRegistry1;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.control.WriterService;

@RegistryLocation(registryPoint = JobRegistry.class, implementationType = ImplementationType.SINGLETON)
public class JobRegistry extends WriterService {
	public static final String CONTEXT_NO_ACTION_LOG = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_NO_ACTION_LOG";

	public static JobRegistry get() {
		return Registry.impl(JobRegistry.class);
	}

	private AtomicInteger actionCounter = new AtomicInteger();

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
		return null;
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

	String describeTask(Task task, String msg) {
		msg += "Clazz: " + task.getClass().getName() + "\n";
		msg += "User: " + PermissionsManager.get().getUserString() + "\n";
		msg += "\nParameters: \n";
		try {
			msg += new JacksonJsonObjectSerializer().withIdRefs()
					.withMaxLength(1000000).serializeNoThrow(task);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return msg;
	}

	public class ActionLauncher<T> {
		private JobTracker actionTracker;

		TopicListener<JobTracker> startListener = new TopicListener<JobTracker>() {
			boolean processed = false;

			@Override
			public void topicPublished(String key, JobTracker message) {
				if (processed) {
				} else {
					processed = true;
					actionTracker = message;
				}
			}
		};

		protected ActionLogItem trackerToResult(final RemoteAction action) {
			ActionLogItem logItem = AlcinaPersistentEntityImpl
					.getNewImplementationInstance(ActionLogItem.class);
			logItem.setActionClass(action.getClass());
			logItem.setActionDate(new Date());
			logItem.setShortDescription(CommonUtils
					.trimToWsChars(actionTracker.getJobResult(), 220));
			if (!LooseContext.is(CONTEXT_NO_ACTION_LOG)) {
				logItem.setActionLog(actionTracker.getLog());
			}
			return logItem;
		}

		protected ActionResult<T> trackerToResult(final RemoteAction action,
				boolean nonPersistent) {
			ActionResult<T> result = new ActionResult<T>();
			if (actionTracker != null) {
				ActionLogItem logItem = trackerToResult(action);
				if (!actionTracker.provideIsRoot() || nonPersistent) {
				} else {
					try {
						Registry.impl(CommonPersistenceProvider.class)
								.getCommonPersistence().logActionItem(logItem);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
				result.actionLogItem = logItem;
				result.resultObject = (T) actionTracker.getJobResultObject();
			}
			return result;
		}

		ActionResult<T> perform(final RemoteAction action) throws WebException {
			checkAnnotatedPermissions(action);
			TaskPerformer performer = (TaskPerformer) Registry.get()
					.instantiateSingle(TaskPerformer.class, action.getClass());
			boolean nonPersistent = LooseContext
					.is(JobRegistry1.CONTEXT_NON_PERSISTENT) || Ax.isTest();
			TransformManager transformManager = TransformManager.get();
			boolean noHttpContext = AlcinaServletContext.httpContext() == null;
			try {
				if (transformManager instanceof ThreadlocalTransformManager) {
					ThreadlocalTransformManager.get().resetTltm(null);
				}
				Transaction.ensureEnded();
				Transaction.begin();
				LooseContext.push();
				if (noHttpContext) {
					ActionPerformerMetricFilter filter = Registry
							.impl(ActionPerformerMetricFilter.class);
					InternalMetrics.get().startTracker(action,
							() -> describeTask(action, ""),
							InternalMetricTypeAlcina.service,
							action.getClass().getSimpleName(), () -> true);
				}
				LooseContext.getContext().addTopicListener(
						JobRegistry1.TOPIC_JOB_STARTED, startListener);
				performer.performAction(action);
				return trackerToResult(action, nonPersistent);
			} catch (Throwable t) {
				Exception e = (Exception) ((t instanceof Exception) ? t
						: new WrappedRuntimeException(t));
				if (actionTracker != null && !actionTracker.isComplete()) {
					JobRegistry1.get().jobError(e);
					trackerToResult(action, nonPersistent);
				}
				if (CommonUtils.extractCauseOfClass(e,
						CancelledException.class) != null) {
				} else {
					EntityLayerLogging
							.persistentLog(LogMessageType.TASK_EXCEPTION, e);
				}
				throw new WebException(e);
			} finally {
				if (noHttpContext) {
					InternalMetrics.get().endTracker(action);
				}
				LooseContext.pop();
				if (transformManager instanceof ThreadlocalTransformManager) {
					ThreadlocalTransformManager.get().resetTltm(null);
				}
				Transaction.endAndBeginNew();
			}
		}
	}

	@RegistryLocation(registryPoint = ActionPerformerMetricFilter.class, implementationType = ImplementationType.SINGLETON)
	public static class ActionPerformerMetricFilter
			implements Predicate<InternalMetricData> {
		@Override
		public boolean test(InternalMetricData imd) {
			return false;
		}
	}

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
			TransformCommit.setPriority(TransformPriorityStd.Job);
			perform(this.action);
		}
	}
}

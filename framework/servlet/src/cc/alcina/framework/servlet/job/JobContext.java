package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.job.JobRegistry.ActionPerformerTrackMetrics;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

public class JobContext {
	static final String CONTEXT_CURRENT = JobContext.class.getName()
			+ ".CONTEXT_CURRENT";

	static final String CONTEXT_LOG_MAX_CHARS = JobContext.class.getName()
			+ ".CONTEXT_LOG_MAX_CHARS";

	private static final String CONTEXT_EX_JOB_RESOURCES = JobContext.class
			.getName() + ".CONTEXT_EX_JOB_RESOURCES";

	public static final String CONTEXT_KNOWN_OUTSIDE_JOB = JobContext.class
			.getName() + ".CONTEXT_KNOWN_OUTSIDE_JOB";

	public static void acquireResource(JobResource resource) {
		if (has()) {
			JobRegistry.get().acquireResource(get().getJob(), resource);
		} else {
			resource.acquire();
			LooseContext.ensure(CONTEXT_EX_JOB_RESOURCES,
					() -> new ArrayList<JobResource>()).add(resource);
		}
	}

	public static void adopt(JobContext jobContext, boolean adopt) {
		if (adopt) {
			Preconditions.checkState(!has());
			LooseContext.set(CONTEXT_CURRENT, jobContext);
		} else {
			Preconditions.checkState(has());
			LooseContext.remove(CONTEXT_CURRENT);
		}
	}

	public static <T> T callWithResource(JobResource resource,
			Callable<T> callable) {
		try {
			acquireResource(resource);
			return callable.call();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseResourceIfExContext(resource);
		}
	}

	public static void checkCancelled() {
		if (has()) {
			get().checkCancelled0(false);
		}
	}

	public static void debug(String template, Object... args) {
		if (get() == null) {
			Ax.out("Called JobContext.debug() outside job - %s %s", template,
					Arrays.asList(args));
		} else {
			get().getLogger().debug(template, args);
		}
	}

	public static JobContext get() {
		return LooseContext.get(CONTEXT_CURRENT);
	}

	public static <JR extends JobResource> Optional<JR>
			getAcquiredResource(JR match) {
		if (has()) {
			return JobRegistry.get().getAcquiredResource(get().getJob(), match);
		} else {
			Optional<JR> result = Optional.empty();
			List<JobResource> resources = LooseContext
					.get(CONTEXT_EX_JOB_RESOURCES);
			if (resources != null) {
				result = resources.stream().filter(r -> r.equals(match))
						.map(r -> (JR) r).findFirst();
			}
			return result;
		}
	}

	public static boolean has() {
		return get() != null;
	}

	public static void info(String template, Object... args) {
		if (get() == null) {
			String message = Ax.format(template.replace("{}", "%"), args);
			if (LooseContext.is(CONTEXT_KNOWN_OUTSIDE_JOB)) {
				Ax.out(message);
			} else {
				Ax.out("Called JobContext.info() outside job - %s", message);
			}
		} else {
			get().getLogger().info(template, args);
		}
	}

	public static void jobException(Exception e) {
		if (has()) {
			get().onJobException(e);
		} else {
			e.printStackTrace();
		}
	}

	public static ProgressBuilder progressBuilder() {
		return get().createProgressBuilder();
	}

	public static void releaseResourceIfExContext(JobResource match) {
		if (has()) {
		} else {
			List<JobResource> resources = LooseContext
					.get(CONTEXT_EX_JOB_RESOURCES);
			JobResource jobResource = resources.stream()
					.filter(r -> r.equals(match)).findFirst().get();
			jobResource.release();
			resources.remove(jobResource);
		}
	}

	public static void setCompletion(double completion) {
		if (has()) {
			get().maybeEnqueue(() -> get().getJob().setCompletion(completion));
		} else {
			LoggerFactory.getLogger(JobContext.class)
					.info("(no-job) job completion => {}", completion);
		}
	}

	public static void setLargeResult(Object largeResult) {
		if (has()) {
			get().getJob().setLargeResult(largeResult);
		} else {
			ResourceUtilities.logToFile(largeResult.toString());
		}
	}

	public static void setStatusMessage(String template, Object... args) {
		if (has()) {
			get().maybeEnqueue(() -> get().getJob()
					.setStatusMessage(Ax.format(template, args)));
		} else {
			LoggerFactory.getLogger(JobContext.class).info(
					"(no-job) status message: {}", Ax.format(template, args));
		}
	}

	public static void warn(String template, Exception ex) {
		get().getLogger().warn(template, ex);
	}

	public static void warn(String template, Object... args) {
		if (get() == null) {
			Ax.err("Called JobContext.warn() outside job - %s %s", template,
					Arrays.asList(args));
		} else {
			get().getLogger().warn(template, args);
		}
	}

	private boolean enqueueProgressOnBackend;

	private TaskPerformer performer;

	private Logger logger;

	private boolean noHttpContext;

	Thread thread;

	private String log;

	private JobAllocator allocator;

	private Job job;

	private int itemCount;

	private int itemsCompleted;

	private String threadStartName;

	private CountDownLatch endedLatch = new CountDownLatch(1);

	LauncherThreadState launcherThreadState;

	public JobContext(Job job, TaskPerformer performer,
			LauncherThreadState launcherThreadState, JobAllocator allocator) {
		this.job = job;
		this.performer = performer;
		this.launcherThreadState = launcherThreadState;
		this.allocator = allocator;
		this.logger = LoggerFactory.getLogger(performer.getClass());
		noHttpContext = AlcinaServletContext.httpContext() == null;
	}

	public ExecutionConstraints getExecutionConstraints() {
		return allocator.getExecutionConstraints();
	}

	public Job getJob() {
		return this.job;
	}

	public String getLog() {
		return this.log;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public TaskPerformer getPerformer() {
		return this.performer;
	}

	public void incrementItemCount(int delta) {
		itemCount += delta;
	}

	public boolean isEnqueueProgressOnBackend() {
		return this.enqueueProgressOnBackend;
	}

	public void jobOk(String resultMessage) {
		job.setResultMessage(resultMessage);
		job.setStatusMessage(resultMessage);
		job.setResultType(JobResultType.OK);
	}

	public void jobProgress(String message, double completion) {
		setStatusMessage(message);
		getJob().setCompletion(completion);
	}

	public void onJobException(Exception e) {
		Transaction.ensureEnded();
		Transaction.begin();
		job.setResultType(JobResultType.EXCEPTION);
		String simpleExceptionMessage = CommonUtils.toSimpleExceptionMessage(e);
		job.setStatusMessage(simpleExceptionMessage);
		job.setResultMessage(simpleExceptionMessage);
		logger.warn("Unexpected job exception - job {}", e, job.getId());
		e.printStackTrace();
	}

	public void remove() {
		LooseContext.remove(CONTEXT_CURRENT);
	}

	public void setEnqueueProgressOnBackend(boolean enqueueProgressOnBackend) {
		this.enqueueProgressOnBackend = enqueueProgressOnBackend;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public void setItemsCompleted(int itemsCompleted) {
		this.itemsCompleted = itemsCompleted;
	}

	public void setResultMessage(String resultMessage) {
		info(resultMessage);
		get().getJob().setResultMessage(resultMessage);
	}

	public void toAwaitingChildren() {
		Transaction.commit();
		allocator.toAwaitingChildren();
	}

	@Override
	public String toString() {
		return Ax.format("JobContext :: Thread - %s; Job - %s", thread, job);
	}

	public void updateJob(String message, int delta) {
		itemsCompleted += delta;
		setStatusMessage("%s (%s/%s)", message, itemsCompleted, itemCount);
	}

	public void updateProcessState(ProcessState processState) {
		if (allocator.thread != null) {
			processState.setAllocatorThreadName(allocator.thread.getName());
		}
		if (thread != null) {
			processState.setThreadName(thread.getName());
			processState.setStackTrace(SEUtilities.getFullStacktrace(thread));
		}
	}

	private ProgressBuilder createProgressBuilder() {
		return new ProgressBuilder();
	}

	private void end0() {
		Transaction.ensureBegun();
		if (noHttpContext) {
			InternalMetrics.get().endTracker(performer);
		}
		if (job.provideIsNotComplete()) {
			log = Registry.impl(PerThreadLogging.class).endBuffer();
			int maxChars = LooseContext
					.<Integer> optional(CONTEXT_LOG_MAX_CHARS).orElse(5000000);
			log = CommonUtils.trimToWsChars(log, maxChars, true);
			job.setLog(log);
			if (job.provideRelatedSequential().stream().filter(j -> j != job)
					.anyMatch(Job::provideIsNotComplete)) {
				job.setState(JobState.COMPLETED);
				allocator.ensureStarted();
			} else {
				job.setState(JobState.SEQUENCE_COMPLETE);
			}
			job.setEndTime(new Date());
			if (job.getResultType() == null) {
				job.setResultType(JobResultType.OK);
			}
			logger.info("Job complete - {} - {} - {} ms", job, job.getEndTime(),
					job.getEndTime().getTime() - job.getStartTime().getTime());
		}
		persistMetadata();
		if (threadStartName != null) {
			Thread.currentThread().setName(threadStartName);
		}
		endedLatch.countDown();
	}

	private void maybeEnqueue(Runnable runnable) {
		if (isEnqueueProgressOnBackend()) {
			TransformCommit.get().enqueueBackendTransform(runnable);
		} else {
			runnable.run();
		}
	}

	protected void persistMetadata() {
		if (performer.deferMetadataPersistence(job)) {
			TransformCommit.enqueueTransforms(JobRegistry.TRANSFORM_QUEUE_NAME);
		} else {
			/*
			 * Because of possible collisions with stacktrace?
			 */
			TransformCommit.commitWithBackoff();
		}
	}

	void awaitChildCompletion() {
		allocator.awaitChildCompletion(this);
	}

	void awaitSequenceCompletion() {
		Transaction.ensureEnded();
		try {
			endedLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		allocator.awaitSequenceCompletion();
		Transaction.begin();
	}

	void checkCancelled0(boolean ignoreSelf) {
		Job cursor = job;
		if (ignoreSelf) {
			Optional<Job> parent = cursor.provideFirstInSequence()
					.provideParent();
			if (parent.isPresent()) {
				cursor = parent.get();
			} else {
				return;
			}
		}
		while (true) {
			if (cursor.provideIsComplete()) {
				info("Job cancelled");
				throw new CancelledException("Job cancelled");
			}
			Optional<Job> parent = cursor.provideFirstInSequence()
					.provideParent();
			if (parent.isPresent()) {
				cursor = parent.get();
			} else {
				return;
			}
		}
	}

	void clearRefs() {
		performer = null;
		// not allocator - it's a circular reference anyway, and required for
		// awaitSequenceCompletion
		// allocator = null;
		thread = null;
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

	void end() {
		if (performer.endInLockedSection()) {
			/*
			 * per task class lock
			 */
			JobRegistry.get().withJobMetadataLock(job.getTaskClassName(),
					this::end0);
		} else {
			end0();
		}
	}

	void start() {
		LooseContext.set(CONTEXT_CURRENT, this);
		thread = Thread.currentThread();
		threadStartName = thread.getName();
		if (job.provideIsNotComplete()) {
			thread.setName(job.getTask().getClass().getSimpleName() + "::"
					+ threadStartName);
			job.setStartTime(new Date());
			job.setState(JobState.PROCESSING);
			job.setPerformerVersionNumber(performer.getVersionNumber());
			persistMetadata();
		}
		Registry.impl(PerThreadLogging.class).beginBuffer();
		if (noHttpContext) {
			ActionPerformerTrackMetrics filter = Registry
					.impl(ActionPerformerTrackMetrics.class);
			InternalMetrics.get().startTracker(performer,
					() -> describeTask(job.getTask(), ""),
					InternalMetricTypeAlcina.service,
					performer.getClass().getSimpleName(), filter);
		}
	}

	public class ProgressBuilder {
		private String message;

		private int delta;

		private int total = 1;

		private boolean log;

		public void publish() {
			setItemCount(total);
			updateJob(message, delta);
			if (log) {
				getLogger().info(message);
			}
		}

		public ProgressBuilder withDelta(int delta) {
			this.delta = delta;
			return this;
		}

		public ProgressBuilder withLog(boolean log) {
			this.log = log;
			return this;
		}

		public ProgressBuilder withMessage(String template, Object... args) {
			this.message = Ax.format(template, args);
			return this;
		}

		public ProgressBuilder withTotal(int total) {
			this.total = total;
			return this;
		}
	}
}

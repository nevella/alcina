package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Timeout;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.BackendTransformQueue;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.servlet.job.JobRegistry.ActionPerformerTrackMetrics;
import cc.alcina.framework.servlet.job.JobRegistry.LatchType;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

/**
 * <p>
 * Logging note - I tried debouncing log messages for higher performance - but
 * that's fundamentally dangerous, since the runnables were evaluated on another
 * (eventcollator) thread
 * 
 * <p>
 * Instead, the debouncer works on the same thread, and the performance cost
 * gets paid. Commits occur off-thread (see {@link BackendTransformQueue})
 * 
 * <p>
 * Another threading topic is checking for cancelled - since jobs will run in a
 * transaction, the cancelled check is run off-thread (in a shared executor )
 */
public class JobContext {
	static final String CONTEXT_CURRENT = JobContext.class.getName()
			+ ".CONTEXT_CURRENT";

	static final String CONTEXT_LOG_MAX_CHARS = JobContext.class.getName()
			+ ".CONTEXT_LOG_MAX_CHARS";

	private static final String CONTEXT_EX_JOB_RESOURCES = JobContext.class
			.getName() + ".CONTEXT_EX_JOB_RESOURCES";

	/*
	 * use only for limited, non-job devconsole testing
	 */
	public static final String CONTEXT_IGNORE_RESOURCES = JobContext.class
			.getName() + ".CONTEXT_IGNORE_RESOURCES";

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
		if (ignoreResource(resource)) {
			try {
				return callable.call();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
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
			// ensure tx *with headroom*
			Transaction.ensureAndRestartIfOlderThan(
					Transaction.getMaxAgeOrDefault() / 2);
			get().checkCancelled0(false);
		}
	}

	public static void debug(String template, Object... args) {
		if (get() == null) {
			Ax.out("Called JobContext.debug() outside job - %s ",
					Ax.format(template, Arrays.asList(args)));
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

	public static TreeProcess.Node getSelectedProcessNode() {
		TreeProcess treeProcess = has() ? get().getTreeProcess()
				: new TreeProcess(JobContext.class);
		return treeProcess.getSelectedNode();
	}

	//
	public static boolean has() {
		return get() != null;
	}

	static boolean ignoreResource(JobResource resource) {
		return LooseContext.is(CONTEXT_IGNORE_RESOURCES);
	}

	public static void info(String template, Object... args) {
		info0(false, template, args);
	}

	static void info0(boolean once, String template, Object... args) {
		String message = Ax.format(template.replace("{}", "%s"), args);
		if (has()) {
			if (once && !get().isFirstTimeMessage(message)) {
				// duplicate
			} else {
				get().getLogger().info(template, args);
			}
		} else {
			if (LooseContext.is(CONTEXT_KNOWN_OUTSIDE_JOB)) {
				Ax.out(message);
			} else {
				Ax.out("Called JobContext.info() outside job - %s", message);
			}
		}
	}

	public static void infoOnce(String template, Object... args) {
		info0(true, template, args);
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

	public static void releaseResourceIfExContext(JobResource resource) {
		if (has() || ignoreResource(resource)) {
		} else {
			List<JobResource> resources = LooseContext
					.get(CONTEXT_EX_JOB_RESOURCES);
			JobResource jobResource = resources.stream()
					.filter(r -> r.equals(resource)).findFirst().get();
			jobResource.release();
			resources.remove(jobResource);
		}
	}

	public static void setCompletion(double completion) {
		if (has()) {
			get().enqueue(() -> get().getJob().setCompletion(completion));
		} else {
			LoggerFactory.getLogger(JobContext.class)
					.info("(no-job) job completion => {}", completion);
		}
	}

	public static void setLargeResult(Object largeResult) {
		if (has()) {
			get().getJob().setLargeResult(largeResult);
		} else {
			if (largeResult == null) {
				Ax.sysLogHigh("Large result is null");
				return;
			}
			Ax.out("Logging large result (ex-job context)\n========================");
			Io.log().toFile(largeResult.toString());
		}
	}

	public static void setResultMessage(String resultMessage) {
		info(resultMessage);
		if (has()) {
			get().getJob().setResultMessage(resultMessage);
		}
	}

	public static void setStatusMessage(String template, Object... args) {
		String fixedTemplate = template.contains("{}")
				? template = template.replace("{}", "%s")
				: template;
		String message = Ax.format(fixedTemplate, args);
		setStatusMessage(() -> message, false);
	}

	/**
	 * 
	 * @param messageSupplier
	 * @param debounceable
	 *            indicates the caller is a treeprocess, messages can be
	 *            debounced
	 */
	public static void setStatusMessage(Supplier<String> messageSupplier,
			boolean debounceable) {
		if (has()) {
			get().updateStatusDebouncer.fireUpdateStatus(messageSupplier,
					debounceable);
		} else {
			LoggerFactory.getLogger(JobContext.class)
					.info("(no-job) status message: {}", messageSupplier.get());
		}
	}

	public static void warn(String template, Exception ex) {
		get().getLogger().warn(template, ex);
	}

	public static void warn(String template, Object... args) {
		String formatted = Ax.format(template.replace("{}", "%s"), args);
		if (Ax.isTest()) {
			Ax.err("[WARN] - %s", formatted);
		}
		if (get() == null) {
			Ax.err("Called JobContext.warn() outside job - %s", formatted);
		} else {
			get().getLogger().warn(template, args);
		}
	}

	Set<String> publishedMessages = Collections
			.synchronizedSet(new LinkedHashSet<>());

	class UpdateStatusDebouncer {
		EventCollator<Runnable> debouncer = new EventCollator<Runnable>(200,
				JobContext.this::updateStatus).withMaxDelayFromFirstEvent(1)
						.withMaxDelayFromFirstCollatedEvent(1000);

		long lastMessageTime;

		Supplier<String> debouncedSupplier;

		void cancel() {
			if (debouncedSupplier != null) {
				debounceMessage(debouncedSupplier.get());
				debouncedSupplier = null;
			}
			debouncer.cancel();
		}

		void fireUpdateStatus(Supplier<String> messageSupplier,
				boolean debounceable) {
			/*
			 * see note in the class doc - the threading issues are just too
			 * dangerous to put this supplier on another thread. But we can by
			 * fiat say 'just replicate any message in the last 5ms *on* thread'
			 * - good enough
			 * 
			 * 
			 */
			if (TimeConstants.within(lastMessageTime, 5) && debounceable) {
				debouncedSupplier = messageSupplier;
				return;
			}
			debouncedSupplier = null;
			lastMessageTime = System.currentTimeMillis();
			debounceMessage(messageSupplier.get());
		}

		void debounceMessage(String message) {
			debouncer.eventOccurred(() -> {
				updateJobStatusDebounced(message);
				MethodContext.instance().withThreadName(thread.getName())
						.run(() -> LoggerFactory.getLogger(JobContext.class)
								.info("status message: {}", message));
			});
		}
	}

	void updateJobStatusDebounced(String message) {
		Job job = getJob();
		job.setStatusMessage(message);
		if (itemCount > 0 && itemsCompleted > 0) {
			setCompletion(((double) itemsCompleted) / ((double) itemCount));
		}
	}

	UpdateStatusDebouncer updateStatusDebouncer = new UpdateStatusDebouncer();

	void updateStatus(EventCollator<Runnable> collator) {
		/*
		 * updateStatusDebouncer.cancel() will execute the collator task on the
		 * main job thread
		 */
		if (JobContext.has()) {
			collator.getLastObject().run();
		} else {
			try {
				JobContext.adopt(this, true);
				// FIXME - this duplicates some of the other debounce routing
				JobRegistry.get().environment.updateJobStatus(this,
						collator.getLastObject());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				JobContext.adopt(this, false);
			}
		}
	}

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

	private TreeProcess treeProcess;

	public Object typedResult;

	public <V> V typedResult() {
		return (V) typedResult;
	}

	public JobContext(Job job, TaskPerformer performer,
			LauncherThreadState launcherThreadState, JobAllocator allocator) {
		this.job = job;
		this.performer = performer;
		treeProcess = new TreeProcess(performer);
		treeProcess.topicPositionChangedMessage
				.add(v -> JobContext.setStatusMessage(v, true));
		this.launcherThreadState = launcherThreadState;
		this.allocator = allocator;
		this.logger = LoggerFactory.getLogger(performer.getClass());
		noHttpContext = AlcinaServletContext.httpContext() == null;
	}

	/*
	 * Public (since jobs may want to launch logical children, await, continue)
	 * - but restricted to performer thread. Note that the state should be
	 * switched back from to SubqueuePhase.Self once the children have completed
	 * - that's a future refinment.
	 *
	 * Note that if parent job behaviour is setup - run children - cleanuup -
	 * finish, and the parent has no subsequent (sequence) jobs, current 'stay
	 * in SubqueuePhase.Children' behaviour will _work_ - but it's not pretty.
	 *
	 * FIXME - mvcc.5 - allow self/children execution interleaving
	 */
	public void awaitChildCompletion() {
		Preconditions.checkArgument(thread == Thread.currentThread());
		allocator.awaitChildCompletion(this);
	}

	void awaitSequenceCompletion() {
		TransactionEnvironment.get().ensureEnded();
		try {
			// this should be LatchType.POST_CHILD_COMPLETION - but leaving at
			// SEQUENCE_COMPLETION pending debugging
			JobRegistry.awaitLatch(job, endedLatch,
					LatchType.SEQUENCE_COMPLETION);
			if (!hadEnd0Exception) {
				allocator.awaitSequenceCompletion();
			}
		} catch (Exception e) {
			logger.warn("DEVEX-0 -- job sequence timeout/interruption", e);
		}
		TransactionEnvironment.get().begin();
	}

	void beginLogBuffer() {
		Registry.impl(PerThreadLogging.class).beginBuffer();
	}

	Timeout lastCheckCancelled = new Timeout(200).withTimeoutOnNextCheck();

	private boolean hadEnd0Exception;

	void checkCancelled0(boolean ignoreSelf) {
		if (lastCheckCancelled.checkAndReset()) {
			return;
		}
		if (computeCancelled(ignoreSelf)) {
			info("Job cancelled");
			throw new CancelledException("Job cancelled");
		}
	}

	/*
	 * This should not timeout, but the check definitely shouldn't take more
	 * than one second
	 */
	boolean computeCancelled(boolean ignoreSelf) {
		Job cursor = job;
		if (ignoreSelf) {
			Optional<Job> parent = cursor.provideFirstInSequence()
					.provideParent();
			if (parent.isPresent()) {
				cursor = parent.get();
			} else {
				return false;
			}
		}
		while (true) {
			if (JobDomain.isComplete(cursor)) {
				return true;
			}
			Optional<Job> parent = cursor.provideFirstInSequence()
					.provideParent();
			if (parent.isPresent()) {
				cursor = parent.get();
			} else {
				return false;
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

	private ProgressBuilder createProgressBuilder() {
		return new ProgressBuilder();
	}

	String describeTask(Task task, String msg) {
		msg += "Clazz: " + task.getClass().getName() + "\n";
		msg += "User: " + Permissions.get().getUserString() + "\n";
		msg += "\nParameters: \n";
		try {
			msg += new JacksonJsonObjectSerializer().withIdRefs()
					.serializeNoThrow(task);
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

	void flushLog() {
		if (job.provideIsNotComplete()) {
			// occurs just before end, since this method is possibly called
			// on a
			// different thread to the logbuffer context
			// log = Registry.impl(PerThreadLogging.class).endBuffer();
			int maxChars = LooseContext
					.<Integer> optional(CONTEXT_LOG_MAX_CHARS).orElse(5000000);
			log = CommonUtils.trimToWsChars(log, maxChars, true);
			job.setLog(log);
		}
	}

	private void end0() {
		try {
			Transaction.ensureBegun();
			if (noHttpContext) {
				if (JobRegistry.get().environment.isTrackMetrics()) {
					InternalMetrics.get().endTracker(performer);
				}
			}
			updateStatusDebouncer.cancel();
			if (job.provideIsNotComplete()) {
				if (job.provideRelatedSequential().stream()
						.filter(j -> j != job)
						.anyMatch(Job::provideIsNotComplete)) {
					job.setState(JobState.COMPLETED);
					allocator.ensureStarted();
				} else {
					job.setState(JobState.SEQUENCE_COMPLETE);
				}
				if (typedResult != null) {
					job.setLargeResult(typedResult);
				}
				job.setEndTime(new Date());
				if (job.getResultType() == null) {
					job.setResultType(JobResultType.OK);
				}
				logger.info("Job complete - {} - {} - {} ms", job,
						job.getEndTime(), job.getEndTime().getTime()
								- job.getStartTime().getTime());
			}
			treeProcess = null;
			persistMetadata();
		} catch (Throwable t) {
			logger.warn("DEVEX-0 -- job end0 issue - " + job.getId(), t);
			hadEnd0Exception = true;
			throw t;
		} finally {
			endedLatch.countDown();
		}
	}

	public void endLogBuffer() {
		if (job.provideIsNotComplete()) {
			log = Registry.impl(PerThreadLogging.class).endBuffer();
		}
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

	public TreeProcess getTreeProcess() {
		return this.treeProcess;
	}

	public void incrementItemCount(int delta) {
		itemCount += delta;
	}

	boolean isFirstTimeMessage(String message) {
		return publishedMessages.add(message);
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

	void enqueue(Runnable runnable) {
		TransformCommit.get().enqueueBackendTransform(runnable,
				JobRegistry.TRANSFORM_QUEUE_NAME);
	}

	public void onJobException(Exception e) {
		Transaction.ensureEnded();
		Transaction.begin();
		job.setResultType(JobResultType.EXCEPTION);
		String simpleExceptionMessage = CommonUtils.toSimpleExceptionMessage(e);
		job.setStatusMessage(simpleExceptionMessage);
		job.setResultMessage(simpleExceptionMessage);
		TransactionEnvironment.get().commit();
		logger.warn("Unexpected job exception - job {}", e, job.getId());
		e.printStackTrace();
	}

	protected void persistMetadata() {
		if (performer.deferMetadataPersistence(job)) {
			TransformCommit.enqueueTransforms(JobRegistry.TRANSFORM_QUEUE_NAME);
		} else {
			/*
			 * Because of possible collisions with stacktrace?
			 */
			TransactionEnvironment.get().commitWithBackoff();
		}
	}

	void persistStart() {
		if (job.provideIsNotComplete()) {
			// Threading - guaranteed that this is sole mutating thread (for
			// job)
			job.setStartTime(new Date());
			job.setState(JobState.PROCESSING);
			job.setPerformerVersionNumber(performer.getVersionNumber());
			new JobObservable.ToProcessing(job).publish();
			persistMetadata();
		}
	}

	public void recordLargeInMemoryResult(String largeSerializedResult) {
		new JobRegistry.InMemoryResult(largeSerializedResult, getJob())
				.record();
	}

	public void remove() {
		LooseContext.remove(CONTEXT_CURRENT);
	}

	void restoreThreadName() {
		if (threadStartName != null) {
			Thread.currentThread().setName(threadStartName);
		}
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public void setItemsCompleted(int itemsCompleted) {
		this.itemsCompleted = itemsCompleted;
	}

	void start() {
		LooseContext.set(CONTEXT_CURRENT, this);
		thread = Thread.currentThread();
		threadStartName = thread.getName();
		if (job.provideIsNotComplete()) {
			String contextThreadName = Ax.format("%s::%s::%s",
					job.provideTaskClass().getSimpleName(),
					job.toLocator().toIdOrNegativeLocalIdString(),
					threadStartName);
			thread.setName(contextThreadName);
		}
		if (noHttpContext) {
			ActionPerformerTrackMetrics filter = Registry
					.impl(ActionPerformerTrackMetrics.class);
			if (JobRegistry.get().environment.isTrackMetrics()) {
				InternalMetrics.get().startTracker(performer,
						() -> describeTask(job.getTask(), ""),
						InternalMetricTypeAlcina.service,
						performer.getClass().getSimpleName(), filter);
			}
		}
	}

	void toAwaitingChildren() {
		TransactionEnvironment.get().commit();
		allocator.toAwaitingChildren(this);
	}

	@Override
	public String toString() {
		return Ax.format("JobContext :: Thread - %s; Job - %s", thread, job);
	}

	public void updateJob(String message, int delta) {
		itemsCompleted += delta;
		setStatusMessage("%s (%s/%s)", message, itemsCompleted, itemCount);
	}

	void updateProcessState(ProcessState processState) {
		if (allocator.thread != null) {
			processState.setAllocatorThreadName(allocator.thread.getName());
		}
		if (thread != null) {
			processState.setThreadName(thread.getName());
			processState.setStackTrace(SEUtilities.getFullStacktrace(thread));
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

	@Registration(TreeProcess.SelectedProcessNodeProvider.class)
	public static class SelectedProcessNodeProviderImpl
			implements TreeProcess.SelectedProcessNodeProvider {
		@Override
		public Node getSelectedProcessNode() {
			return JobContext.getSelectedProcessNode();
		}
	}

	public static String getJobTreeContextAsString() {
		FormatBuilder format = new FormatBuilder();
		String prelude = Ax.format("Job tree - thread %s",
				Thread.currentThread().getName());
		format.line(prelude);
		if (!JobContext.has()) {
			format.append("[no job context]");
		} else {
			List<String> jobStrings = new ArrayList<>();
			Job jobCursor = JobContext.get().getJob();
			while (jobCursor != null) {
				jobStrings.add(0, jobCursor.toString());
				Set<? extends JobRelation> toRelations = jobCursor
						.getToRelations();
				Optional<? extends JobRelation> awaiter = toRelations.stream()
						.filter(t -> t.getType() == JobRelationType.AWAITED)
						.findFirst();
				if (awaiter.isPresent()) {
					jobCursor = awaiter.get().getFrom();
					continue;
				}
				jobCursor = jobCursor.provideParent().orElse(null);
			}
			jobStrings.forEach(js -> format.line("job - %s", js));
		}
		return format.toString();
	}
}

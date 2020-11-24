package cc.alcina.framework.servlet.job;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.JobResource;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.RelatedJobCompletion;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.job.JobRegistry.ActionPerformerTrackMetrics;
import cc.alcina.framework.servlet.job.JobRegistry.SequenceCompletionLatch;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

public class JobContext {
	static final String CONTEXT_CURRENT = JobContext.class.getName()
			+ ".CONTEXT_CURRENT";

	private static final String CONTEXT_EX_JOB_RESOURCES = JobContext.class
			.getName() + ".CONTEXT_EX_JOB_RESOURCES";

	private static final long AWAIT_CHILDREN_DELTA_TIMEOUT_MS = 30
			* TimeConstants.ONE_MINUTE_MS;

	public static void acquireResource(JobResource resource) {
		if (has()) {
			JobRegistry.get().acquireResource(get().getJob(), resource);
		} else {
			resource.acquire();
			LooseContext.ensure(CONTEXT_EX_JOB_RESOURCES,
					() -> new ArrayList<JobResource>()).add(resource);
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
			get().checkCancelled0();
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
			Ax.out("Called JobContext.info() outside job - %s %s", template,
					Arrays.asList(args));
		} else {
			get().getLogger().info(template, args);
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

	private boolean schedulingSubTasks;

	private Job job;

	private TaskPerformer performer;

	private Logger logger;

	private boolean noHttpContext;

	private PersistenceType persistenceType;

	private int subtaskCount;

	private int subtasksCompleted;

	private int itemCount;

	private int itemsCompleted;

	private LinkedList<RelatedJobCompletion> completionEvents = new LinkedList<>();

	private TopicListener<RelatedJobCompletion> relatedJobCompletionNotifier = (
			k, completion) -> {
		if (completion.job == job) {
			logger.debug(
					"Related job completion changed for job {} - {} complete - {}",
					job, completion.related.size(), completion.related);
			synchronized (completionEvents) {
				completionEvents.add(completion);
				completionEvents.notifyAll();
			}
		}
	};

	Thread thread;

	String threadStartName = null;

	private String log;

	public JobContext(Job job, PersistenceType persistenceType,
			TaskPerformer performer) {
		this.job = job;
		this.persistenceType = persistenceType;
		this.performer = performer;
		this.logger = LoggerFactory.getLogger(performer.getClass());
		noHttpContext = AlcinaServletContext.httpContext() == null;
	}

	public synchronized void awaitChildCompletion() {
		Set<Job> uncompletedChildren = new LinkedHashSet<>();
		long lastCompletionEventTime = System.currentTimeMillis();
		double lastPublishedPercentComplete = 0.0;
		long lastPublishedCompletionStatsTime = System.currentTimeMillis();
		/*
		 * early termination
		 * 
		 */
		if (job.provideIsComplete()
				|| job.provideUncompletedChildren().count() == 0) {
			return;
		}
		try {
			DomainDescriptorJob.get().relatedJobCompletionChanged
					.add(relatedJobCompletionNotifier);
			while (!job.provideIsComplete()) {
				RelatedJobCompletion event = null;
				// must publish statusMessage changes outside the synchronized
				// block
				String statusMessage = null;
				long now = 0L;
				/*
				 * The on-demand creation and event-driven countdown of
				 * uncompletedChildren (rather than checking
				 * job.provideUncompletedChildren() directly) is for performance
				 * reasons - specifically when the job has 10,000s of children.
				 * 
				 * Do our waits outside the synchronized block (to avoid
				 * transform.commit() listener deadlocks) - but get the
				 * uncompleted children _inside_ (to ensure consistency of
				 * completion events vs uncompleted children - i.e. this loop
				 * will receive completion events _after_ the collection get)
				 */
				if (uncompletedChildren.isEmpty()) {
					Transaction.commit();
					DomainStore.waitUntilCurrentRequestsProcessed();
				}
				synchronized (completionEvents) {
					if (uncompletedChildren.isEmpty()) {
						// double-check
						uncompletedChildren = job.provideUncompletedChildren()
								.collect(Collectors.toSet());
						if (uncompletedChildren.isEmpty()) {
							break;
						}
					}
					if (uncompletedChildren.size() > 0
							&& completionEvents.isEmpty()) {
						int totalCount = job.getFromRelations().size();
						int completedCount = totalCount
								- uncompletedChildren.size();
						double percentComplete = ((double) completedCount)
								/ totalCount * 100.0;
						now = System.currentTimeMillis();
						if (percentComplete - lastPublishedPercentComplete > 1
								|| (now - lastPublishedCompletionStatsTime > 3000)) {
							statusMessage = Ax.format(
									"Await child completion - %s% - %s/%s",
									(int) percentComplete, completedCount,
									totalCount);
							lastPublishedPercentComplete = percentComplete;
							lastPublishedCompletionStatsTime = now;
						}
						Transaction.ensureEnded();
						try {
							// FIXME - mvcc.jobs - remove timeout
							completionEvents.wait(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Transaction.begin();
					}
					now = System.currentTimeMillis();
					if (completionEvents.size() > 0) {
						event = completionEvents.removeFirst();
						lastCompletionEventTime = now;
					}
				}
				if (now - lastCompletionEventTime > 30
						* TimeConstants.ONE_SECOND_MS) {
					// FIXME - mvcc.jobs.1a - checking our propagation is OK
					if (job.provideUncompletedChildren()
							.collect(Collectors.toSet())
							.size() != uncompletedChildren.size()) {
						logger.warn(
								"Differing child counts for uncompleted children");
						uncompletedChildren.clear();
					}
				}
				if (now - lastCompletionEventTime > AWAIT_CHILDREN_DELTA_TIMEOUT_MS) {
					// FIXME - mvcc.jobs - A policy?
					logger.warn(
							"Cancelling {} - timed out awaiting child completion",
							job);
					job.cancel();
					Transaction.commit();
					return;
				}
				if (statusMessage != null) {
					JobContext.get().setStatusMessage(statusMessage);
					Transaction.commit();
				}
				if (event != null) {
					uncompletedChildren.removeAll(event.related);
				}
			}
		} finally {
			DomainDescriptorJob.get().relatedJobCompletionChanged
					.remove(relatedJobCompletionNotifier);
		}
	}

	public void end() {
		try {
			Preconditions.checkState(
					persistenceType != PersistenceType.SchedulingSubJobs);
			if (noHttpContext) {
				InternalMetrics.get().endTracker(performer);
			}
			if (job.provideIsNotComplete()) {
				log = Registry.impl(PerThreadLogging.class).endBuffer();
				job.setLog(log);
				if (!job.provideIsComplete()) {
					job.setState(JobState.COMPLETED);
				}
				job.setEndTime(new Date());
				if (job.getResultType() == null) {
					job.setResultType(JobResultType.OK);
				}
				persistMetadata(true);
			}
			if (threadStartName != null) {
				thread.setName(threadStartName);
			}
			if (persistenceType == PersistenceType.None) {
				// don't end transaction (job metadata changes needed for queue
				// exit)
			} else {
				Transaction.endAndBeginNew();
			}
		} finally {
			SequenceCompletionLatch sequenceCompletionLatch = JobRegistry
					.get().completionLatches.remove(job);
			if (sequenceCompletionLatch != null) {
				logger.warn("Fired child completion to sequence latch: {}",
						job);
				sequenceCompletionLatch.onChildJobsCompleted();
			}
		}
	}

	public void followCurrentJobWith(Task followingTask) {
		getJob().followWith(JobRegistry.get().schedule(followingTask));
	}

	public int getItemCount() {
		return this.itemCount;
	}

	public int getItemsCompleted() {
		return this.itemsCompleted;
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

	public int getSubtaskCount() {
		return this.subtaskCount;
	}

	public int getSubtasksCompleted() {
		return this.subtasksCompleted;
	}

	public void incrementItemCount(int delta) {
		itemCount += delta;
	}

	public boolean isSchedulingSubTasks() {
		return this.schedulingSubTasks;
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
		job.setResultType(JobResultType.EXCEPTION);
		String simpleExceptionMessage = CommonUtils.toSimpleExceptionMessage(e);
		job.setStatusMessage(simpleExceptionMessage);
		job.setResultMessage(simpleExceptionMessage);
		logger.warn("Unexpected job exception", e);
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

	public void setSchedulingSubTasks(boolean schedulingSubTasks) {
		this.schedulingSubTasks = schedulingSubTasks;
		if (schedulingSubTasks) {
			persistenceType = PersistenceType.SchedulingSubJobs;
		} else {
			Transaction.commit();
			persistenceType = PersistenceType.Immediate;
		}
	}

	public void setStatusMessage(String template, Object... args) {
		getJob().setStatusMessage(Ax.format(template, args));
	}

	public void setSubtaskCount(int subtaskCount) {
		this.subtaskCount = subtaskCount;
	}

	public void setSubtasksCompleted(int subtasksCompleted) {
		this.subtasksCompleted = subtasksCompleted;
	}

	public File snapshotLog() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return Ax.format("JobContext :: Thread - %s; Job - %s", thread, job);
	}

	public void updateJob(String message, int delta) {
		itemsCompleted += delta;
		setStatusMessage("%s (%s/%s)", message, itemsCompleted, itemCount);
	}

	private ProgressBuilder createProgressBuilder() {
		return new ProgressBuilder();
	}

	protected void persistMetadata(boolean respectImmediate) {
		switch (persistenceType) {
		case Immediate: {
			if (respectImmediate) {
				Transaction.commit();
			} else {
				TransformCommit.enqueueTransforms(
						JobRegistry.TRANSFORM_QUEUE_NAME, Job.class,
						JobRelation.class);
			}
			break;
		}
		case Queued: {
			TransformCommit.enqueueTransforms(JobRegistry.TRANSFORM_QUEUE_NAME,
					Job.class, JobRelation.class);
			break;
		}
		/*
		 * very limited support (no scheduled jobs e.g.)
		 */
		case None:
			TransformCommit.removeTransforms(Job.class, JobRelation.class);
			break;
		case SchedulingSubJobs:
			break;
		}
	}

	void awaitSequenceCompletion() {
		Transaction.endAndBeginNew();
		if (job.provideUncompletedSequential().isEmpty()) {
			return;
		}
		try {
			DomainDescriptorJob.get().relatedJobCompletionChanged
					.add(relatedJobCompletionNotifier);
			while (true) {
				DomainStore.waitUntilCurrentRequestsProcessed();
				RelatedJobCompletion event = null;
				synchronized (completionEvents) {
					if (completionEvents.isEmpty()) {
						Set<Job> uncompletedSequential = job
								.provideUncompletedSequential();
						if (uncompletedSequential.size() == 0) {
							break;
						}
						Transaction.ensureEnded();
						try {
							// FIXME - mvcc.jobs - timeout is for debugging
							completionEvents.wait(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Transaction.begin();
					}
					if (completionEvents.size() > 0) {
						event = completionEvents.removeFirst();
					}
				}
			}
		} finally {
			DomainDescriptorJob.get().relatedJobCompletionChanged
					.remove(relatedJobCompletionNotifier);
		}
	}

	void checkCancelled0() {
		Job cursor = job;
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
			persistMetadata(true);
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

		public void publish() {
			setItemCount(total);
			updateJob(message, delta);
		}

		public ProgressBuilder withDelta(int delta) {
			this.delta = delta;
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

	enum PersistenceType {
		None, Immediate, Queued, SchedulingSubJobs
	}
}

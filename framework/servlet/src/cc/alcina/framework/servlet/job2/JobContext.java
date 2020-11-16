package cc.alcina.framework.servlet.job2;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

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
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.RelatedJobCompletion;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.job2.JobRegistry.ActionPerformerTrackMetrics;
import cc.alcina.framework.servlet.job2.JobRegistry.SequenceCompletionLatch;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

public class JobContext {
	static final String CONTEXT_CURRENT = JobContext.class.getName()
			+ ".CONTEXT_CURRENT";

	private static final long AWAIT_CHILDREN_TIMEOUT_MS = 60000;

	public static void checkCancelled() {
		get().checkCancelled0();
	}

	public static JobContext get() {
		return LooseContext.get(CONTEXT_CURRENT);
	}

	public static void info(String template, Object... args) {
		if (get() == null) {
			Ax.out("Called JobContext.info() outside job - %s %s", template,
					Arrays.asList(args));
		}
		get().getLogger().info(template, args);
	}

	public static void warn(String template, Exception ex) {
		get().getLogger().warn(template, ex);
	}

	private boolean schedulingSubTasks;

	private Job job;

	private TaskPerformer performer;

	private Logger logger;

	private boolean noHttpContext;

	private PersistenceType persistenceType;

	private int subtaskCount;

	private int subtasksCompleted;

	private LinkedList<RelatedJobCompletion> completionEvents = new LinkedList<>();

	private TopicListener<RelatedJobCompletion> relatedJobCompletionNotifier = (
			k, completion) -> {
		if (completion.job == job) {
			logger.debug(
					"Related job completion changed for job {} - {} complete",
					job, completion.related.size());
			synchronized (completionEvents) {
				completionEvents.add(completion);
				completionEvents.notifyAll();
			}
		}
	};

	Thread thread;

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
		while (true) {
			/*
			 * Because jobs can be added to uncompleted children, only populate
			 * in the synchronized block (so we don[t miss events)
			 */
			if (uncompletedChildren.isEmpty()) {
				if (!job.provideUncompletedChildren().anyMatch(j -> true)) {
					break;
				}
			}
			Transaction.commit();
			try {
				DomainDescriptorJob.get().relatedJobCompletionChanged
						.add(relatedJobCompletionNotifier);
				RelatedJobCompletion event = null;
				// must publish outside the synchronized block
				String statusMessage = null;
				synchronized (completionEvents) {
					if (uncompletedChildren.isEmpty()) {
						// double-check
						uncompletedChildren = job.provideUncompletedChildren()
								.collect(Collectors.toSet());
					}
					if (uncompletedChildren.size() > 0
							&& completionEvents.isEmpty()) {
						int totalCount = job.getFromRelations().size();
						int completedCount = totalCount
								- uncompletedChildren.size();
						double percentComplete = ((double) completedCount)
								/ totalCount * 100.0;
						long now = System.currentTimeMillis();
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
					long now = System.currentTimeMillis();
					if (completionEvents.size() > 0) {
						event = completionEvents.removeFirst();
						lastCompletionEventTime = now;
					} else {
						if (now - lastCompletionEventTime > AWAIT_CHILDREN_TIMEOUT_MS) {
							// FIXME - mvcc.jobs - cancel children? A policy?
							logger.warn(
									"Cancelling {} - timed out awaiting child completion");
							job.cancel();
							return;
						}
					}
				}
				if (statusMessage != null) {
					JobContext.get().setStatusMessage(statusMessage);
					Transaction.commit();
				}
				if (event != null) {
					uncompletedChildren.removeAll(event.related);
				}
			} finally {
				DomainDescriptorJob.get().relatedJobCompletionChanged
						.remove(relatedJobCompletionNotifier);
			}
		}
	}

	public void end() {
		Preconditions.checkState(
				persistenceType != PersistenceType.SchedulingSubJobs);
		if (noHttpContext) {
			InternalMetrics.get().endTracker(performer);
		}
		if (job.provideIsNotComplete()) {
			String log = Registry.impl(PerThreadLogging.class).endBuffer();
			job.setLog(log);
			if (!job.provideIsComplete()) {
				job.setState(JobState.COMPLETED);
			}
			job.setEndTime(new Date());
			job.setResultType(JobResultType.OK);
			persistMetadata(true);
		}
		if (persistenceType == PersistenceType.None) {
			// don't end transaction (job metadata changes needed for queue
			// exit)
		} else {
			Transaction.endAndBeginNew();
		}
		SequenceCompletionLatch sequenceCompletionLatch = JobRegistry
				.get().completionLatches.remove(job);
		if (sequenceCompletionLatch != null) {
			logger.warn("Fired child completion to sequence latch: {}", job);
			sequenceCompletionLatch.context = this;
			sequenceCompletionLatch.onChildJobsCompleted();
		}
	}

	public Job getJob() {
		return this.job;
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

	public boolean isSchedulingSubTasks() {
		return this.schedulingSubTasks;
	}

	public void jobOk(String resultMessage) {
		job.setResultMessage(resultMessage);
		job.setStatusMessage(resultMessage);
		job.setResultType(JobResultType.OK);
	}

	public void onJobException(Exception e) {
		job.setResultType(JobResultType.EXCEPTION);
		String simpleExceptionMessage = CommonUtils.toSimpleExceptionMessage(e);
		job.setStatusMessage(simpleExceptionMessage);
		job.setResultMessage(simpleExceptionMessage);
		logger.warn("Unexpected job exception", e);
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

	protected void checkCancelled0() {
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
		while (true) {
			if (job.provideUncompletedSequential().isEmpty()) {
				return;
			}
			try {
				DomainDescriptorJob.get().relatedJobCompletionChanged
						.add(relatedJobCompletionNotifier);
				RelatedJobCompletion event = null;
				synchronized (completionEvents) {
					if (completionEvents.isEmpty()) {
						/*
						 * double-checked
						 */
						Set<Job> uncompletedSequential = job
								.provideUncompletedSequential();
						if (uncompletedSequential.size() > 0) {
							Transaction.ensureEnded();
							try {
								// FIXME - mvcc.jobs - timeout is for debugging
								completionEvents.wait(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (SEUtilities
									.getFullStacktrace(Thread.currentThread())
									.contains("ControlServlet.handle1")) {
								int debug = 3;
							}
							Transaction.begin();
						}
					}
					if (completionEvents.size() > 0) {
						event = completionEvents.removeFirst();
					}
				}
			} finally {
				DomainDescriptorJob.get().relatedJobCompletionChanged
						.remove(relatedJobCompletionNotifier);
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
		if (job.provideIsNotComplete()) {
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

	enum PersistenceType {
		None, Immediate, Queued, SchedulingSubJobs
	}
}

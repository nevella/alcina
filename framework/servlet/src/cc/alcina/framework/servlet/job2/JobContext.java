package cc.alcina.framework.servlet.job2;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.servlet.job2.JobRegistry.ActionPerformerTrackMetrics;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

public class JobContext {
	static final String CONTEXT_CURRENT = JobContext.class.getName()
			+ ".CONTEXT_CURRENT";

	public static JobContext current() {
		return LooseContext.get(CONTEXT_CURRENT);
	}

	private Job job;

	private TaskPerformer performer;

	private Logger logger;

	private boolean noHttpContext;

	private PersistenceType persistenceType;

	private int subtaskCount;

	private int subtasksCompleted;

	public JobContext(Job job, PersistenceType persistenceType,
			TaskPerformer performer) {
		this.job = job;
		this.persistenceType = persistenceType;
		this.performer = performer;
		this.logger = LoggerFactory.getLogger(performer.getClass());
		noHttpContext = AlcinaServletContext.httpContext() == null;
	}

	public void end() {
		if (noHttpContext) {
			InternalMetrics.get().endTracker(performer);
		}
		persistMetadata(true);
		Transaction.endAndBeginNew();
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

	public void onJobException(Exception e) {
		job.setResultType(JobResultType.EXCEPTION);
		logger.warn("Unexpected job exception", e);
	}

	public void setSubtaskCount(int subtaskCount) {
		this.subtaskCount = subtaskCount;
	}

	public void setSubtasksCompleted(int subtasksCompleted) {
		this.subtasksCompleted = subtasksCompleted;
	}

	protected void persistMetadata(boolean respectImmediate) {
		switch (persistenceType) {
		case Immediate: {
			if (respectImmediate) {
				// commit asap
				Transaction.commit();
				synchronized (job) {
					job.notify();
				}
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
		case None:
			TransformCommit.removeTransforms(JobRegistry.TRANSFORM_QUEUE_NAME,
					Job.class, JobRelation.class);
			break;
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
		job.setStart(new Date());
		job.setState(JobState.PROCESSING);
		persistMetadata(true);
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
		None, Immediate, Queued
	}
}

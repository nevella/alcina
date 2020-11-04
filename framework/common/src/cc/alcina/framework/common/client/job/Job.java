package cc.alcina.framework.common.client.job;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.JobTrackerImpl;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadOracle;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = Job.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class Job extends Entity<Job> implements HasIUser {
	private Task task;

	private String taskSerialized;

	private String taskClassName;

	private Date runAt;

	private Date start;

	private Date finish;

	private JobState state;

	private String resultMessage;

	private String statusMessage;

	private String log;

	private double completion;

	private JobResultType resultType;

	private String key;

	private String queue;

	private boolean stacktraceRequested;

	private int retryCount;

	private String stacktraceResponse;

	private transient Object producedObject;

	private boolean clustered;

	private int performerVersionNumber;

	// FIXME - mvcc.jobs - get rid'o'me
	public JobResult asJobResult() {
		JobResult result = new JobResult() {
			@Override
			public String getActionLog() {
				return getLog();
			}

			@Override
			// FIXME - mvcc.jobs - get rid'o'me
			public ActionLogItem getActionLogItem() {
				ActionLogItem logItem = Reflections
						.newInstance(AlcinaPersistentEntityImpl
								.getImplementation(ActionLogItem.class));
				logItem.setActionClass((Class) getTask().getClass());
				logItem.setActionClassName(getTaskClassName());
				logItem.setActionDate(getFinish());
				logItem.setActionLog(getLog());
				logItem.setShortDescription(getResultMessage());
				return logItem;
			}
		};
		result.setProducedObject(producedObject);
		return result;
	}

	public JobTracker asJobTracker() {
		JobTrackerImpl tracker = new JobTrackerImpl();
		tracker.setCancelled(state == JobState.CANCELLED);
		tracker.setComplete(state.isComplete());
		tracker.setEndTime(finish);
		tracker.setId(key);
		tracker.setJobName(getTask().getName());
		tracker.setJobResult(resultMessage);
		tracker.setJobResultType(getResultType());
		tracker.setLog(log);
		tracker.setProgressMessage(statusMessage);
		tracker.setStartTime(start);
		return tracker;
	}

	public void cancel() {
		if (!getState().isComplete()) {
			setState(JobState.CANCELLED);
		}
	}

	public double getCompletion() {
		return this.completion;
	}

	public Date getFinish() {
		return this.finish;
	}

	@Transient
	public abstract Set<? extends JobRelation> getFromRelations();

	@Column(name = "key_")
	public String getKey() {
		return this.key;
	}

	@Lob
	@Transient
	public String getLog() {
		return this.log;
	}

	@Transient
	public abstract ClientInstance getPerformer();

	public int getPerformerVersionNumber() {
		return this.performerVersionNumber;
	}

	@Transient
	public Object getProducedObject() {
		return this.producedObject;
	}

	public String getQueue() {
		return this.queue;
	}

	public String getResultMessage() {
		return this.resultMessage;
	}

	public JobResultType getResultType() {
		return this.resultType;
	}

	public int getRetryCount() {
		return this.retryCount;
	}

	public Date getRunAt() {
		return this.runAt;
	}

	@Lob
	@Transient
	public String getStacktraceResponse() {
		return this.stacktraceResponse;
	}

	public Date getStart() {
		return this.start;
	}

	public JobState getState() {
		return this.state;
	}

	public String getStatusMessage() {
		return this.statusMessage;
	}

	@Transient
	@DomainProperty(serialize = true)
	public Task getTask() {
		task = TransformManager.resolveMaybeDeserialize(task,
				this.taskSerialized, null);
		return this.task;
	}

	public String getTaskClassName() {
		return this.taskClassName;
	}

	@Lob
	@DomainStoreProperty(loadType = DomainStorePropertyLoadType.LAZY)
	@Transient
	public String getTaskSerialized() {
		return this.taskSerialized;
	}

	@Transient
	public abstract Set<? extends JobRelation> getToRelations();

	public boolean isClustered() {
		return this.clustered;
	}

	public boolean isStacktraceRequested() {
		return this.stacktraceRequested;
	}

	public boolean provideIsActive() {
		return state == JobState.PROCESSING;
	}

	public boolean provideIsComplete() {
		return state.isComplete();
	}

	public boolean provideIsPending() {
		return state == JobState.PENDING;
	}

	public String provideName() {
		return getTask().getName();
	}

	public Optional<Job> provideParent() {
		return getToRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.parent_child)
				.findFirst().map(JobRelation::getFrom);
	}

	public void setClustered(boolean clustered) {
		boolean old_clustered = this.clustered;
		this.clustered = clustered;
		propertyChangeSupport().firePropertyChange("clustered", old_clustered,
				clustered);
	}

	public void setCompletion(double completion) {
		double old_completion = this.completion;
		this.completion = completion;
		propertyChangeSupport().firePropertyChange("completion", old_completion,
				completion);
	}

	public void setFinish(Date finish) {
		Date old_finish = this.finish;
		this.finish = finish;
		propertyChangeSupport().firePropertyChange("finish", old_finish,
				finish);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setKey(String key) {
		String old_key = this.key;
		this.key = key;
		propertyChangeSupport().firePropertyChange("key", old_key, key);
	}

	public void setLog(String log) {
		String old_log = this.log;
		this.log = log;
		propertyChangeSupport().firePropertyChange("log", old_log, log);
	}

	public abstract void setPerformer(ClientInstance performer);

	public void setPerformerVersionNumber(int performerVersionNumber) {
		int old_performerVersionNumber = this.performerVersionNumber;
		this.performerVersionNumber = performerVersionNumber;
		propertyChangeSupport().firePropertyChange("performerVersionNumber",
				old_performerVersionNumber, performerVersionNumber);
	}

	public void setProducedObject(Object producedObject) {
		this.producedObject = producedObject;
	}

	public void setQueue(String queue) {
		String old_queue = this.queue;
		this.queue = queue;
		propertyChangeSupport().firePropertyChange("queue", old_queue, queue);
	}

	public void setResultMessage(String resultMessage) {
		String old_resultMessage = this.resultMessage;
		this.resultMessage = resultMessage;
		propertyChangeSupport().firePropertyChange("resultMessage",
				old_resultMessage, resultMessage);
	}

	public void setResultType(JobResultType resultType) {
		JobResultType old_resultType = this.resultType;
		this.resultType = resultType;
		propertyChangeSupport().firePropertyChange("resultType", old_resultType,
				resultType);
	}

	public void setRetryCount(int retryCount) {
		int old_retryCount = this.retryCount;
		this.retryCount = retryCount;
		propertyChangeSupport().firePropertyChange("retryCount", old_retryCount,
				retryCount);
	}

	public void setRunAt(Date runAt) {
		Date old_runAt = this.runAt;
		this.runAt = runAt;
		propertyChangeSupport().firePropertyChange("runAt", old_runAt, runAt);
	}

	public void setStacktraceRequested(boolean stacktraceRequested) {
		boolean old_stacktraceRequested = this.stacktraceRequested;
		this.stacktraceRequested = stacktraceRequested;
		propertyChangeSupport().firePropertyChange("stacktraceRequested",
				old_stacktraceRequested, stacktraceRequested);
	}

	public void setStacktraceResponse(String stacktraceResponse) {
		String old_stacktraceResponse = this.stacktraceResponse;
		this.stacktraceResponse = stacktraceResponse;
		propertyChangeSupport().firePropertyChange("stacktraceResponse",
				old_stacktraceResponse, stacktraceResponse);
	}

	public void setStart(Date start) {
		Date old_start = this.start;
		this.start = start;
		propertyChangeSupport().firePropertyChange("start", old_start, start);
	}

	public void setState(JobState state) {
		JobState old_state = this.state;
		this.state = state;
		propertyChangeSupport().firePropertyChange("state", old_state, state);
	}

	public void setStatusMessage(String statusMessage) {
		String old_statusMessage = this.statusMessage;
		this.statusMessage = statusMessage;
		propertyChangeSupport().firePropertyChange("statusMessage",
				old_statusMessage, statusMessage);
	}

	public void setTask(Task task) {
		Task old_task = this.task;
		this.task = task;
		propertyChangeSupport().firePropertyChange("task", old_task, task);
	}

	public void setTaskClassName(String taskClassName) {
		String old_taskClassName = this.taskClassName;
		this.taskClassName = taskClassName;
		propertyChangeSupport().firePropertyChange("taskClassName",
				old_taskClassName, taskClassName);
	}

	public void setTaskSerialized(String taskSerialized) {
		String old_taskSerialized = this.taskSerialized;
		this.taskSerialized = taskSerialized;
		propertyChangeSupport().firePropertyChange("taskSerialized",
				old_taskSerialized, taskSerialized);
	}

	@Override
	public String toString() {
		return Ax.format("%s - %s - %s", toLocator(), provideName(), state);
	}

	public static abstract class ClientInstanceLoadOracle
			extends DomainStorePropertyLoadOracle<Job> {
	}
}

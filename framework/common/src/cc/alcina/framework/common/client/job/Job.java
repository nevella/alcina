package cc.alcina.framework.common.client.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.rpc.GwtTransient;

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
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
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
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = Job.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class Job extends VersionableEntity<Job> implements HasIUser {
	public static final transient String PROPERTY_STATE = "state";

	public static Job byId(long id) {
		return AlcinaPersistentEntityImpl.find(Job.class, id);
	}

	private Task task;

	private String taskSerialized;

	private String taskClassName;

	private Date runAt;

	private Date startTime;

	private Date endTime;

	private JobState state;

	private String resultMessage;

	private String statusMessage;

	private String log;

	private double completion;

	private JobResultType resultType;

	private String queue;

	private boolean stacktraceRequested;

	private int retryCount;

	private String stacktraceResponse;

	private boolean clustered;

	private int performerVersionNumber;

	@GwtTransient
	private Object result;

	@GwtTransient
	private String resultSerialized;

	@GwtTransient
	private Object largeResult;

	@GwtTransient
	private String largeResultSerialized;

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
				logItem.setActionDate(getEndTime());
				logItem.setActionLog(
						CommonUtils.trimToWsChars(getLog(), 200000, true));
				logItem.setShortDescription(getResultMessage());
				return logItem;
			}
		};
		result.setProducedObject(getResult());
		return result;
	}

	public JobTracker asJobTracker() {
		JobTrackerImpl tracker = new JobTrackerImpl();
		tracker.setCancelled(resolveState() == JobState.CANCELLED);
		tracker.setComplete(resolveState().isComplete());
		tracker.setEndTime(endTime);
		tracker.setId(toLocator().toString());
		tracker.setJobName(getTask().getName());
		tracker.setJobResult(resultMessage);
		tracker.setJobResultType(getResultType());
		tracker.setLog(log);
		tracker.setProgressMessage(statusMessage);
		tracker.setStartTime(startTime);
		return tracker;
	}

	public void cancel() {
		if (!resolveState().isComplete()) {
			setState(JobState.CANCELLED);
			setEndTime(new Date());
			// no need to set child state - completed states propagate down
		}
	}

	public void createRelation(Job to, JobRelationType type) {
		String invalidMessage = null;
		Preconditions.checkArgument(to != domainIdentity());
		switch (type) {
		case parent_child:
			if (to.getToRelations().stream().anyMatch(
					rel -> rel.getType() == JobRelationType.parent_child)) {
				invalidMessage = "Existing parent";
			}
			if (to.getRunAt() != null) {
				invalidMessage = "Child with runAt";
			}
			break;
		case retry:
			if (to.getToRelations().stream()
					.anyMatch(rel -> rel.getType() == JobRelationType.retry)) {
				invalidMessage = "Existing retry";
			}
			break;
		case sequence:
			if (to.getToRelations().stream().anyMatch(
					rel -> rel.getType() == JobRelationType.sequence)) {
				invalidMessage = "Existing sequence";
			}
			if (to.getRunAt() != null) {
				invalidMessage = "Sequential with runAt";
			}
			break;
		}
		if (invalidMessage != null) {
			throw new IllegalStateException(invalidMessage);
		}
		JobRelation relation = AlcinaPersistentEntityImpl
				.create(JobRelation.class);
		relation.setType(type);
		relation.setFrom(domainIdentity());
		relation.setTo(to);
	}

	/*
	 * Creates a 'sequence' relation - execute the 'to' job after this (or the
	 * end of its 'sequence' relation change)
	 */
	public void followWith(Job to) {
		List<? extends JobRelation> fromRelations = getFromRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.sequence)
				.collect(Collectors.toList());
		Preconditions.checkArgument(fromRelations.size() <= 1);
		if (fromRelations.isEmpty()) {
			createRelation(to, JobRelationType.sequence);
			Optional<Job> parent = provideParent();
			if (parent.isPresent() && !to.provideParent().isPresent()) {
				parent.get().createRelation(to, JobRelationType.parent_child);
			}
		} else {
			Job sequenceTo = fromRelations.get(0).getTo();
			// see JobRegistry.createJob - this relation may have been created
			// automagically
			if (sequenceTo == to) {
			} else {
				sequenceTo.followWith(to);
			}
		}
	}

	public double getCompletion() {
		return this.completion;
	}

	@Transient
	public abstract ClientInstance getCreator();

	public Date getEndTime() {
		return this.endTime;
	}

	@Transient
	public abstract Set<? extends JobRelation> getFromRelations();

	@Transient
	@DomainProperty(serialize = true)
	public Object getLargeResult() {
		largeResult = TransformManager.resolveMaybeDeserialize(result,
				this.largeResultSerialized, null);
		return this.largeResult;
	}

	@Lob
	@Transient
	@DomainStoreProperty(loadType = DomainStorePropertyLoadType.LAZY)
	public String getLargeResultSerialized() {
		return this.largeResultSerialized;
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

	public String getQueue() {
		return this.queue;
	}

	@Transient
	@DomainProperty(serialize = true)
	public Object getResult() {
		result = TransformManager.resolveMaybeDeserialize(result,
				this.resultSerialized, null);
		return this.result;
	}

	public String getResultMessage() {
		return this.resultMessage;
	}

	@Transient
	@Lob
	public String getResultSerialized() {
		return this.resultSerialized;
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

	public Date getStartTime() {
		return this.startTime;
	}

	/*
	 * Normally use resolveState - some completed states inherit from the parent
	 */
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

	public Stream<Job> provideAntecedents() {
		Job cursor = domainIdentity();
		List<Job> previousSiblings = new ArrayList<>();
		while (true) {
			Optional<? extends JobRelation> relation = cursor.getToRelations()
					.stream()
					.filter(rel -> rel.getType() == JobRelationType.sequence)
					.findFirst();
			if (relation.isPresent()) {
				cursor = relation.get().getFrom();
				previousSiblings.add(cursor);
			} else {
				break;
			}
		}
		Optional<Job> parent = provideParent();
		return Stream.concat(previousSiblings.stream(),
				parent.map(Stream::of).orElse(Stream.empty()));
	}

	public boolean provideCanBePerformedBy(ClientInstance clientInstance) {
		return isClustered() || Objects.equals(getCreator(), clientInstance);
	}

	public boolean provideCanDeserializeTask() {
		try {
			getTask();
			return true;
		} catch (Exception e) {
			// Invalid class/serialized form
			return false;
		}
	}

	public Stream<Job> provideChildren() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		return getFromRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.parent_child)
				.map(JobRelation::getTo);
	}

	public Date provideCreationDateOrNow() {
		return getCreationDate() == null ? new Date() : getCreationDate();
	}

	public Stream<Job> provideDescendants() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		return Stream.concat(provideChildren(),
				provideChildren().flatMap(Job::provideDescendants));
	}

	public Job provideFirstInSequence() {
		Job cursor = domainIdentity();
		while (true) {
			Optional<? extends JobRelation> relation = cursor.getToRelations()
					.stream()
					.filter(rel -> rel.getType() == JobRelationType.sequence)
					.findFirst();
			if (relation.isPresent()) {
				cursor = relation.get().getFrom();
			} else {
				break;
			}
		}
		return cursor;
	}

	public boolean provideHasIncompleteSubsequent() {
		return getFromRelations().stream()
				.filter(rel -> rel.getTo().provideIsNotComplete())
				.anyMatch(rel -> rel.getType() == JobRelationType.sequence);
	}

	public boolean provideHasPerformer() {
		return getPerformer() != null;
	}

	public boolean provideIsActive() {
		return resolveState() == JobState.PROCESSING;
	}

	public boolean provideIsAllocatable() {
		// FIXME - mvcc.jobs.1a - there still seem to be issues with state
		// propagation in JobDescriptor
		if (provideIsComplete()) {
			return false;
		}
		if (getRunAt() != null && getRunAt().after(new Date())) {
			return false;
		}
		if (getToRelations().stream()
				.anyMatch(rel -> rel.getType() == JobRelationType.sequence
						&& !rel.getFrom().provideIsComplete())) {
			return false;
		}
		return true;
	}

	public boolean provideIsComplete() {
		JobState resolvedState = resolveState();
		return resolvedState == null ? false : resolvedState.isComplete();
	}

	public boolean provideIsInCompletedQueue() {
		Optional<Job> parent = provideFirstInSequence().provideParent();
		if (parent.isPresent() && parent.get().provideIsNotComplete()) {
			return false;
		}
		return provideIsComplete();
	}

	public boolean provideIsLastInSequence() {
		return getFromRelations().stream()
				.noneMatch(rel -> rel.getType() == JobRelationType.sequence);
	}

	public boolean provideIsNotComplete() {
		return !resolveState().isComplete();
	}

	public boolean provideIsPending() {
		return resolveState() == JobState.PENDING;
	}

	public boolean provideIsSibling(Job job) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean provideIsTaskClass(Class<? extends Task> taskClass) {
		return Objects.equals(getTaskClassName(), taskClass.getName());
	}

	public String provideName() {
		try {
			return getTask().getName();
		} catch (Exception e) {
			Ax.simpleExceptionOut(e);
			return getTaskClassName();
		}
	}

	public Optional<Job> provideParent() {
		return getToRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.parent_child)
				.findFirst().map(JobRelation::getFrom);
	}

	public Job provideParentOrSelf() {
		Optional<Job> parent = provideParent();
		return parent.isPresent() ? parent.get() : domainIdentity();
	}

	public List<Job> provideRelatedSequential() {
		// backup to the start of the sequence, then traverse
		Job cursor = provideFirstInSequence();
		List<Job> result = new ArrayList<>();
		while (true) {
			result.add(cursor);
			Optional<? extends JobRelation> relation = cursor.getFromRelations()
					.stream()
					.filter(rel -> rel.getType() == JobRelationType.sequence
							&& rel.getTo() != null)
					.findFirst();
			if (relation.isPresent()) {
				cursor = relation.get().getTo();
			} else {
				break;
			}
		}
		return result;
	}

	public String provideShortName() {
		return provideName().replaceFirst(".+\\.", "");
	}

	public Stream<Job> provideUncompletedChildren() {
		return getFromRelations().stream()
				.filter(r -> r.getType() == JobRelationType.parent_child)
				.map(JobRelation::getTo).filter(Job::provideIsNotComplete);
	}

	public Set<Job> provideUncompletedSequential() {
		return provideRelatedSequential().stream()
				.filter(Job::provideIsNotComplete).collect(Collectors.toSet());
	}

	public Date resolveCompletionDate() {
		return resolveCompletionDate0(0);
	}

	public JobState resolveState() {
		return resolveState0(0);
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

	public abstract void setCreator(ClientInstance performer);

	public void setEndTime(Date endTime) {
		Date old_endTime = this.endTime;
		this.endTime = endTime;
		propertyChangeSupport().firePropertyChange("endTime", old_endTime,
				endTime);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setLargeResult(Object largeResult) {
		Object old_largeResult = this.largeResult;
		this.largeResult = largeResult;
		propertyChangeSupport().firePropertyChange("largeResult",
				old_largeResult, largeResult);
	}

	public void setLargeResultSerialized(String largeResultSerialized) {
		String old_largeResultSerialized = this.largeResultSerialized;
		this.largeResultSerialized = largeResultSerialized;
		propertyChangeSupport().firePropertyChange("largeResultSerialized",
				old_largeResultSerialized, largeResultSerialized);
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

	public void setQueue(String queue) {
		String old_queue = this.queue;
		this.queue = queue;
		propertyChangeSupport().firePropertyChange("queue", old_queue, queue);
	}

	public void setResult(Object result) {
		Object old_result = this.result;
		this.result = result;
		propertyChangeSupport().firePropertyChange("result", old_result,
				result);
	}

	public void setResultMessage(String resultMessage) {
		resultMessage = CommonUtils.trimToWsChars(resultMessage, 200, true);
		String old_resultMessage = this.resultMessage;
		this.resultMessage = resultMessage;
		propertyChangeSupport().firePropertyChange("resultMessage",
				old_resultMessage, resultMessage);
	}

	public void setResultSerialized(String resultSerialized) {
		String old_resultSerialized = this.resultSerialized;
		this.resultSerialized = resultSerialized;
		propertyChangeSupport().firePropertyChange("resultSerialized",
				old_resultSerialized, resultSerialized);
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
		Preconditions.checkState(runAt == null || !provideParent().isPresent());
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

	public void setStartTime(Date startTime) {
		Date old_startTime = this.startTime;
		this.startTime = startTime;
		propertyChangeSupport().firePropertyChange("startTime", old_startTime,
				startTime);
	}

	public void setState(JobState state) {
		JobState old_state = this.state;
		this.state = state;
		propertyChangeSupport().firePropertyChange("state", old_state, state);
	}

	public void setStatusMessage(String statusMessage) {
		statusMessage = CommonUtils.trimToWsChars(statusMessage, 200, true);
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
		return Ax.format("%s - %s - %s from: %s to: %s", toLocator(),
				provideName(), resolveState(), toString(getFromRelations()),
				toString(getToRelations()));
	}

	private Date resolveCompletionDate0(int depth) {
		if (depth > 10) {
			throw new RuntimeException("Invalid job depth - maybe a loop?");
		}
		if (this.state == null) {
			return null;
		}
		if (this.state.isComplete()) {
			return this.endTime;
		}
		Optional<Job> parent = provideParent();
		if (parent.isPresent()) {
			return parent.get().resolveCompletionDate0(depth + 1);
		}
		return null;
	}

	private JobState resolveState0(int depth) {
		if (depth > 10) {
			throw new RuntimeException("Invalid job depth - maybe a loop?");
		}
		if (this.state == null) {
			return null;
		}
		if (this.state.isComplete()) {
			return this.state;
		}
		Optional<Job> parent = provideParent();
		if (parent.isPresent()) {
			JobState parentState = parent.get().resolveState0(depth + 1);
			if (parentState != null && parentState.isComplete()) {
				return parentState;
			}
		}
		return this.state;
	}

	private String toString(Set<? extends JobRelation> relations) {
		String suffix = relations.size() > 4
				? Ax.format(" (%s)", relations.size())
				: "";
		return relations.stream().limit(4)
				.map(rel -> rel.toStringOther(domainIdentity()))
				.collect(Collectors.joining(", ")) + suffix;
	}

	public static abstract class ClientInstanceLoadOracle
			extends DomainStorePropertyLoadOracle<Job> {
	}

	public static class RunAtComparator implements Comparator<Job> {
		@Override
		public int compare(Job o1, Job o2) {
			Date runAt1 = o1.getRunAt();
			Date runAt2 = o2.getRunAt();
			if (runAt1 == null && runAt2 != null) {
				return -1;
			}
			if (runAt1 != null && runAt2 == null) {
				return 1;
			}
			if (runAt1 != null && runAt2 != null) {
				int i = runAt1.compareTo(runAt2);
				if (i != 0) {
					return i;
				}
			}
			return EntityComparator.INSTANCE.compare(o1, o2);
		}
	}
}

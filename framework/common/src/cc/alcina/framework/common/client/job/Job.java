package cc.alcina.framework.common.client.job;

import java.beans.PropertyChangeListener;
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
import cc.alcina.framework.common.client.actions.JobResource;
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
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = Job.class)
@DomainTransformPropagation(PropagationType.PERSISTENT)
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

	private boolean stacktraceRequested;

	private int retryCount;

	private int performerVersionNumber;

	@GwtTransient
	private Object result;

	@GwtTransient
	private String resultSerialized;

	@GwtTransient
	private Object largeResult;

	@GwtTransient
	private String largeResultSerialized;

	private String processStateSerialized;

	private ProcessState processState;

	private transient String cachedDisplayName;

	public Job() {
	}

	public Job(long id) {
		setId(id);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(listener);
	}

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
		tracker.setPercentComplete(completion);
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

	public <T extends Task> T castTask() {
		return (T) getTask();
	}

	public void createRelation(Job to, JobRelationType type) {
		String invalidMessage = null;
		Preconditions.checkArgument(to != domainIdentity());
		if (type == JobRelationType.RESUBMIT) {
			if (to.provideToResubmitRelation().isPresent()) {
				invalidMessage = "to has existing incoming resubmit relation";
			}
		} else {
			if (to.provideToAntecedentRelation().isPresent()) {
				invalidMessage = "to has existing incoming antecedent relation";
			}
		}
		Job from = domainIdentity();
		if (type == JobRelationType.SEQUENCE) {
			Optional<? extends JobRelation> next = null;
			while ((next = from.getFromRelations().stream()
					.filter(r -> r.getType() == JobRelationType.SEQUENCE)
					.findFirst()).isPresent()) {
				from = next.get().getTo();
			}
		}
		if (type != JobRelationType.PARENT_CHILD && from.getFromRelations()
				.stream().anyMatch(r -> r.getType() == type)) {
			invalidMessage = Ax.format(
					"from has existing outgoing relation of type %s", type);
		}
		if (invalidMessage != null) {
			throw new IllegalStateException(invalidMessage);
		}
		JobRelation relation = AlcinaPersistentEntityImpl
				.create(JobRelation.class);
		relation.setType(type);
		relation.setFrom(from);
		relation.setTo(to);
	}

	public ProcessState ensureProcessState() {
		if (getProcessState() == null) {
			setProcessState(new ProcessState());
		}
		return getProcessState();
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
		largeResult = TransformManager.resolveMaybeDeserialize(largeResult,
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
	@DomainStoreProperty(loadType = DomainStorePropertyLoadType.LAZY)
	public String getLog() {
		return this.log;
	}

	@Transient
	public abstract ClientInstance getPerformer();

	public int getPerformerVersionNumber() {
		return this.performerVersionNumber;
	}

	@Transient
	@DomainProperty(serialize = true)
	public ProcessState getProcessState() {
		processState = TransformManager.resolveMaybeDeserialize(processState,
				this.processStateSerialized, null);
		return this.processState;
	}

	@Lob
	@Transient
	@DomainStoreProperty(loadType = DomainStorePropertyLoadType.LAZY)
	public String getProcessStateSerialized() {
		return this.processStateSerialized;
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

	public Date getStartTime() {
		return this.startTime;
	}

	/*
	 * Normally use resolveState - some completed states inherit from the parent
	 */
	public JobState getState() {
		return this.state;
	}

	@Transient
	public abstract Set<? extends JobStateMessage> getStateMessages();

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

	// not used, replaced by jobstatemessage - FIXME mvcc.jobs.2 - remove
	public boolean isStacktraceRequested() {
		return this.stacktraceRequested;
	}

	public void persistProcessState() {
		ProcessState state = ensureProcessState();
		setProcessStateSerialized(TransformManager.serialize(state));
	}

	public boolean provideCanDeserializeTask() {
		try {
			Objects.requireNonNull(getTask());
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
		/*
		 * requires the final filter for indexing during a deletion cycle
		 */
		return getFromRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.PARENT_CHILD)
				.map(JobRelation::getTo).filter(Objects::nonNull);
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

	public Stream<Job> provideDescendantsAndSubsequents() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		Stream s1 = Stream.concat(provideChildren(), provideChildren()
				.flatMap(Job::provideDescendantsAndSubsequents));
		Stream s2 = Stream.concat(provideSubsequents(), provideSubsequents()
				.flatMap(Job::provideDescendantsAndSubsequents));
		return Stream.concat(s1, s2);
	}

	public Optional<Exception> provideException() {
		return getResultType() == JobResultType.EXCEPTION
				? Optional.of(new Exception(getResultMessage()))
				: Optional.empty();
	}

	public Job provideFirstInSequence() {
		Job cursor = domainIdentity();
		while (true) {
			Optional<Job> previous = cursor.providePrevious();
			if (previous.isPresent()) {
				cursor = previous.get();
			} else {
				break;
			}
		}
		return cursor;
	}

	public boolean provideHasCompletePredecesorOrNone() {
		Optional<? extends JobRelation> predecessor = getToRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.SEQUENCE)
				.findFirst();
		return !predecessor.isPresent()
				|| predecessor.get().getFrom().provideIsComplete();
	}

	public boolean provideHasIncompleteSubsequent() {
		return getFromRelations().stream()
				.filter(rel -> rel.getTo().provideIsNotComplete())
				.anyMatch(rel -> rel.getType() == JobRelationType.SEQUENCE);
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
				.anyMatch(rel -> rel.getType() == JobRelationType.SEQUENCE
						&& !rel.getFrom().provideIsComplete())) {
			return false;
		}
		return true;
	}

	public boolean provideIsComplete() {
		JobState resolvedState = resolveState();
		return resolvedState == null ? false : resolvedState.isComplete();
	}

	public boolean provideIsFirstInSequence() {
		return provideFirstInSequence() == domainIdentity();
	}

	public boolean provideIsFuture() {
		return resolveState() == JobState.FUTURE;
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
				.noneMatch(rel -> rel.getType() == JobRelationType.SEQUENCE);
	}

	public boolean provideIsNotComplete() {
		return !resolveState().isComplete();
	}

	public boolean provideIsNotTopLevel() {
		return !provideIsTopLevel();
	}

	public boolean provideIsPending() {
		return resolveState() == JobState.PENDING;
	}

	public boolean provideIsSibling(Job job) {
		return provideRelatedSequential().stream().anyMatch(j -> j == job);
	}

	public boolean provideIsTaskClass(Class<? extends Task> taskClass) {
		return Objects.equals(getTaskClassName(), taskClass.getName());
	}

	public boolean provideIsTopLevel() {
		return !provideFirstInSequence().provideParent().isPresent();
	}

	public String provideName() {
		try {
			return getTask().getName();
		} catch (Exception e) {
			Ax.simpleExceptionOut(e);
			return getTaskClassName();
		}
	}

	public Optional<Job> provideNextInSequence() {
		if (getFromRelations().isEmpty()) {
			return Optional.empty();
		}
		return getFromRelations().stream()
				.filter(r -> r.getType() == JobRelationType.SEQUENCE)
				.map(JobRelation::getTo).findFirst();
	}

	/*
	 * Often will want to call provideFirstInSequence().provideParent()
	 */
	public Optional<Job> provideParent() {
		return provideToAntecedentRelation()
				.filter(rel -> rel.getType() == JobRelationType.PARENT_CHILD)
				.map(JobRelation::getFrom);
	}

	public Job provideParentOrSelf() {
		return provideParent().orElse(domainIdentity());
	}

	public Optional<Job> providePrevious() {
		return provideToAntecedentRelation()
				.filter(rel -> rel.getType() == JobRelationType.SEQUENCE)
				.map(JobRelation::getFrom);
	}

	public Job providePreviousOrSelfInSequence() {
		return providePrevious().orElse(domainIdentity());
	}

	public List<Job> provideRelatedSequential() {
		// backup to the start of the sequence, then traverse
		Job cursor = provideFirstInSequence();
		List<Job> result = new ArrayList<>();
		while (true) {
			result.add(cursor);
			Optional<Job> next = cursor.provideNextInSequence();
			if (next.isPresent()) {
				cursor = next.get();
			} else {
				break;
			}
		}
		return result;
	}

	public Job provideRelatedSubqueueOwner() {
		Job first = provideFirstInSequence();
		if (first != domainIdentity()) {
			return first;
		}
		return provideParentOrSelf();
	}

	/*
	 * self, previous in stream, and, if present, repeat from parent
	 */
	public Stream<Job> provideSelfAndAntecedents() {
		Job cursor = domainIdentity();
		List<Job> selfAndPreviousSiblings = new ArrayList<>();
		while (true) {
			selfAndPreviousSiblings.add(cursor);
			Optional<Job> previous = cursor.providePrevious();
			if (previous.isPresent()) {
				cursor = previous.get();
			} else {
				break;
			}
		}
		Optional<Job> parent = cursor.provideParent();
		return Stream.concat(selfAndPreviousSiblings.stream(), parent
				.map(Job::provideSelfAndAntecedents).orElse(Stream.empty()));
	}

	public String provideShortName() {
		return provideName().replaceFirst(".+\\.", "");
	}

	public Stream<Job> provideSubsequents() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		/*
		 * requires the final filter for indexing during a deletion cycle
		 */
		return getFromRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.SEQUENCE)
				.map(JobRelation::getTo).filter(Objects::nonNull);
	}

	public Class<? extends Task> provideTaskClass() {
		if (provideCanDeserializeTask()) {
			return getTask().getClass();
		} else {
			return null;
		}
	}

	public Stream<Job> provideUncompletedChildren() {
		return provideChildren().filter(Job::provideIsNotComplete);
	}

	public Set<Job> provideUncompletedSequential() {
		return provideRelatedSequential().stream()
				.filter(Job::provideIsNotComplete).collect(Collectors.toSet());
	}

	public Date resolveCompletionDate() {
		return resolveCompletionDate0(0);
	}

	public Date resolveCompletionDateOrLastModificationDate() {
		Date completionDate = resolveCompletionDate();
		if (completionDate != null) {
			return completionDate;
		}
		if (getLastModificationDate() != null) {
			return getLastModificationDate();
		}
		return getCreationDate();
	}

	public JobState resolveState() {
		return resolveState0(0);
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

	public void setProcessState(ProcessState processState) {
		ProcessState old_processState = this.processState;
		this.processState = processState;
		propertyChangeSupport().firePropertyChange("processState",
				old_processState, processState);
	}

	public void setProcessStateSerialized(String processStateSerialized) {
		String old_processStateSerialized = this.processStateSerialized;
		this.processStateSerialized = processStateSerialized;
		propertyChangeSupport().firePropertyChange("processStateSerialized",
				old_processStateSerialized, processStateSerialized);
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
		Preconditions.checkState(runAt == null
				|| !provideFirstInSequence().provideParent().isPresent());
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

	public String toDisplayName() {
		if (cachedDisplayName == null) {
			cachedDisplayName = toDisplayName0();
		}
		return cachedDisplayName;
	}

	@Override
	public String toString() {
		return toDisplayName() + " - " + resolveState();
	}

	public String toStringFull() {
		return Ax.format("%s - %s - %s from: %s to: %s", toLocator(),
				provideName(), resolveState(), toString(getFromRelations()),
				toString(getToRelations()));
	}

	private Optional<? extends JobRelation> provideToAntecedentRelation() {
		if (getToRelations().isEmpty()) {
			return Optional.empty();
		}
		return getToRelations().stream()
				.filter(rel -> rel.getType() != JobRelationType.RESUBMIT)
				.findFirst();
	}

	private Optional<? extends JobRelation> provideToResubmitRelation() {
		if (getToRelations().isEmpty()) {
			return Optional.empty();
		}
		return getToRelations().stream()
				.filter(rel -> rel.getType() == JobRelationType.RESUBMIT)
				.findFirst();
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
		Optional<Job> parent = provideFirstInSequence().provideParent();
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
		Optional<Job> parent = provideFirstInSequence().provideParent();
		if (parent.isPresent()) {
			JobState parentState = parent.get().resolveState0(depth + 1);
			if (parentState != null && parentState.isComplete()) {
				return parentState;
			}
		}
		return this.state;
	}

	private String toDisplayName0() {
		if (getTaskClassName() == null) {
			return Ax.format("%s - <null task>",
					toLocator().toRecoverableNumericString());
		}
		if (provideCanDeserializeTask()) {
			return Ax.format("%s::%s", task.getName(),
					toLocator().toRecoverableNumericString());
		} else {
			return Ax.format("%s::%s",
					getTaskClassName().replaceFirst(".+\\.(.+)", "$1"),
					toLocator().toRecoverableNumericString());
		}
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

	@Bean
	public static class ProcessState extends Model {
		private List<ResourceRecord> resources = new ArrayList<>();

		private String threadName;

		private String allocatorThreadName;

		private String stackTrace;

		private String trimmedStackTrace;

		public ResourceRecord addResourceRecord(JobResource resource) {
			ResourceRecord record = new ResourceRecord();
			record.className = resource.getClass().getName();
			record.path = resource.getPath();
			resources.add(record);
			return record;
		}

		public String getAllocatorThreadName() {
			return this.allocatorThreadName;
		}

		public List<ResourceRecord> getResources() {
			return this.resources;
		}

		public String getStackTrace() {
			return this.stackTrace;
		}

		public String getThreadName() {
			return this.threadName;
		}

		public String getTrimmedStackTrace() {
			return this.trimmedStackTrace;
		}

		public ResourceRecord provideRecord(ResourceRecord record) {
			return HasEquivalenceHelper.getEquivalent(getResources(), record);
		}

		public void setAllocatorThreadName(String allocatorThreadName) {
			this.allocatorThreadName = allocatorThreadName;
		}

		public void setResources(List<ResourceRecord> resources) {
			this.resources = resources;
		}

		public void setStackTrace(String stackTrace) {
			this.stackTrace = stackTrace;
		}

		public void setThreadName(String threadName) {
			this.threadName = threadName;
		}

		public void setTrimmedStackTrace(String trimmedStackTrace) {
			this.trimmedStackTrace = trimmedStackTrace;
		}
	}

	@Bean
	public static class ResourceRecord extends Model
			implements HasEquivalenceString<ResourceRecord> {
		private boolean acquiredFromAncestor;

		private boolean acquired;

		private String className;

		private String path;

		@Override
		public String equivalenceString() {
			return Ax.format("%s::%s", getClassName(), getPath());
		}

		public String getClassName() {
			return this.className;
		}

		public String getPath() {
			return this.path;
		}

		public boolean isAcquired() {
			return this.acquired;
		}

		public boolean isAcquiredFromAncestor() {
			return this.acquiredFromAncestor;
		}

		public void setAcquired(boolean acquired) {
			this.acquired = acquired;
		}

		public void setAcquiredFromAncestor(boolean acquiredFromAncestor) {
			this.acquiredFromAncestor = acquiredFromAncestor;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s - Acquired: %s - Ancestor: %s",
					getClassName().replaceFirst("(.+)(\\..+)", "$2"), getPath(),
					isAcquired(), isAcquiredFromAncestor());
		}
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

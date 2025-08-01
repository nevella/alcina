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

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadOracle;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;

@MappedSuperclass
@ObjectPermissions(
	create = @Permission(access = AccessLevel.ADMIN),
	read = @Permission(access = AccessLevel.ADMIN),
	write = @Permission(access = AccessLevel.ADMIN),
	delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
@Registration({ PersistentImpl.class, Job.class })
@TypedProperties
public abstract class Job extends VersionableEntity<Job>
		implements HasIUser, Comparable<Job> {
	public static transient PackageProperties._Job properties = PackageProperties.job;

	public static final transient String CONTEXT_DO_NOT_POPULATE_DURING_TRACKER_CREATION = Job.class
			.getName() + ".CONTEXT_DO_NOT_POPULATE_DURING_TRACKER_CREATION";

	private static final transient String CONSISTENCY_PRIORITY_DEFAULT = "_default";

	public static final transient String PROPERTY_STATE = "state";

	public static transient boolean throwOnDeserializationException = false;

	public static Job byId(long id) {
		return PersistentImpl.find(Job.class, id);
	}

	@GwtTransient
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

	@GwtTransient
	private volatile ProcessState processState;

	private transient String cachedDisplayName;

	@GwtTransient
	private String taskSignature;

	// cached value for sequence computation
	private transient Job firstInSequence;

	// cached value for sequence computation
	private transient Job previousForComputeFirst;

	@GwtTransient
	private String consistencyPriority;

	/**
	 * creatorInstanceId.creationLocalId
	 */
	@GwtTransient
	private String uuid;

	@GwtTransient
	private String cause;

	public Job() {
	}

	public Job(long id) {
		setId(id);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(listener);
	}

	public JobTracker asJobTracker() {
		JobTracker tracker = new JobTracker();
		tracker.setCancelled(resolveState() == JobState.CANCELLED);
		tracker.setComplete(resolveState().isSequenceComplete());
		tracker.setEndTime(endTime);
		tracker.setId(String.valueOf(getId()));
		tracker.setJobName(getTask().getName());
		tracker.setJobResult(resultMessage);
		tracker.setJobResultType(getResultType());
		tracker.setLog(log);
		tracker.setPercentComplete(completion);
		tracker.setProgressMessage(statusMessage);
		tracker.setStartTime(startTime);
		// compute leafProgress
		long completedTreeCount = provideDescendantsAndSubsequentsAndAwaited()
				.filter(Job::provideIsComplete).count();
		long treeCount = provideDescendantsAndSubsequentsAndAwaited().count();
		tracker.setLeafCount(Ax.format("%s/%s", completedTreeCount, treeCount));
		if (tracker.isComplete()) {
			if (!LooseContext
					.is(CONTEXT_DO_NOT_POPULATE_DURING_TRACKER_CREATION)) {
				domain().ensurePopulated();
			}
			tracker.setSerializedResult(getResultSerialized());
		}
		tracker.setResubmitId(EntityHelper.getIdOrZero(provideResubmittedTo()));
		return tracker;
	}

	public void cancel() {
		if (!resolveState().isComplete()) {
			setState(JobState.CANCELLED);
			setEndTime(new Date());
			setResultType(JobResultType.DID_NOT_COMPLETE);
			// no need to set child state - completed states propagate down
		}
	}

	public <T extends Task> T castTask() {
		return (T) getTask();
	}

	@Override
	public int compareTo(Job o) {
		return EntityComparator.INSTANCE.compare(domainIdentity(), o);
	}

	public void createRelation(Job to, JobRelationType type) {
		String invalidMessage = null;
		Preconditions.checkArgument(to != domainIdentity());
		if (type.isSequential()) {
			if (to.provideToAntecedentRelation().isPresent()) {
				invalidMessage = Ax.format(
						"to has existing incoming antecedent relation: %s",
						to.provideToAntecedentRelation().get());
			}
		} else {
			if (to.provideToRelation(type).isPresent()) {
				invalidMessage = Ax
						.format("to has existing incoming %s relation", type);
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
		if (type != JobRelationType.PARENT_CHILD
				&& type != JobRelationType.AWAITED && from.getFromRelations()
						.stream().anyMatch(r -> r.getType() == type)) {
			invalidMessage = Ax.format(
					"from has existing outgoing relation: %s",
					from.getFromRelations().stream()
							.filter(r -> r.getType() == type).findFirst()
							.get());
		}
		if (invalidMessage != null) {
			throw new IllegalStateException(invalidMessage);
		}
		JobRelation relation = PersistentImpl.create(JobRelation.class);
		relation.setType(type);
		relation.setFrom(from);
		relation.setTo(to);
	}

	// Delete while ensuring job sequencing is still correct after deletion
	public void deleteEnsuringSequence() {
		Optional<? extends JobRelation> previousRelation = getToRelations()
				.stream().filter(JobRelation::provideIsSequential).findFirst();
		Optional<? extends JobRelation> nextRelation = getFromRelations()
				.stream().filter(JobRelation::provideIsSequential).findFirst();
		if (previousRelation.isPresent() && nextRelation.isPresent()) {
			// If this job is in the middle of the chain,
			// re-link it so ensure the sequence stays connected
			Job previous = previousRelation.get().getFrom();
			Job next = nextRelation.get().getTo();
			// delete before creating new relation (required for validation)
			JobRelationType type = previousRelation.get().getType();
			delete();
			// Create new relation
			previous.createRelation(next, type);
		} else {
			delete();
		}
	}

	public String getCause() {
		return this.cause;
	}

	public double getCompletion() {
		return this.completion;
	}

	public String getConsistencyPriority() {
		return this.consistencyPriority;
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
	@AlcinaTransient
	@Directed.Exclude
	@Display.Exclude
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

	/**
	 * The serialized property is lazy, so only call after
	 * job.domain().ensurePopulated()
	 * 
	 * @return
	 */
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
	@AlcinaTransient
	@Directed.Exclude
	@Display.Exclude
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
	@Directed.Exclude
	@Display.Exclude
	public Task getTask() {
		task = TransformManager.resolveMaybeDeserialize(task,
				this.taskSerialized, null,
				Reflections.forName(getTaskClassName()));
		return this.task;
	}

	@Display(orderingHint = -1)
	@ValueTransformer(ClassNameStringNestedName.class)
	public String getTaskClassName() {
		return this.taskClassName;
	}

	@Reflected
	public static class ClassNameStringNestedName
			implements ModelTransform<String, String> {
		@Override
		public String apply(String t) {
			return t == null ? null : t.replaceFirst(".+\\.([A-Z_].+)", "$1");
		}
	}

	@Lob
	@Transient
	public String getTaskSerialized() {
		return this.taskSerialized;
	}

	public String getTaskSignature() {
		return this.taskSignature;
	}

	@Transient
	public abstract Set<? extends JobRelation> getToRelations();

	public String getUuid() {
		return this.uuid;
	}

	public boolean hasSelfOrAncestorTask(Class<? extends Task> taskClass) {
		if (provideIsTaskClass(taskClass)) {
			return true;
		}
		return provideParent().map(job -> job.hasSelfOrAncestorTask(taskClass))
				.orElse(false);
	}

	public boolean provideCanDeserializeTask() {
		try {
			Objects.requireNonNull(getTask());
			return true;
		} catch (Exception e) {
			if (throwOnDeserializationException || Objects.equals(
					Permissions.get().getClientInstance(), getCreator())) {
				throw new RuntimeException(e);
			} else {
				// Invalid class/serialized form
				return false;
			}
		}
	}

	// this looks wrong but it's not - the 'toRelated' are the other endpoints
	// of fromRelations
	public Stream<Job> provideToRelated(JobRelationType type) {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		/*
		 * requires the final filter for indexing during a deletion cycle
		 */
		return getFromRelations().stream().filter(rel -> rel.getType() == type)
				.map(JobRelation::getTo).filter(Objects::nonNull);
	}

	// this looks wrong but it's not - the 'fromRelated' are the other endpoints
	// of toRelations
	private Stream<Job> provideFromRelated(JobRelationType type) {
		if (getToRelations().isEmpty()) {
			return Stream.empty();
		}
		/*
		 * requires the final filter for indexing during a deletion cycle
		 */
		return getToRelations().stream().filter(rel -> rel.getType() == type)
				.map(JobRelation::getFrom).filter(Objects::nonNull);
	}

	public Stream<Job> provideChildren() {
		return provideToRelated(JobRelationType.PARENT_CHILD);
	}

	public Stream<Job> provideChildrenAndChildSubsequents() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		return Stream.concat(provideChildren(), provideChildren()
				.flatMap(Job::provideDescendantsAndSubsequents));
	}

	public String provideConsistencyPriority() {
		return Optional.ofNullable(getConsistencyPriority())
				.orElse(Job.CONSISTENCY_PRIORITY_DEFAULT);
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

	public Stream<Job> provideDescendantsAndSubsequentsAndAwaited() {
		if (getFromRelations().isEmpty()) {
			return Stream.empty();
		}
		Stream s1 = Stream.concat(provideChildren(), provideChildren()
				.flatMap(Job::provideDescendantsAndSubsequentsAndAwaited));
		Stream s2 = Stream.concat(provideSubsequents(), provideSubsequents()
				.flatMap(Job::provideDescendantsAndSubsequentsAndAwaited));
		Stream s3 = provideAwaitedSubtree();
		return Stream.concat(s1, Stream.concat(s2, s3)).distinct();
	}

	public Stream<Job> provideAwaitedSubtree() {
		return Stream.concat(provideAwaiteds(), provideAwaiteds()
				.flatMap(Job::provideDescendantsAndSubsequentsAndAwaited));
	}

	public boolean provideEquivalentTask(Job other) {
		return CommonUtils.equals(getTaskClassName(), other.getTaskClassName(),
				getTaskSerialized(), other.getTaskSerialized());
	}

	public Optional<Exception> provideException() {
		return provideIsException()
				? Optional.of(new Exception(getResultMessage()))
				: Optional.empty();
	}

	public Job provideFirstInSequence() {
		Job previous = providePrevious().orElse(null);
		if (previous == null) {
			return domainIdentity();
		}
		/*
		 * The previous of the previous job will be invariant, so only change
		 * the cached value if previous changes (during Job creation)
		 */
		if (previous != this.previousForComputeFirst) {
			this.previousForComputeFirst = previous;
			this.firstInSequence = this.previousForComputeFirst
					.provideFirstInSequence();
		}
		return firstInSequence;
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

	public boolean provideIsProcessing() {
		return resolveState() == JobState.PROCESSING;
	}

	public boolean provideIsNotCompleteNonFuture() {
		if (provideIsComplete()) {
			return false;
		}
		if (provideIsFuture()) {
			return false;
		}
		if (provideIsFutureConsistency()) {
			return false;
		}
		return true;
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

	public boolean provideIsCompletedNormally() {
		JobState resolvedState = resolveState();
		return resolvedState == null ? false
				: resolvedState.isCompletedNormally();
	}

	public boolean provideIsCompleteWithEndTime() {
		return provideIsComplete() && getEndTime() != null;
	}

	public boolean provideIsConsistency() {
		return Ax.notBlank(consistencyPriority);
	}

	public boolean provideIsException() {
		return getResultType() == JobResultType.EXCEPTION;
	}

	public boolean provideIsFirstInSequence() {
		return provideFirstInSequence() == domainIdentity();
	}

	public boolean provideIsFuture() {
		return resolveState() == JobState.FUTURE;
	}

	public boolean provideIsFutureConsistency() {
		return resolveState() == JobState.FUTURE_CONSISTENCY;
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

	public boolean provideIsSequenceComplete() {
		return provideIsComplete() && (!provideHasIncompleteSubsequent()
				|| resolveState() == JobState.ABORTED);
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
			return Ax.blankTo(getTask().getName(),
					Ax.format("blank task name - %s", toStringId()));
		} catch (Exception e) {
			Ax.simpleExceptionOut(e);
			return Ax.blankTo(getTaskClassName(),
					Ax.format("blank task className - %s", toStringId()));
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
	 * self, previous in stream, and, if present, repeat from parent -or-
	 * awaiter
	 */
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
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
		Optional<Job> parentOrAwaiter = cursor.provideParentOrAwaiter();
		return Stream.concat(selfAndPreviousSiblings.stream(), parentOrAwaiter
				.map(Job::provideSelfAndAntecedents).orElse(Stream.empty()));
	}

	public Optional<Job> provideParentOrAwaiter() {
		Optional<? extends JobRelation> rel = provideToRelation(
				JobRelationType.PARENT_CHILD);
		if (rel.isEmpty()) {
			rel = provideToRelation(JobRelationType.AWAITED);
		}
		return rel.map(JobRelation::getFrom);
	}

	public String provideShortName() {
		return provideName().replaceFirst(".+\\.", "");
	}

	public Stream<Job> provideSubsequents() {
		return provideToRelated(JobRelationType.SEQUENCE);
	}

	public Stream<Job> provideAwaiteds() {
		return provideToRelated(JobRelationType.AWAITED);
	}

	public Optional<Job> provideAwaiting() {
		return provideFromRelated(JobRelationType.AWAITED).findFirst();
	}

	public Class<? extends Task> provideTaskClass() {
		if (provideCanDeserializeTask()) {
			return getTask().getClass();
		} else {
			return null;
		}
	}

	private Optional<? extends JobRelation> provideToAntecedentRelation() {
		Set<? extends JobRelation> toRelations = getToRelations();
		if (toRelations.isEmpty()) {
			return Optional.empty();
		}
		// return toRelations.stream()
		// .filter(rel -> rel.getType().isSequential()).findFirst();
		/*
		 * Non-stream optimisation.
		 */
		for (JobRelation rel : toRelations) {
			if (rel.getType().isSequential()) {
				return Optional.of(rel);
			}
		}
		return Optional.empty();
	}

	public Job provideTopLevelAncestor() {
		return provideIsTopLevel() ? domainIdentity()
				: provideParent().get().provideTopLevelAncestor();
	}

	private Optional<? extends JobRelation>
			provideToRelation(JobRelationType type) {
		if (getToRelations().isEmpty()) {
			return Optional.empty();
		}
		return getToRelations().stream().filter(rel -> rel.getType() == type)
				.findFirst();
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

	public Job root() {
		return provideFirstInSequence().provideParent().map(Job::root)
				.orElse(domainIdentity());
	}

	public Job rootAwaiter() {
		Job firstInSequence = provideFirstInSequence();
		Optional<Job> awaiting = firstInSequence.provideAwaiting();
		if (awaiting.isPresent()) {
			return awaiting.get().rootAwaiter();
		} else {
			return firstInSequence.provideParent().map(Job::rootAwaiter)
					.orElse(domainIdentity());
		}
	}

	public void setCause(String cause) {
		String old_cause = this.cause;
		this.cause = cause;
		propertyChangeSupport().firePropertyChange("cause", old_cause, cause);
	}

	public void setCompletion(double completion) {
		double old_completion = this.completion;
		this.completion = completion;
		propertyChangeSupport().firePropertyChange("completion", old_completion,
				completion);
	}

	public void setConsistencyPriority(String consistencyPriority) {
		String old_consistencyPriority = this.consistencyPriority;
		this.consistencyPriority = consistencyPriority;
		propertyChangeSupport().firePropertyChange("consistencyPriority",
				old_consistencyPriority, consistencyPriority);
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
		super.setId(id);
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

	public void setRunAt(Date runAt) {
		Preconditions.checkState(runAt == null
				|| !provideFirstInSequence().provideParent().isPresent());
		Date old_runAt = this.runAt;
		this.runAt = runAt;
		propertyChangeSupport().firePropertyChange("runAt", old_runAt, runAt);
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
		if (this.taskClassName != null && taskClassName == null &&
		// this is nulled for indexing
				!TransformManager.get().isIgnorePropertyChanges()) {
			throw new IllegalStateException(
					"job-domain-exception :: Clearing invariant - " + id);
		}
		String old_taskClassName = this.taskClassName;
		this.taskClassName = taskClassName;
		propertyChangeSupport().firePropertyChange("taskClassName",
				old_taskClassName, taskClassName);
		cachedDisplayName = null;
	}

	public void setTaskSerialized(String taskSerialized) {
		if (this.taskSerialized != null && taskSerialized == null &&
		// this is nulled for indexing
				!TransformManager.get().isIgnorePropertyChanges()) {
			throw new IllegalStateException(
					"job-domain-exception :: Clearing invariant - " + id);
		}
		String old_taskSerialized = this.taskSerialized;
		this.taskSerialized = taskSerialized;
		propertyChangeSupport().firePropertyChange("taskSerialized",
				old_taskSerialized, taskSerialized);
	}

	public void setTaskSignature(String taskSignature) {
		String old_taskSignature = this.taskSignature;
		this.taskSignature = taskSignature;
		propertyChangeSupport().firePropertyChange("taskSignature",
				old_taskSignature, taskSignature);
	}

	public void setUuid(String uuid) {
		String old_uuid = this.uuid;
		this.uuid = uuid;
		propertyChangeSupport().firePropertyChange("uuid", old_uuid, uuid);
	}

	public void throwIfException() {
		if (getResultType() == JobResultType.EXCEPTION) {
			throw new JobException(getLog());
		}
	}

	public String toDisplayName() {
		if (cachedDisplayName != null && id != 0
				&& cachedDisplayName.contains("/")) {
			// crumby invalidation to avoid yet another field (since id may
			cachedDisplayName = null;
		}
		if (cachedDisplayName == null) {
			try {
				cachedDisplayName = toDisplayName0();
			} catch (Throwable e) {
				e.printStackTrace();
				Ax.out("DEVEXZ - 0 - Exception generating displayName: %s : %s ",
						getId(), CommonUtils.toSimpleExceptionMessage(e));
				cachedDisplayName = Ax.format(
						"Exception generating job name - id: %s", getId());
				if (Al.isNonProduction()) {
					throw e;
				}
			}
		}
		return cachedDisplayName;
	}

	private String toDisplayName0() {
		if (getTaskClassName() == null) {
			return Ax.format("%s - <null task>",
					toLocator().toRecoverableNumericString());
		}
		if (provideCanDeserializeTask()) {
			// of interest
			return Ax.format("%s::%s::%s", task.getName(),
					toLocator().toRecoverableNumericString(), uuid);
		} else {
			return Ax.format("%s::%s",
					getTaskClassName().replaceFirst(".+\\.(.+)", "$1"),
					toLocator().toRecoverableNumericString());
		}
	}

	@Override
	public String toString() {
		return toDisplayName() + " - " + resolveState();
	}

	private String toString(Set<? extends JobRelation> relations) {
		String suffix = relations.size() > 4
				? Ax.format(" (%s)", relations.size())
				: "";
		return relations.stream().limit(4)
				.map(rel -> rel.toStringOther(domainIdentity()))
				.collect(Collectors.joining(", ")) + suffix;
	}

	public String toStringFull() {
		return Ax.format("%s - %s - %s from: %s to: %s", toLocator(),
				provideName(), resolveState(), toString(getFromRelations()),
				toString(getToRelations()));
	}

	public void writeLargeObject() {
		Registry.impl(DebugLogWriter.class).write(domainIdentity());
	}

	public static abstract class ClientInstanceLoadOracle
			extends DomainStorePropertyLoadOracle<Job> {
	}

	@Registration(DebugLogWriter.class)
	public static class DebugLogWriter {
		public void write(Job job) {
			throw new UnsupportedOperationException();
		}
	}

	public static class JobException extends RuntimeException {
		public JobException(String message) {
			super(message);
		}
	}

	public static class ProcessState extends Bindable
			implements TreeSerializable {
		private List<ResourceRecord> resources = new ArrayList<>();

		private String threadName;

		private String allocatorThreadName;

		private String stackTrace;

		private String trimmedStackTrace;

		public ResourceRecord addResourceRecord(JobResource resource) {
			ResourceRecord record = new ResourceRecord();
			record.setClassName(resource.getClass().getName());
			record.setPath(resource.getPath());
			resources.add(record);
			return record;
		}

		@Override
		public ProcessState clone() {
			return FlatTreeSerializer.clone(this);
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

	public static class ResourceRecord extends Bindable
			implements HasEquivalenceString<ResourceRecord>, TreeSerializable {
		private boolean acquiredFromAntecedent;

		private boolean acquired;

		private String className;

		private String path = "";

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

		public boolean isAcquiredFromAntecedent() {
			return this.acquiredFromAntecedent;
		}

		public void setAcquired(boolean acquired) {
			this.acquired = acquired;
		}

		public void setAcquiredFromAntecedent(boolean acquiredFromAntecedent) {
			this.acquiredFromAntecedent = acquiredFromAntecedent;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s - Acquired: %s - Antecedent: %s",
					getClassName().replaceFirst("(.+)\\.(.+)", "$2"), getPath(),
					isAcquired(), isAcquiredFromAntecedent());
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

	public Optional<Job> provideResubmittedFrom() {
		return getToRelations().stream()
				.filter(rel -> rel.is(JobRelationType.RESUBMIT)).findFirst()
				.map(JobRelation::getFrom);
	}

	public Optional<Job> provideResubmittedTo() {
		return getFromRelations().stream()
				.filter(rel -> rel.is(JobRelationType.RESUBMIT)).findFirst()
				.map(JobRelation::getTo);
	}

	public boolean provideCanAppendPending() {
		return state == JobState.ALLOCATED || state == JobState.PROCESSING;
	}

	public Job provideOriginatingJob() {
		Optional<Job> parentOrAwaiter = provideParentOrAwaiter();
		if (parentOrAwaiter.isPresent()) {
			return parentOrAwaiter.get().provideOriginatingJob();
		} else {
			return provideFirstInSequence();
		}
	}
}

package cc.alcina.framework.common.client.job;

import java.util.Date;
import java.util.Set;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = Job.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class Job<T extends Job> extends Entity<T> implements HasIUser {
	private Task taskDefinition;

	private String taskDefinitionSerialized;

	private Date start;

	private Date finish;

	private JobState state;

	private String resultMessage;

	private String statusMessage;

	private String log;

	private double completion;

	private JobResultType resultType;

	private String key;

	public double getCompletion() {
		return this.completion;
	}

	public Date getFinish() {
		return this.finish;
	}

	@Transient
	public abstract Set<? extends JobRelation> getFromRelations();

	public String getKey() {
		return this.key;
	}

	@Lob
	@Transient
	public String getLog() {
		return this.log;
	}

	public String getResultMessage() {
		return this.resultMessage;
	}

	public JobResultType getResultType() {
		return this.resultType;
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
	public Task getTaskDefinition() {
		taskDefinition = TransformManager.resolveMaybeDeserialize(
				taskDefinition, this.taskDefinitionSerialized, null);
		return this.taskDefinition;
	}

	@Lob
	@Type(type = "org.hibernate.type.StringClobType")
	@DomainStoreProperty(loadType = DomainStorePropertyLoadType.LAZY)
	public String getTaskDefinitionSerialized() {
		return this.taskDefinitionSerialized;
	}

	@Transient
	public abstract Set<? extends JobRelation> getToRelations();

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

	public void setTaskDefinition(Task taskDefinition) {
		Task old_taskDefinition = this.taskDefinition;
		this.taskDefinition = taskDefinition;
		propertyChangeSupport().firePropertyChange("taskDefinition",
				old_taskDefinition, taskDefinition);
	}

	public void setTaskDefinitionSerialized(String taskDefinitionSerialized) {
		String old_taskDefinitionSerialized = this.taskDefinitionSerialized;
		this.taskDefinitionSerialized = taskDefinitionSerialized;
		propertyChangeSupport().firePropertyChange("taskDefinitionSerialized",
				old_taskDefinitionSerialized, taskDefinitionSerialized);
	}
}

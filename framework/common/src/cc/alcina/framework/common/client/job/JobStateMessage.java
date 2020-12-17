package cc.alcina.framework.common.client.job;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.job.Job.ProcessState;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = JobStateMessage.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class JobStateMessage<T extends JobStateMessage>
		extends Entity<T> {
	private String processStateSerialized;

	private ProcessState processState;

	public ProcessState ensureProcessState() {
		if (getProcessState() == null) {
			setProcessState(new ProcessState());
		}
		return getProcessState();
	}

	@Transient
	public abstract Job getJob();

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

	public void persistProcessState() {
		ProcessState state = ensureProcessState();
		setProcessStateSerialized(TransformManager.serialize(state));
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public abstract void setJob(Job job);

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
}

package cc.alcina.framework.common.client.job;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = JobRelation.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
public abstract class JobRelation<T extends JobRelation> extends Entity<T> {
	private JobRelationType type = JobRelationType.parent_child;

	@Transient
	public abstract Job getFrom();

	@Transient
	public abstract Job getTo();

	public JobRelationType getType() {
		return this.type;
	}

	public abstract void setFrom(Job from);

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public abstract void setTo(Job to);

	public void setType(JobRelationType type) {
		JobRelationType old_type = this.type;
		this.type = type;
		propertyChangeSupport().firePropertyChange("type", old_type, type);
	}

	@ClientInstantiable
	public static enum JobRelationType {
		parent_child, cascade, retry
	}
}

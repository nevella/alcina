package cc.alcina.template.cs.persistent;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import cc.alcina.framework.common.client.csobjects.Entity;
import cc.alcina.framework.common.client.entity.VersioningEntityListener;



@MappedSuperclass
@javax.persistence.EntityListeners(VersioningEntityListener.class)
public abstract class DomainBase extends Entity {
}

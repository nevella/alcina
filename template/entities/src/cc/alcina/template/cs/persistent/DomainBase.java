package cc.alcina.template.cs.persistent;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.entity.VersioningEntityListener;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;

@AlcinaTemplate
@MappedSuperclass
@EntityListeners(VersioningEntityListener.class)
public abstract class DomainBase extends AbstractDomainBase {
}

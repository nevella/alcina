package cc.alcina.framework.entity.domaintransform.policy;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

//FIXME - mvcc.3 - difference between this and TransformPersistencePolicy?
public interface TransformLoggingPolicy extends Serializable {
	public abstract boolean shouldPersist(DomainTransformEvent dte);
}

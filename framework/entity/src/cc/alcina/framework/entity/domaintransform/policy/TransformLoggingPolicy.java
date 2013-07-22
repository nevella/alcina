package cc.alcina.framework.entity.domaintransform.policy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

public interface TransformLoggingPolicy {
	public abstract boolean shouldPersist(DomainTransformEvent dte);
}

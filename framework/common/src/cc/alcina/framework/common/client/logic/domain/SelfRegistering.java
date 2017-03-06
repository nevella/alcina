package cc.alcina.framework.common.client.logic.domain;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

public interface SelfRegistering<T extends HasIdAndLocalId> {
	default T register() {
		TransformManager.get().registerDomainObject((T) this);
		return (T) this;
	}
}

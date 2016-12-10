package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface HasTransformPersistable {
	public HasIdAndLocalId resolvePersistable();
}

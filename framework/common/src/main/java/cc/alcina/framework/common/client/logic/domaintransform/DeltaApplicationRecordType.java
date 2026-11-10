package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public enum DeltaApplicationRecordType {
	LOCAL_TRANSFORMS_APPLIED, LOCAL_TRANSFORMS_REMOTE_PERSISTED,
	REMOTE_DELTA_APPLIED;
}
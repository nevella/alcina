package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

public abstract class RerunOnTransformDelta {
	public void run() throws Exception {
		run0();
		if (!TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
				.isEmpty()) {
			ServletLayerUtils.pushTransformsAsRoot();
			run0();
		}
	}

	protected abstract void run0() throws Exception;
}
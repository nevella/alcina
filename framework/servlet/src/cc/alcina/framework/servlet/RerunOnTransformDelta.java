package cc.alcina.framework.servlet;

import java.util.ConcurrentModificationException;

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

public abstract class RerunOnTransformDelta {
	public void run() throws Exception {
		run0();
		if (!TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
				.isEmpty()) {
			ServletLayerTransforms.pushTransformsAsRoot();
			try {
				Thread.sleep(10);
				run0();
				// TODO - concurrency issue with transforms hitting in a
				// different thread??
			} catch (ConcurrentModificationException e) {
				Thread.sleep(500);
				run0();
			}
		}
	}

	protected abstract void run0() throws Exception;
}
package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

public abstract class RerunOnTransformDelta {
	public void run() throws Exception {
		run0();
		if (!TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
				.isEmpty()) {
			if (AppPersistenceBase.isInstanceReadOnly()) {
				Ax.err("Discarding transforms: read-only");
				ThreadlocalTransformManager.cast().resetTltm(null);
			}
			Transaction.commit();
			Thread.sleep(10);
			run0();
		}
	}

	protected abstract void run0() throws Exception;
}
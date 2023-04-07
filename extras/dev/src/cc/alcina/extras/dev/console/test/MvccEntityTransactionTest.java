package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.ServerTask;

public abstract class MvccEntityTransactionTest extends ServerTask {
	transient private Exception lastThreadException;

	@Override
	public void run() throws Exception {
		try {
			Ax.err(getClass().getSimpleName());
			LooseContext.push();
			Transaction.ensureEnded();
			Transaction.begin();
			logger.info("Started job: {}", getClass().getName());
			run1();
			if (lastThreadException != null) {
				throw lastThreadException;
			}
		} catch (Exception e) {
			TransformManager.get().clearTransforms();
			throw WrappedRuntimeException.wrap(e);
		} finally {
			Transaction.endAndBeginNew();
			LooseContext.pop();
		}
	}

	protected void notifyThreadException(Exception e) {
		TransformManager.get().clearTransforms();
		lastThreadException = e;
	}

	protected abstract void run1() throws Exception;
}

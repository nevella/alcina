package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public abstract class MvccEntityTransactionTest extends AbstractTaskPerformer {
	transient private Exception lastThreadException;

	protected void notifyThreadException(Exception e) {
		TransformManager.get().clearTransforms();
		lastThreadException = e;
	}

	@Override
	protected void run(boolean throwExceptions) {
		try {
			Ax.err(getClass().getSimpleName());
			LooseContext.push();
			Transaction.ensureEnded();
			Transaction.begin();
			logger.info("Started job: {}", getClass().getName());
			run0();
			if (lastThreadException != null) {
				throw lastThreadException;
			}
		} catch (Exception e) {
			TransformManager.get().clearTransforms();
			if (throwExceptions) {
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			} else {
				e.printStackTrace();
			}
		} finally {
			Transaction.endAndBeginNew();
			LooseContext.pop();
		}
	}
}

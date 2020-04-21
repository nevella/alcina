package cc.alcina.extras.dev.console.test;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class MvccEntiityTransactionTest extends AbstractTaskPerformer{
	private Exception lastThreadException;
	protected void run(boolean throwExceptions) {
		try {
			Ax.err(getClass().getSimpleName());
			LooseContext.push();
			Transaction.ensureEnded();
			Transaction.begin();
			String message = Ax.format("Started job: %s", getClass().getName());
			if (actionLogger != null) {
				actionLogger.info(message);
			} else {
				Ax.out(message);
			}
			run0();
			if(lastThreadException!=null){
				throw lastThreadException;
			}
		} catch (Exception e) {
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
	protected void notifyThreadException(Exception e) {
		TransformManager.get().clearTransforms();
		lastThreadException=e;		
	}
}

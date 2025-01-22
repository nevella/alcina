package cc.alcina.framework.servlet.environment;

import java.util.concurrent.CountDownLatch;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * A remote UI that requires a domain store transaction when executing
 */
public interface DomainUi extends RemoteUi {
	static class DomainInitAwaiter {
		CountDownLatch loadedLatch;

		void await() {
			loadedLatch = new CountDownLatch(1);
			DomainStore.topicStoreLoadingComplete
					.addWithPublishedCheck(loadedLatch::countDown);
			try {
				loadedLatch.await();
			} catch (Exception e) {
				WrappedRuntimeException.throwWrapped(e);
			}
		}
	}

	@Override
	default void onBeforeEnterContext() {
		new DomainInitAwaiter().await();
	}

	@Override
	default void onEnterIteration() {
		PermissionsManager.get().pushSystemUser();
		Transaction.ensureBegun();
	}

	@Override
	default void onExitIteration() {
		Transaction.end();
		PermissionsManager.get().popSystemUser();
	}
}

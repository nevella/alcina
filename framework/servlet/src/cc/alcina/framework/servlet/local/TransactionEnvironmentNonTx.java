package cc.alcina.framework.servlet.local;

import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

public class TransactionEnvironmentNonTx implements TransactionEnvironment {
	public static final String CONTEXT_COMMITTING = TransactionEnvironmentNonTx.class
			.getName() + ".CONTEXT_COMMITTING";

	public static void runAndCommit(ThrowingRunnable runnable) {
		LocalDomainQueue.run(() -> {
			runnable.run();
			TransactionEnvironment.get().commit();
		});
	}

	@Override
	public void begin() {
		// NOOP, tx is effectively just the presence of a transformmanager
	}

	@Override
	public void commit() {
		LocalDomainStore.get().commit();
	}

	@Override
	public void commitWithBackoff() {
		commit();
	}

	@Override
	public void end() {
		withDomainAccess0(() -> {
			if (!LocalDomainStore.get().isEmptyCommitQueue()) {
				LocalDomainStore.get().dumpCommitQueue();
				throw new IllegalStateException(
						"LocalDomainStore commit queue not empty");
			}
			Preconditions
					.checkState(LocalDomainStore.get().isEmptyCommitQueue());
		});
	}

	@Override
	public void endAndBeginNew() {
		end();
	}

	@Override
	public void ensureBegun() {
		// NOOP
	}

	@Override
	public void ensureEnded() {
		end();
	}

	@Override
	public TransactionId getCurrentTxId() {
		return new TransactionId(
				LocalDomainStore.get().commitToStorageTransformListener
						.getCurrentRequestId());
	}

	@Override
	public boolean isInActiveTransaction() {
		return true;
	}

	@Override
	public boolean isInNonSingleThreadedProjectionState() {
		return false;
	}

	@Override
	public boolean isInTransaction() {
		return true;
	}

	@Override
	public boolean isMultiple() {
		return false;
	}

	@Override
	public boolean isToDomainCommitting() {
		return LocalDomainStore.get().committingRequest;
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		// NOOP (probably)
	}

	@Override
	public void withDomainAccess0(Runnable runnable) {
		LocalDomainQueue.run(ThrowingRunnable.wrapRunnable(runnable));
	}

	@Override
	public <T> T withDomainAccess0(Supplier<T> supplier) {
		Ref<T> ref = new Ref<>();
		LocalDomainQueue.run(() -> {
			ref.set(supplier.get());
		});
		return ref.get();
	}

	@Override
	public boolean isCommittedOrRelatedCommitted(TransactionId transactionId) {
		return LooseContext.is(CONTEXT_COMMITTING);
	}
}

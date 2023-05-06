package cc.alcina.framework.servlet.local;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.domain.TransactionId;

public class TransactionEnvironmentNonTx implements TransactionEnvironment {
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
		Preconditions.checkState(LocalDomainStore.get().isEmptyCommitQueue());
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
}

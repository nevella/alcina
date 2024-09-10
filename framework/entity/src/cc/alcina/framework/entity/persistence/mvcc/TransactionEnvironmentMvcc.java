package cc.alcina.framework.entity.persistence.mvcc;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.BplDelegateMapCreator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;

public class TransactionEnvironmentMvcc
		implements TransactionEnvironment {
	public TransactionEnvironmentMvcc() {
		Registry.register().add(
				BaseProjectionSupportMvcc.BplDelegateMapCreatorTransactional.class,
				List.of(BplDelegateMapCreator.class),
				Registration.Implementation.INSTANCE,
				Registration.Priority.PREFERRED_LIBRARY);
		Registry.register().add(
				BaseProjectionSupportMvcc.TreeMapRevCreatorImpl.class,
				List.of(CollectionCreators.TreeMapRevCreator.class),
				Registration.Implementation.INSTANCE,
				Registration.Priority.PREFERRED_LIBRARY);
		Registry.register().add(
				BaseProjectionSupportMvcc.TreeMapCreatorImpl.class,
				List.of(CollectionCreators.TreeMapCreator.class),
				Registration.Implementation.INSTANCE,
				Registration.Priority.PREFERRED_LIBRARY);
		Registry.register().add(
				BaseProjectionSupportMvcc.MultiTrieCreatorImpl.class,
				List.of(CollectionCreators.MultiTrieCreator.class),
				Registration.Implementation.INSTANCE,
				Registration.Priority.PREFERRED_LIBRARY);
	}

	@Override
	public void begin() {
		Transaction.begin();
	}

	@Override
	public void commit() {
		Transaction.commit();
	}

	@Override
	public void commitWithBackoff() {
		TransformCommit.commitWithBackoff();
	}

	@Override
	public void end() {
		Transaction.end();
	}

	@Override
	public void endAndBeginNew() {
		Transaction.endAndBeginNew();
	}

	@Override
	public void ensureBegun() {
		Transaction.ensureBegun();
	}

	@Override
	public void ensureEnded() {
		Transaction.ensureEnded();
	}

	@Override
	public TransactionId getCurrentTxId() {
		return Transaction.current().getId();
	}

	@Override
	public boolean isInActiveTransaction() {
		return Transaction.isInActiveTransaction();
	}

	@Override
	public boolean isInNonSingleThreadedProjectionState() {
		return !Transaction.current().isBaseTransaction();
	}

	@Override
	public boolean isInTransaction() {
		return Transaction.isInTransaction();
	}

	@Override
	public boolean isMultiple() {
		return true;
	}

	@Override
	public boolean isToDomainCommitting() {
		return Transaction.current().isToDomainCommitting();
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		DomainStore.waitUntilCurrentRequestsProcessed();
	}

	@Override
	public void withDomainAccess0(Runnable runnable) {
		Preconditions.checkState(isInActiveTransaction());
		runnable.run();
	}

	@Override
	public <T> T withDomainAccess0(Supplier<T> supplier) {
		Preconditions.checkState(isInActiveTransaction());
		return supplier.get();
	}

	@Override
	public boolean
			isCommittedOrRelatedCommitted(TransactionId committingTxId) {
		return Transactions.get()
				.isCommittedOrRelatedCommitted0(committingTxId);
	}
}
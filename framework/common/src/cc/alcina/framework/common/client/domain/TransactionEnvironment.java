package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.mvcc.TransactionId;

/*
 * Provides access to either a full mvcc transactional system
 * (mvcc/transactions) or a decent enough single-threaded, single current
 * transaction approximation (TransactionEnvironmentNonTx)
 * 
 * Although *this* could be Transaction and the mvcc Transaction class be
 * MvccTransaction, fact is that the latter is much more heavily used. So leave
 * as is
 */
public interface TransactionEnvironment {
	public static TransactionEnvironment get() {
		return Registry.impl(TransactionEnvironment.class);
	}

	public void begin();

	public void commit();

	public void commitWithBackoff();

	public void end();

	public void endAndBeginNew();

	public void ensureBegun();

	public void ensureEnded();

	public TransactionId getCurrentTxId();

	public boolean isInActiveTransaction();

	public boolean isInNonSingleThreadedProjectionState();

	public boolean isInTransaction();

	public boolean isMultiple();

	public boolean isToDomainCommitting();

	public void waitUntilCurrentRequestsProcessed();;
}
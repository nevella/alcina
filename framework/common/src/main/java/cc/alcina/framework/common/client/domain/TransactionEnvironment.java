package cc.alcina.framework.common.client.domain;

import java.util.function.Supplier;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

/*
 * Provides access to either a full mvcc transactional system
 * (mvcc/transactions) or a decent enough single-threaded, single current
 * transaction approximation (TransactionEnvironmentNonTx)
 * 
 * Although *this* could be Transaction and the mvcc Transaction class be
 * MvccTransaction, fact is that the latter is much more heavily used. So leave
 * as is
 */
@Reflected
public interface TransactionEnvironment {
	static TransactionEnvironment get() {
		return Registry.impl(TransactionEnvironment.class);
	}

	static void withDomain(Runnable runnable) {
		get().withDomainAccess0(runnable);
	}

	static void withDomainTxThrowing(ThrowingRunnable runnable) {
		TransactionEnvironment env = get();
		env.withDomainAccess0(() -> {
			boolean startedTx = false;
			try {
				if (!env.isInActiveTransaction()) {
					env.begin();
					startedTx = true;
				}
				runnable.run();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				if (startedTx) {
					env.end();
				}
			}
		});
	}

	static <T> T withDomain(Supplier<T> supplier) {
		return get().withDomainAccess0(supplier);
	}

	static <T> T withDomainCommitting(Supplier<T> supplier) {
		return withDomain(() -> {
			T result = supplier.get();
			get().commit();
			return result;
		});
	}

	static void withDomainCommitting(Runnable runnable) {
		withDomain(() -> {
			runnable.run();
			get().commit();
		});
	}

	void begin();

	void commit();

	void commitWithBackoff();

	void end();

	void endAndBeginNew();

	void ensureBegun();

	void ensureEnded();

	TransactionId getCurrentTxId();

	boolean isInActiveTransaction();

	boolean isInNonSingleThreadedProjectionState();

	boolean isInTransaction();

	boolean isMultiple();

	boolean isToDomainCommitting();

	void waitUntilCurrentRequestsProcessed();

	void withDomainAccess0(Runnable runnable);

	<T> T withDomainAccess0(Supplier<T> supplier);

	boolean isCommittedOrRelatedCommitted(TransactionId transactionId);
}
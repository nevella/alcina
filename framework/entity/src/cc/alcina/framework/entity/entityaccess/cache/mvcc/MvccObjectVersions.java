package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before its commit (unless the object is
 * created within that transaction).
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class MvccObjectVersions<T extends HasIdAndLocalId> {
	public static <T extends HasIdAndLocalId> MvccObjectVersions<T> ensure(T t,
			Transaction transaction) {
		MvccObject mvccObject = (MvccObject) t;
		MvccObjectVersions<T> versions = null;
		// double-check
		synchronized (mvccObject) {
			versions = mvccObject.__getMvccVersions__();
			if (versions == null) {
				versions = new MvccObjectVersions<T>(t, transaction);
				mvccObject.__setMvccVersions__(versions);
			}
			return versions;
		}
	}

	ConcurrentHashMap<Transaction, ObjectVersion<T>> versions = new ConcurrentHashMap<>();

	StoreTransaction baseStoreTransaction;

	Object versionCreationMonitor = new Object();

	public MvccObjectVersions(T t, Transaction initialTransaction) {
		/*
		 * If in the 'preparing' phase, and t is non-local, this is a
		 * modification of the base transaction version.
		 * 
		 * In that case, create an initial copy, keyed to the base transaction -
		 * if developing (console, not server - assume single-task) this allows
		 * users to directly modify their copy without needing to swap to a
		 * writeable version. In production, force the swap (there's a tiny risk
		 * of a race)
		 */
		if (initialTransaction.phase == TransactionPhase.PREPARING
				&& t.provideWasPersisted()) {
			if (Ax.isTest()) {
				{
					ObjectVersion<T> version = new ObjectVersion<>();
					version.transaction = Transactions
							.baseTransaction(Mvcc.getStore(t));
					version.object = Transactions.copyObject(t);
					versions.put(version.transaction, version);
				}
				{
					ObjectVersion<T> version = new ObjectVersion<>();
					version.transaction = initialTransaction;
					version.object = t;
					versions.put(version.transaction, version);
				}
			} else {
				{
					ObjectVersion<T> version = new ObjectVersion<>();
					version.transaction = Transactions
							.baseTransaction(Mvcc.getStore(t));
					version.object = t;
					versions.put(version.transaction, version);
				}
				{
					ObjectVersion<T> version = new ObjectVersion<>();
					version.transaction = initialTransaction;
					version.object = Transactions.copyObject(t);
					versions.put(version.transaction, version);
				}
			}
		} else {
			/* transforms from another vm or created this tx */
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = t;
				versions.put(version.transaction, version);
			}
		}
	}

	T resolve(boolean write) {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null) {
			return version.object;
		}
		synchronized (versionCreationMonitor) {
			version = versions.get(transaction);
			if (version != null) {
				return version.object;
			}
			version = new ObjectVersion<>();
			version.transaction = transaction;
			Transaction mostRecent = transaction.mostRecentPriorTransaction(
					versions.keys(), baseStoreTransaction.store);
			version.object = Transactions
					.copyObject(versions.get(mostRecent).object);
			versions.put(transaction, version);
			if (write) {
				Domain.register(version.object);
				// FIXME - probably register prop changes with indexing listener
			}
		}
		return version.object;
	}
}

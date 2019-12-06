package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

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

	/*
	 * debugging aids
	 */
	T __mostRecentReffed;

	T __mostRecentWritable;

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
		 * 
		 * If developing, make sure to abort the transaction if not committing
		 * (dangling transforms will modify the base graph)
		 * 
		 */
		if (initialTransaction.phase == TransactionPhase.TO_DB_PREPARING
				&& t.provideWasPersisted()) {
			if (Ax.isTest() && false) {
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
					((MvccObject) version.object).__setMvccVersions__(this);
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
		Ax.err(toString());
	}

	@Override
	public String toString() {
		try {
			ObjectVersion<T> firstVersion = versions.values().iterator().next();
			T object = firstVersion.object;
			Field field = SEUtilities.getFieldByName(object.getClass(), "id");
			Object id = field.get(object);
			return Ax.format("versions: %s : base: %s/%s : initial-tx: %s",
					versions.size(), object.getClass(), id,
					firstVersion.transaction);
		} catch (Exception e) {
			return "exception..";
		}
	}

	private T resolve0(boolean write) {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null) {
			return version.object;
		}
		// use versions as a monitor on creation
		synchronized (versions) {
			version = versions.get(transaction);
			if (version != null) {
				return version.object;
			}
			Ax.err(toString());
			version = new ObjectVersion<>();
			version.transaction = transaction;
			Transaction mostRecent = transaction.mostRecentPriorTransaction(
					versions.keys(),
					DomainStore.stores()
							.storeFor(versions.values().iterator().next().object
									.provideEntityClass()));
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

	T resolve(boolean write) {
		T resolved = resolve0(write);
		__mostRecentReffed = resolved;
		if (write) {
			__mostRecentWritable = resolved;
		}
		return resolved;
	}
}

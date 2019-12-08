package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before it is committed to the domain
 * graph (unless the object is created within that transaction).
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class MvccObjectVersions<T extends HasIdAndLocalId>
		implements Vacuumable {
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

	volatile ConcurrentHashMap<Transaction, ObjectVersion<T>> versions = new ConcurrentHashMap<>();

	volatile T baseObject;

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
		 * In that case, create an initial copy, keyed to the base transaction.
		 * 
		 * 
		 * 
		 */
		if (initialTransaction.phase == TransactionPhase.TO_DB_PREPARING
				&& t.provideWasPersisted()) {
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = Transactions
						.baseTransaction(Mvcc.getStore(t));
				version.object = t;
				putVersion(version);
			}
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = Transactions.copyObject(t);
				((MvccObject) version.object).__setMvccVersions__(this);
				putVersion(version);
			}
		} else {
			/*
			 * transforms from another vm - or domain commit - or created this
			 * tx
			 */
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = t;
				putVersion(version);
			}
		}
		baseObject = t;
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

	@Override
	public void vacuum(Transaction transaction) {
		ObjectVersion<T> version = versions.get(transaction);
		/*
		 * Must change the base before removing transaction (otherwise there's a
		 * narrow window where we'd return an older version)
		 */
		if (transaction.getPhase() == TransactionPhase.TO_DOMAIN_COMMITTED) {
			baseObject = version.object;
		}
		versions.remove(transaction);
	}

	private void putVersion(ObjectVersion<T> version) {
		versions.put(version.transaction, version);
		Transactions.get().onAddedVacuumable(this);
	}

	/*
	 * Try and return a cached version if possible (if cache is not 'writeable'
	 * and we're writing, we need to replace it for this tx with a writeable
	 * version). Note that all this is not hit unless the domain object is
	 * modified during the jvm lifetime -
	 */
	private T resolve0(boolean write) {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		// use versions as a monitor on creation
		synchronized (versions) {
			version = versions.get(transaction);
			if (version != null && version.isCorrectWriteableState(write)) {
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
			T mostRecentObject = null;
			ObjectVersion<T> mostRecentVersion = versions.get(mostRecent);
			if (mostRecentVersion != null) {
				mostRecentObject = mostRecentVersion.object;
			} else {
				mostRecentObject = baseObject;
			}
			if (write) {
				version.object = Transactions.copyObject(mostRecentObject);
				Domain.register(version.object);
				putVersion(version);
				return version.object;
			} else {
				/*
				 * We could cache - by creating an ObjectVerson for this read
				 * call - but that would require a later vacuum
				 */
				return mostRecentObject;
			}
		}
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

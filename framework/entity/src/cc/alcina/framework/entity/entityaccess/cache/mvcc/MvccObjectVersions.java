package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Field;
import java.util.Iterator;
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
			Transaction transaction, boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) t;
		MvccObjectVersions<T> versions = null;
		// double-check
		synchronized (mvccObject) {
			versions = mvccObject.__getMvccVersions__();
			if (versions == null) {
				versions = new MvccObjectVersions<T>(t, transaction,
						initialObjectIsWriteable);
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

	public MvccObjectVersions(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
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
				version.writeable = initialObjectIsWriteable;
				putVersion(version);
			}
		}
		baseObject = t;
		Ax.err("created object version: %s : %s", toString(), hashCode());
	}

	@Override
	public String toString() {
		try {
			T object = baseObject;
			Transaction transaction = null;
			synchronized (versions) {
				Iterator<ObjectVersion<T>> itr = versions.values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			/*
			 * use field rather than getters to not resolve
			 */
			Field idField = SEUtilities.getFieldByName(object.getClass(), "id");
			Field localIdField = SEUtilities.getFieldByName(object.getClass(),
					"localId");
			Object id = idField.get(object);
			return Ax.format("versions: %s : base: %s/%s/%s : initial-tx: %s",
					versions.size(), object.getClass(), id,
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
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
		synchronized (versions) {
			versions.remove(transaction);
		}
	}

	private void putVersion(ObjectVersion<T> version) {
		synchronized (versions) {
			versions.put(version.transaction, version);
		}
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
			if (versions.isEmpty()) {
				return baseObject;
			}
			version = versions.get(transaction);
			if (version != null && version.isCorrectWriteableState(write)) {
				return version.object;
			}
			version = new ObjectVersion<>();
			version.transaction = transaction;
			Transaction mostRecentTransaction = transaction
					.mostRecentPriorTransaction(versions.keys(),
							DomainStore.stores().storeFor(
									versions.values().iterator().next().object
											.provideEntityClass()));
			T mostRecentObject = null;
			/*
			 * mostRecent will be null if just created
			 */
			ObjectVersion<T> mostRecentVersion = mostRecentTransaction == null
					? null
					: versions.get(mostRecentTransaction);
			if (mostRecentVersion != null) {
				mostRecentObject = mostRecentVersion.object;
			} else {
				mostRecentObject = baseObject;
			}
			if (write) {
				Ax.err(toString());
				version.object = Transactions.copyObject(mostRecentObject);
				version.writeable = true;
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

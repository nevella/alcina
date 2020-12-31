package cc.alcina.framework.entity.persistence.mvcc;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.Vacuumable;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.VacuumableTransactions;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before it is committed to the domain
 * graph (unless the object is created within that transaction).
 * 
 * synchronization - resolution of version is synchronized on the baseobject, as
 * is vacuum.
 * 
 * @author nick@alcina.cc
 * 
 * 
 *
 * @param <T>
 */
public abstract class MvccObjectVersions<T> implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

	public static int debugRemoval = 0;

	// called in a synchronized block (synchronized on baseObject)
	static <E extends Entity> MvccObjectVersions<E> ensureEntity(E baseObject,
			Transaction transaction, boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) baseObject;
		MvccObjectVersions<E> versions = null;
		versions = new MvccObjectVersionsEntity<E>(baseObject, transaction,
				initialObjectIsWriteable);
		mvccObject.__setMvccVersions__(versions);
		return versions;
	}

	// called in a synchronized block (synchronized on baseObject)
	static MvccObjectVersions<TransactionalTrieEntry> ensureTrieEntry(
			TransactionalTrieEntry baseObject, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) baseObject;
		MvccObjectVersions<TransactionalTrieEntry> versions = null;
		versions = new MvccObjectVersionsTrieEntry(baseObject, transaction,
				initialObjectIsWriteable);
		mvccObject.__setMvccVersions__(versions);
		return versions;
	}

	private volatile ConcurrentSkipListMap<Transaction, ObjectVersion<T>> versions = new ConcurrentSkipListMap<>(
			Collections.reverseOrder());

	// object pointed to by this field never changes (for a given class/id or
	// class/clientinstance/localid
	// tuple) - it is the 'domain identity'. Its fields can be updated by vacuum
	// if not accessible from any
	// active transaction
	//
	// TODO - above is true for mvccobjectversionsentity, but not for other
	// variants (where baseObject may in fact be changed by vacuum)...in
	// fact...maybe the above para is true (since we use copyobjectfields)
	private volatile T baseObject;

	/*
	 * debugging aids
	 */
	@SuppressWarnings("unused")
	private T __mostRecentReffed;

	@SuppressWarnings("unused")
	private T __mostRecentWritable;

	/*
	 * Only used to determine if a txmap key is not visible to a given
	 * transaction (post vacuum)
	 */
	private TransactionId firstCommittedTransactionId;

	/*
	 * synchronization - either a newly created object t for this domainstore
	 * (so no sync needed), or accessed via a block synced on 't'
	 */
	MvccObjectVersions(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		/*
		 * If in the 'preparing' phase, and t is non-local, this is a
		 * modification of the initial transaction version.
		 * 
		 * In that case, create a copy (to be modified) from the base object
		 * 
		 * 
		 * FIXME - mvcc.4 - explain logic here and in resolve0
		 * 
		 * Also check the logic here. What about a transform from 'outside'
		 * against an object in our graph? Doesn't that also need the defensive
		 * copy?
		 * 
		 * 20201228 - this may be the root of many evils (the 'optimisation') -
		 * it's the old 'domain identity // defensive copy' thang
		 */
		baseObject = t;
		if (initialTransaction.phase == TransactionPhase.TO_DB_PREPARING
				&& accessibleFromOtherTransactions(t)) {
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = copyObject(t);
				((MvccObject) version.object).__setMvccVersions__(this);
				putVersion(version);
			}
		} else {
			/*
			 * transforms from another vm - or domain commit - or created this
			 * tx. initialObjectIsWriteable will only be true on object creation
			 * (so it's the correct value, the object won't be visible outside
			 * the tx until the tx is finished).
			 * 
			 * if initialObjectIsWriteable==false; the incoming object won't be
			 * modified (so may well be the base or a previous tx version)
			 */
			{
				ObjectVersion<T> version = new ObjectVersion<>();
				version.transaction = initialTransaction;
				version.object = t;
				version.writeable = initialObjectIsWriteable;
				putVersion(version);
			}
		}
	}

	public T getBaseObject() {
		return this.baseObject;
	}

	@Override
	public String toString() {
		try {
			T object = baseObject;
			Transaction transaction = null;
			synchronized (baseObject) {
				Iterator<ObjectVersion<T>> itr = versions.values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			if (object instanceof Entity) {
				/*
				 * use field rather than getters to not resolve
				 */
				Field idField = SEUtilities.getFieldByName(object.getClass(),
						"id");
				Field localIdField = SEUtilities
						.getFieldByName(object.getClass(), "localId");
				Object id = idField.get(object);
				return Ax.format(
						"versions: %s : base: %s/%s/%s : initial-tx: %s",
						versions.size(), object.getClass(), id,
						System.identityHashCode(object),
						transaction == null ? transaction : "base");
			} else {
				return Ax.format("versions: %s : base: %s/%s : initial-tx: %s",
						versions.size(), object.getClass(),
						System.identityHashCode(object),
						transaction == null ? transaction : "base");
			}
		} catch (Exception e) {
			return "exception..";
		}
	}

	/*
	 * only synchronize at the "possibly remove from base" phase - all other
	 * operations are threadsafe
	 * 
	 */
	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		versions.keySet().removeAll(
				vacuumableTransactions.completedNonDomainTransactions);
		/*
		 * completedDomainTransactions ordered by most-recent to oldest
		 */
		Transaction oldestVacuumableDomainTransaction = vacuumableTransactions.completedDomainTransactions
				.iterator().hasNext()
						? vacuumableTransactions.completedDomainTransactions
								.last()
						: null;
		if (oldestVacuumableDomainTransaction != null) {
			Transaction mostRecentCommonVisible = TransactionVersions
					.mostRecentCommonVisible(versions.keySet(),
							vacuumableTransactions.completedDomainTransactions);
			/*
			 * Must change the base before removing transaction (otherwise
			 * there's a narrow window where we'd return an older version)
			 */
			if (mostRecentCommonVisible != null) {
				ObjectVersion<T> version = versions
						.get(mostRecentCommonVisible);
				// baseObject fields will not be reachable here to app code (all
				// active txs will be
				// looking at version.object fields)
				copyObject(version.object, baseObject);
				if (firstCommittedTransactionId == null) {
					firstCommittedTransactionId = oldestVacuumableDomainTransaction
							.getId();
				}
			}
			// Transactions.debugRemoveVersion(baseObject, version);
			ObjectBidirectionalIterator<Transaction> bidiItr = vacuumableTransactions.completedDomainTransactions
					.iterator(oldestVacuumableDomainTransaction);
			// remove from oldest to most recent, otherwise there's a chance of
			// accessing incorrect versions
			while (bidiItr.hasPrevious()) {
				versions.keySet().remove(bidiItr.previous());
			}
		}
		if (versions.isEmpty()) {
			synchronized (baseObject) {
				if (versions.isEmpty() && baseObject instanceof MvccObject) {
					logger.trace("removed mvcc versions: {} : {}", baseObject,
							System.identityHashCode(baseObject));
					((MvccObject) baseObject).__setMvccVersions__(null);
				}
			}
		}
	}

	private void putVersion(ObjectVersion<T> version) {
		versions.put(version.transaction, version);
		Transactions.get().onAddedVacuumable(version.transaction, this);
	}

	/*
	 * Try and return a cached version if possible (if cached version is not
	 * 'writeable' and we're writing, we need to replace it for this tx with a
	 * writeable version). Note that all this is not hit unless the mvcc object
	 * is modified during the jvm lifetime -
	 */
	private T resolve0(boolean write) {
		Transaction transaction = Transaction.current();
		if (write && transaction.isReadonly()) {
			throw new MvccException("Writing within a readonly transaction");
		}
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		if (versions.isEmpty() && !write) {
			if (thisMayBeVisibleToPriorTransactions()
					&& !transaction.isVisible(firstCommittedTransactionId)) {
				return null;
			} else {
				return baseObject;
			}
		}
		Class<? extends Entity> entityClass = entityClass();
		Transaction mostRecentTransaction = transaction
				.mostRecentVisibleCommittedTransaction(versions.keySet());
		if (mostRecentTransaction == null && !write) {
			/*
			 * Object not visible to the current tx - note that if this is an
			 * instance of MvccObjectVersionsEntity this will never be true
			 */
			if (thisMayBeVisibleToPriorTransactions()
					&& !transaction.isVisible(firstCommittedTransactionId)) {
				return null;
			}
		}
		T mostRecentObject = null;
		/*
		 * mostRecentVersion will be null if just created
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
			version = new ObjectVersion<>();
			version.transaction = transaction;
			version.object = copyObject(mostRecentObject);
			version.writeable = true;
			// put before register (which will call resolve());
			putVersion(version);
			onVersionCreation(version.object);
			return version.object;
		} else {
			/*
			 * We could cache - by creating an ObjectVerson for this read call -
			 * but that would require a later vacuum
			 */
			return mostRecentObject;
		}
	}

	protected abstract boolean accessibleFromOtherTransactions(T t);

	protected T copyObject(T mostRecentObject) {
		return (T) Transactions.copyObject((MvccObject) mostRecentObject);
	}

	protected abstract void copyObject(T fromObject, T baseObject);

	protected abstract <E extends Entity> Class<E> entityClass();

	protected abstract void onVersionCreation(T object);

	protected abstract boolean thisMayBeVisibleToPriorTransactions();

	void debugResolvedVersion() {
		try {
			T resolved = resolve0(false);
			long id = (long) SEUtilities
					.getFieldByName(resolved.getClass(), "id").get(resolved);
			long localId = (long) SEUtilities
					.getFieldByName(resolved.getClass(), "localId")
					.get(resolved);
			Ax.out("dbg resolved: %s %s %s", id, localId,
					System.identityHashCode(resolved));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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

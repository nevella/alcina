package cc.alcina.framework.entity.persistence.mvcc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.Vacuumable;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.VacuumableTransactions;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before it is committed to the domain
 * graph (unless the object is created within that transaction).
 * 
 * synchronization - resolution of version is synchronized on the baseobject, as
 * is vacuum/removal of this from base object.
 * 
 * vacuum map swap is synchronized on (this)
 * 
 * 
 * 
 * @author nick@alcina.cc
 * 
 * 
 *
 * @param <T>
 */
public abstract class MvccObjectVersions<T> implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

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

	private List<VersionDebug> debugVersions = null;

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

	private AtomicInteger size = new AtomicInteger();

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
			synchronized (this) {
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
	 * the two bulk removal operations effectively use the same 'iterate across
	 * the smallest set' optimisation of AbstractSet, tailored to avoid
	 * CSLM.size()
	 * 
	 */
	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		int maxToRemove = vacuumableTransactions.completedDomainTransactions
				.size()
				+ vacuumableTransactions.completedNonDomainTransactions.size();
		if (size.get() == 0) {
			return;
		}
		// /*
		// * swap or modify live map, depending on size of delta
		// */
		// if (maxToRemove * 4 > size) {
		// ConcurrentSkipListMap<Transaction, ObjectVersion<T>> swap = new
		// ConcurrentSkipListMap<>(
		// Collections.reverseOrder());
		// versions.entrySet().stream().filter(
		// e -> !vacuumableTransactions.completedDomainTransactions
		// .contains(e.getKey()))
		// .filter(e -> !vacuumableTransactions.completedNonDomainTransactions
		// .contains(e.getKey()));
		// } else {
		/*
		 * Don't use removeAll (because it calls CSLM.size() - OK with Java9,
		 * not with Java8
		 */
		long start = System.currentTimeMillis();
		List initialValues = Arrays.asList(size,
				vacuumableTransactions.completedNonDomainTransactions.size(),
				vacuumableTransactions.completedDomainTransactions.size());
		if (size.get() > vacuumableTransactions.completedNonDomainTransactions
				.size()) {
			vacuumableTransactions.completedNonDomainTransactions
					.forEach(this::removeWithSize);
		} else {
			versions.keySet().stream().filter(
					tx -> vacuumableTransactions.completedNonDomainTransactions
							.contains(tx))
					.forEach(this::removeWithSize);
		}
		if (vacuumableTransactions.oldestVacuumableDomainTransaction != null) {
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
				// baseObject fields will not be reachable here to app code
				// (all active txs will be looking at version.object fields)
				if (isDebug()) {
					logger.info("Copying {} :: {} => {}", version,
							System.identityHashCode(version.object),
							System.identityHashCode(baseObject));
				}
				copyObject(version.object, baseObject);
				if (firstCommittedTransactionId == null) {
					firstCommittedTransactionId = vacuumableTransactions.oldestVacuumableDomainTransaction
							.getId();
				}
			}
			// remove from oldest to most recent, otherwise there's a chance
			// of accessing incorrect versions
			if (size.get() > vacuumableTransactions.completedDomainTransactions
					.size()) {
				// Transactions.debugRemoveVersion(baseObject, version);
				ObjectBidirectionalIterator<Transaction> bidiItr = vacuumableTransactions.completedDomainTransactions
						.iterator(
								vacuumableTransactions.oldestVacuumableDomainTransaction);
				while (bidiItr.hasPrevious()) {
					removeWithSize(bidiItr.previous());
				}
			} else {
				versions.descendingKeySet().tailSet(
						vacuumableTransactions.oldestVacuumableDomainTransaction)
						.stream()
						.filter(tx -> vacuumableTransactions.completedDomainTransactions
								.contains(tx))
						.forEach(this::removeWithSize);
			}
		}
		// }
		if (versions.isEmpty() && !isDebug()) {
			synchronized (baseObject) {
				if (versions.isEmpty() && baseObject instanceof MvccObject) {
					logger.trace("removed mvcc versions: {} : {}", baseObject,
							System.identityHashCode(baseObject));
					((MvccObject) baseObject).__setMvccVersions__(null);
				}
			}
		}
		long duration = System.currentTimeMillis() - start;
		if (duration > 10 && !EntityLayerUtils.isTestOrTestServer()) {
			logger.trace(
					"debug vacuum sizes pre  - versions: {}, completedNonDomainTransactions {}, completedDomainTransactions {}",
					initialValues.get(0), initialValues.get(1),
					initialValues.get(2));
			logger.trace(
					"debug vacuum sizes post - versions: {}, completedNonDomainTransactions {}, completedDomainTransactions {} - time {}ms",
					size,
					vacuumableTransactions.completedNonDomainTransactions
							.size(),
					vacuumableTransactions.completedDomainTransactions.size(),
					duration);
		}
	}

	private boolean isDebug() {
		return ResourceUtilities.is(MvccObjectVersions.class, "debug");
	}

	/*
	 * Guaranteed that version.transaction does not exist
	 */
	private void putVersion(ObjectVersion<T> version) {
		versions.put(version.transaction, version);
		size.incrementAndGet();
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
		T result = (T) Transactions.copyObject((MvccObject) mostRecentObject);
		if (isDebug()) {
			logger.info("Tx {} - copied {} {} => {}", Transaction.current(),
					mostRecentObject, System.identityHashCode(mostRecentObject),
					System.identityHashCode(result));
		}
		return result;
	}

	protected abstract void copyObject(T fromObject, T baseObject);

	protected abstract <E extends Entity> Class<E> entityClass();

	protected abstract void onVersionCreation(T object);

	protected void removeWithSize(Transaction tx) {
		ObjectVersion<T> removed = versions.remove(tx);
		if (removed != null) {
			if (isDebug()) {
				synchronized (this) {
					if (debugVersions == null) {
						debugVersions = new ArrayList<>();
					}
					debugVersions.add(new VersionDebug(tx, removed));
				}
			}
			size.decrementAndGet();
		}
	}

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

	@SuppressWarnings("unused")
	private class VersionDebug {
		Transaction transaction;

		ObjectVersion version;

		public VersionDebug(Transaction transaction, ObjectVersion version) {
			this.transaction = transaction;
			this.version = version;
		}
	}
}

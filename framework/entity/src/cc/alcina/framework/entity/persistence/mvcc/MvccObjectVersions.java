package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
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
 * FIXME - mvcc.4 - mvcc - object uniqueness - anything non-local must have a tx
 * - add tx/version caching - trans value only refs one mvccentity - look at
 * non-concurrent map for mvccobject.
 * 
 * This is in response to TransformCommit.getLocatorMapForClient - two
 * lazy-loaded mvcc objects, identical class/id, are non-identical. The
 * MvccObject (entity) should be unique until unreachable (post-vscuum) - ensure
 * this via checks in TransactionalValue and possible replacement in
 * DomainStore.SubgraphTransformManagerPostProcess.getEntityForCreate(DomainTransformEvent)
 * and local-id/creation checks in DomainStore.find(Class<T>, long)
 * 
 * Docs - why readable/writeable?
 * 
 * @author nick@alcina.cc
 * 
 * 
 *
 * @param <T>
 */
public abstract class MvccObjectVersions<T> implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

	// called in a synchronized block (synchronized on domainIdentity)
	static <E extends Entity> MvccObjectVersions<E> ensureEntity(
			E domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		return new MvccObjectVersionsEntity<E>(domainIdentity, transaction,
				initialObjectIsWriteable);
	}

	// called in a synchronized block (synchronized on domainIdentity)
	static MvccObjectVersions<TransactionalTrieEntry> ensureTrieEntry(
			TransactionalTrieEntry domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		return new MvccObjectVersionsTrieEntry(domainIdentity, transaction,
				initialObjectIsWriteable);
	}

	volatile ConcurrentSkipListMap<Transaction, ObjectVersion<T>> versions = new ConcurrentSkipListMap<>(
			Collections.reverseOrder());

	protected volatile T visibleAllTransactions;

	/*
	 * debugging aids, resolution caching
	 */
	private volatile T __mostRecentReffed;

	private volatile T __mostRecentWritable;

	/*
	 * Only used to determine if a txmap key is not visible to a given
	 * transaction (post vacuum)
	 */
	private TransactionId firstCommittedTransactionId;

	// resolution caching
	private volatile TransactionId mostRecentReffedTransactionId;

	private volatile TransactionId mostRecentWritableTransactionId;

	private AtomicInteger size = new AtomicInteger();

	Transaction initialWriteableTransaction;

	// object pointed to by this field never changes (for a given class/id or
	// class/clientinstance/localid
	// tuple) - . Its fields can be updated by vacuum
	// if not accessible from any
	// active transaction. The exception that proves this rule is vacuum -
	// lazy-loaded entities can be evicted and then reloaded (if the txs are
	// mutually invisible)
	//
	// TODO - document diff between domainIdentity and visibleAllTransactions
	protected T domainIdentity;

	/*
	 * synchronization - either a newly created object t for this domainstore
	 * (so no sync needed), or accessed via a block synced on 't'
	 */
	MvccObjectVersions(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		ObjectVersion<T> version = new ObjectVersion<>();
		version.transaction = initialTransaction;
		domainIdentity = t;
		visibleAllTransactions = initialAllTransactionsValueFor(t);
		if (initialObjectIsWriteable) {
			this.initialWriteableTransaction = initialTransaction;
			version.object = domainIdentity;
			version.writeable = true;
		} else {
			version.object = copyObject(t);
		}
		putVersion(version);
		onVersionCreation(version);
	}

	public T getDomainIdentity() {
		return this.domainIdentity;
	}

	@Override
	public String toString() {
		try {
			T object = visibleAllTransactions;
			Transaction transaction = null;
			synchronized (this) {
				Iterator<ObjectVersion<T>> itr = versions.values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			return Ax.format("versions: %s : base: %s/%s : initial-tx: %s",
					versions.size(), object.getClass(),
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
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
		boolean hadInitialWriteableTransaction = initialWriteableTransaction != null;
		if (hadInitialWriteableTransaction) {
			if (vacuumableTransactions.completedNonDomainTransactions
					.contains(initialWriteableTransaction)
					|| vacuumableTransactions.completedDomainTransactions
							.contains(initialWriteableTransaction)) {
				initialWriteableTransaction = null;
			}
		}
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
				if (firstCommittedTransactionId == null) {
					firstCommittedTransactionId = vacuumableTransactions.oldestVacuumableDomainTransaction
							.getId();
				}
			}
			// remove from oldest to most recent, otherwise there's a chance
			// of accessing incorrect versions
			if (size.get() > vacuumableTransactions.completedDomainTransactions
					.size()) {
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
	}

	/*
	 * Try and return a cached version if possible (if cached version is not
	 * 'writeable' and we're writing, we need to replace it for this tx with a
	 * writeable version). Note that all this is not hit unless the mvcc object
	 * is modified during the jvm lifetime -
	 */
	private T resolve0(Transaction transaction, boolean write) {
		if (write && transaction.isReadonly()) {
			throw new MvccException("Writing within a readonly transaction");
		}
		// try cached
		if (write) {
			if (mostRecentWritableTransactionId == transaction.getId()) {
				T result = __mostRecentWritable;
				// double-check on volatile;
				if (mostRecentWritableTransactionId == transaction.getId()) {
					return result;
				}
			}
		} else {
			if (mostRecentReffedTransactionId == transaction.getId()) {
				T result = __mostRecentReffed;
				// double-check on volatile;
				if (mostRecentReffedTransactionId == transaction.getId()) {
					return result;
				}
			}
		}
		/*
		 * TODO - doc - this makes sense but explain why...
		 */
		if ((size.get() == 0 || (transaction.isEmptyCommittedTransactions()
				&& !versions.containsKey(transaction))) && !write) {
			if (thisMayBeVisibleToPriorTransactions()
					&& !transaction.isVisible(firstCommittedTransactionId)) {
				return null;
			} else {
				return visibleAllTransactions;
			}
		}
		ObjectVersion<T> version = versions.get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		Transaction mostRecentTransaction = transaction
				.mostRecentVisibleCommittedTransaction(this);
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
		 * Allow upping from read- to write-version
		 */
		ObjectVersion<T> mostRecentVersion = version;
		if (mostRecentVersion == null) {
			mostRecentVersion = mostRecentTransaction == null ? null
					: versions.get(mostRecentTransaction);
		}
		if (mostRecentVersion != null) {
			mostRecentObject = mostRecentVersion.object;
		} else {
			mostRecentObject = visibleAllTransactions;
		}
		if (write) {
			version = new ObjectVersion<>();
			version.transaction = transaction;
			version.object = copyObject(mostRecentObject);
			version.writeable = true;
			// will remove the readable version, if any
			removeWithSize(transaction);
			// put before register (which will call resolve());
			putVersion(version);
			onVersionCreation(version);
			return version.object;
		} else {
			/*
			 * We could cache - by creating an ObjectVerson for this read call -
			 * but that would require a later vacuum
			 */
			return mostRecentObject;
		}
	}

	protected T copyObject(T mostRecentObject) {
		if (mostRecentObject == null) {
			T result = (T) Transactions.copyObject((MvccObject) domainIdentity,
					false);
			return result;
		} else {
			T result = (T) Transactions
					.copyObject((MvccObject) mostRecentObject, true);
			return result;
		}
	}

	protected abstract void copyObject(T fromObject, T baseObject);

	protected int getSize() {
		return this.size.get();
	}

	protected T initialAllTransactionsValueFor(T t) {
		return null;
	}

	protected void onVersionCreation(ObjectVersion<T> version) {
	}

	/*
	 * Guaranteed that version.transaction does not exist
	 */
	protected void putVersion(ObjectVersion<T> version) {
		versions.put(version.transaction, version);
		size.incrementAndGet();
		Transactions.get().onAddedVacuumable(version.transaction, this);
		updateCached(version.transaction, version.object, version.writeable);
	}

	protected void removeWithSize(Transaction tx) {
		ObjectVersion<T> version = versions.get(tx);
		if (version != null) {
			if (tx.isToDomainCommitted()) {
				visibleAllTransactions = version.object;
			}
			versions.remove(tx);
			if (__mostRecentReffed == version.object) {
				mostRecentReffedTransactionId = null;
				__mostRecentReffed = null;
			}
			if (__mostRecentWritable == version.object) {
				mostRecentWritableTransactionId = null;
				__mostRecentWritable = null;
			}
			size.decrementAndGet();
		}
	}

	protected boolean thisMayBeVisibleToPriorTransactions() {
		return false;
	}

	protected void updateCached(Transaction transaction, T resolved,
			boolean write) {
		if (resolved == null && this instanceof MvccObjectVersionsTrieEntry) {
			int debug = 3;
		}
		mostRecentReffedTransactionId = transaction.getId();
		__mostRecentReffed = resolved;
		if (write) {
			mostRecentWritableTransactionId = transaction.getId();
			__mostRecentWritable = resolved;
		}
	}

	T resolve(boolean write) {
		Transaction transaction = Transaction.current();
		T resolved = resolve0(transaction, write);
		updateCached(transaction, resolved, write);
		return resolved;
	}

	static abstract class MvccObjectVersionsMvccObject<T>
			extends MvccObjectVersions<T> {
		MvccObjectVersionsMvccObject(T t, Transaction initialTransaction,
				boolean initialObjectIsWriteable) {
			super(t, initialTransaction, initialObjectIsWriteable);
			if (!initialObjectIsWriteable) {
				// creating a versions object from a committed domainIdentity
				// object.
				//
				// DOC : this is called either from create()
				// (Transaction.create(), TransactionalTrieEntry constructor;
				// both with initialObjectIsWriteable) or resolve( with
				// false)(from a visible object with no mvccobjectversions i.e.
				// visible to all txs)
				visibleAllTransactions = domainIdentity;
			}
		}

		@Override
		public void vacuum(VacuumableTransactions vacuumableTransactions) {
			super.vacuum(vacuumableTransactions);
			if (versions.isEmpty()) {
				synchronized (domainIdentity) {
					if (versions.isEmpty()
							&& initialWriteableTransaction == null) {
						if (visibleAllTransactions != null) {
							if (visibleAllTransactions != domainIdentity) {
								copyObject(visibleAllTransactions,
										domainIdentity);
							}
							((MvccObject) visibleAllTransactions)
									.__setMvccVersions__(null);
							((MvccObject) domainIdentity)
									.__setMvccVersions__(null);
						}
					}
				}
			}
		}

		@Override
		protected void putVersion(ObjectVersion<T> version) {
			((MvccObject) domainIdentity).__setMvccVersions__(this);
			((MvccObject) version.object).__setMvccVersions__(this);
			super.putVersion(version);
		}
	}
}

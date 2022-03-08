package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.ResourceUtilities;
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
 * Complexities related to lazy load (e.g.
 * TransformCommit.getLocatorMapForClient) - two lazy-loaded mvcc objects,
 * identical class/id, are non-identical. The MvccObject (entity) is unique
 * until unreachable (post-vscuum) - this is ensured via checks in
 * TransactionalValue and possible replacement in
 * DomainStore.SubgraphTransformManagerPostProcess.getEntityForCreate(DomainTransformEvent)
 * and local-id/creation checks in DomainStore.find(Class<T>, long)
 *
 * FIXME - mvcc.5 - Docs - why/how do readable/writeable operate? Answer: the
 * "read" version is immutable and used as the source of later transaction's
 * versions. Note that read-only _versions_ are rare (possibly non-existent?
 * lazy-load?), but read-only _access_ (to prior visible tx version) is
 * definitely not.
 *
 * @author nick@alcina.cc
 *
 *
 *
 * @param <T>
 */
public abstract class MvccObjectVersions<T> implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

	protected static int notifyResolveNullCount = 100;

	protected static int notifyInvalidReadStateCount = 100;

	private static final ConcurrentSkipListMap<Transaction, ObjectVersion> EMPTY = new ConcurrentSkipListMap<Transaction, ObjectVersion>() {
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		};

		@Override
		public ObjectVersion put(Transaction key, ObjectVersion value) {
			throw new UnsupportedOperationException();
		};

		@Override
		public void
				putAll(Map<? extends Transaction, ? extends ObjectVersion> m) {
			throw new UnsupportedOperationException();
		};

		@Override
		public ObjectVersion putIfAbsent(Transaction key, ObjectVersion value) {
			throw new UnsupportedOperationException();
		};

		@Override
		public ObjectVersion remove(Object key) {
			throw new UnsupportedOperationException();
		};

		@Override
		public boolean remove(Object key, Object value) {
			throw new UnsupportedOperationException();
		};
	};

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

	private volatile ConcurrentSkipListMap<Transaction, ObjectVersion<T>> versions;

	protected volatile T visibleAllTransactions;

	private volatile CachedResolution cachedResolution;

	/*
	 * Only used to determine if a txmap key is not visible to a given
	 * transaction (post vacuum)
	 */
	private TransactionId firstCommittedTransactionId;

	// debugging
	private TransactionId initialTransactionId;

	/*
	 * Used as a monitor for any post-constructor operations which would change
	 * the map size (so read is not synchronized, write always is).
	 */
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
		initialTransactionId = initialTransaction.getId();
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
				Iterator<ObjectVersion<T>> itr = versions().values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			return Ax.format("versions: %s : base: %s/%s : initial-tx: %s",
					versions().size(), object.getClass(),
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
		} catch (Exception e) {
			return "exception..";
		}
	}

	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		synchronized (domainIdentity) {
			vacuum0(vacuumableTransactions);
		}
	}

	/*
	 * Try and return a cached version if possible (if cached version is not
	 * 'writeable' and we're writing, we need to replace it for this tx with a
	 * writeable version). Note that all this is not hit unless the mvcc object
	 * is modified during the jvm lifetime -
	 */
	private T resolve0(Transaction transaction, boolean write) {
		/*
		 * TODO - doc - this makes sense but explain why...
		 *
		 * and the doc is...I think - that this occurs either for
		 * TransactionalValue or during
		 * zeroVersions->visibleAllTransactions->remove-mvccObjectVersions
		 * vacuum sequence.
		 */
		if ((size.get() == 0 || (transaction.isEmptyCommittedTransactions()
				&& !versions().containsKey(transaction))) && !write) {
			if (mayBeReachableFromPreCreationTransactions()
					&& !transaction.isVisible(firstCommittedTransactionId)) {
				return null;
			} else {
				return visibleAllTransactions;
			}
		}
		ObjectVersion<T> version = versions().get(transaction);
		if (version != null && version.isCorrectWriteableState(write)) {
			return version.object;
		}
		Transaction mostRecentTransaction = transaction
				.mostRecentVisibleCommittedTransaction(this);
		if (mostRecentTransaction == null && !write) {
			/*
			 * Object not visible to the current tx
			 */
			if (mayBeReachableFromPreCreationTransactions()
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
					: versions().get(mostRecentTransaction);
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
			// note that resolve() is synchronized on domainIdentity, so no need
			// here...
			// synchronized (domainIdentity) {
			removeWithSize(transaction);
			// put before register (which will call resolve());
			putVersion(version);
			// }
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

	protected void debugNotResolved() {
		FormatBuilder fb = new FormatBuilder();
		fb.line("Version count: %s", versions().size());
		fb.line("visibleAllTransactions: %s", visibleAllTransactions != null);
		fb.line("cachedResolution: %s", cachedResolution);
		fb.line("firstCommittedTransactionId: %s", firstCommittedTransactionId);
		fb.line("initialWriteableTransaction: %s", initialWriteableTransaction);
		logger.warn(fb.toString());
	}

	protected int getSize() {
		return this.size.get();
	}

	protected T initialAllTransactionsValueFor(T t) {
		return null;
	}

	protected boolean mayBeReachableFromPreCreationTransactions() {
		return false;
	}

	protected void onResolveNull(boolean write) {
		if (!mayBeReachableFromPreCreationTransactions()) {
			if (notifyResolveNullCount-- >= 0) {
				logger.warn(
						"onResolveNull: \nVersions: {}\nCurrent tx-id: {} - highest visible id: {}\n"
								+ "Visible all tx?: {}\nFirst committed tx-id: {}\nInitial  txid: {}"
								+ "\nInitial writeable tx: {}\nCached resolution: {}",
						versions().keySet(), Transaction.current(),
						Transaction
								.current().highestVisibleCommittedTransactionId,
						visibleAllTransactions != null,
						firstCommittedTransactionId, initialTransactionId,
						initialWriteableTransaction, cachedResolution);
				logger.warn("onResolveNull", new Exception());
			}
		}
	}

	protected void onVersionCreation(ObjectVersion<T> version) {
	}

	/*
	 * Guaranteed that version.transaction does not exist
	 */
	protected void putVersion(ObjectVersion<T> version) {
		if (versions == null) {
			versions = new ConcurrentSkipListMap<>(Collections.reverseOrder());
		}
		versions().put(version.transaction, version);
		size.incrementAndGet();
		Transactions.get().onAddedVacuumable(version.transaction, this);
		updateCached(version.transaction, version.object, version.writeable);
	}

	protected void removeWithSize(Transaction tx) {
		ObjectVersion<T> version = versions().get(tx);
		if (version != null) {
			if (tx.isToDomainCommitted()) {
				visibleAllTransactions = version.object;
			}
			versions().remove(tx);
			cachedResolution = null;
			size.decrementAndGet();
		}
	}

	protected void updateCached(Transaction transaction, T resolved,
			boolean write) {
		CachedResolution cachedResolution = new CachedResolution();
		cachedResolution.readTransactionId = transaction.getId();
		cachedResolution.read = resolved;
		if (write) {
			cachedResolution.writableTransactionId = transaction.getId();
			cachedResolution.writeable = resolved;
		}
		this.cachedResolution = cachedResolution;
	}

	/*
	 *
	 *
	 * the two bulk removal operations effectively use the same 'iterate across
	 * the smallest set' optimisation of AbstractSet, tailored to avoid
	 * CSLM.size()
	 *
	 */
	protected void vacuum0(VacuumableTransactions vacuumableTransactions) {
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
				ObjectVersion<T> version = versions()
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
		if (size.get() == 0) {
			versions = null;
			cachedResolution = null;
			return;
		}
	}

	protected ConcurrentNavigableMap<Transaction, ObjectVersion<T>> versions() {
		ConcurrentNavigableMap versions = this.versions;
		return versions == null ? (ConcurrentNavigableMap) EMPTY : versions;
	}

	boolean hasNoVisibleTransaction() {
		return resolve(false, false) == null;
	}

	boolean hasVisibleVersion() {
		return resolve(false, false) != null;
	}

	T resolve(boolean write) {
		return resolve(write, true);
	}

	T resolve(boolean write, boolean notifyResolveNull) {
		Transaction transaction = Transaction.current();
		if (write && transaction.isReadonly()
				&& !TransformManager.get().isIgnorePropertyChanges()) {
			throw new MvccException("Writing within a readonly transaction");
		}
		// try cached
		CachedResolution cachedResolution = this.cachedResolution;
		if (write) {
			if (cachedResolution != null
					&& cachedResolution.writableTransactionId == transaction
							.getId()) {
				return cachedResolution.writeable;
			}
		} else {
			if (cachedResolution != null
					&& cachedResolution.readTransactionId == transaction
							.getId()) {
				return cachedResolution.read;
			}
		}
		// try non-cached and update cached
		T resolved = resolve0(transaction, write);
		if (resolved == null) {
			onResolveNull(write);
		}
		updateCached(transaction, resolved, write);
		return resolved;
	}

	// sorted maps are nice but also painful (boiling down to "the comparable
	// must be invariant") - this takes care of
	void resolveInvariantToDomainIdentity() {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = new ObjectVersion<>();
		version.object = domainIdentity;
		version.transaction = transaction;
		version.writeable = true;
		versions().put(transaction, version);
		updateCached(transaction, domainIdentity, true);
	}

	void verifyWritable(Transaction transaction) {
		if (notifyInvalidReadStateCount < 0) {
			return;
		}
		CachedResolution cachedResolution = this.cachedResolution;
		boolean verified = cachedResolution != null
				&& cachedResolution.writableTransactionId == transaction
						.getId();
		if ((size.get() == 0 || (transaction.isEmptyCommittedTransactions()
				&& !versions().containsKey(transaction)))) {
			// no
		} else {
			ObjectVersion<T> version = versions().get(transaction);
			if (version != null && version.writeable) {
				verified = true;
			}
		}
		if (!verified) {
			IllegalStateException exception = new IllegalStateException(
					Ax.format("Invalid read state: %s", domainIdentity));
			if (ResourceUtilities.is(MvccObjectVersions.class,
					"throwOnInvalidReadState")) {
				throw exception;
			} else {
				notifyInvalidReadStateCount--;
				logger.warn("Invalid state", exception);
			}
		}
	}

	class CachedResolution {
		private T read;

		private T writeable;

		private TransactionId readTransactionId;

		private TransactionId writableTransactionId;

		@Override
		public String toString() {
			return Ax.format(
					"Read :: tx %s :: value :: %s - Write :: tx %s :: value :: %s",
					readTransactionId, read != null, writableTransactionId,
					writeable != null);
		}
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
		protected void putVersion(ObjectVersion<T> version) {
			((MvccObject) domainIdentity).__setMvccVersions__(this);
			((MvccObject) version.object).__setMvccVersions__(this);
			super.putVersion(version);
		}

		// synchronized on domain identity by vacuum call
		@Override
		protected void vacuum0(VacuumableTransactions vacuumableTransactions) {
			super.vacuum0(vacuumableTransactions);
			if (getSize() == 0) {
				// monitor for creation/removal of
				// domainIdentity.__mvccVersions__. Not used since currently
				// parent monitor is domainIdentity
				// synchronized (domainIdentity) {
				if (initialWriteableTransaction == null) {
					if (visibleAllTransactions != null) {
						if (visibleAllTransactions != domainIdentity) {
							copyObject(visibleAllTransactions, domainIdentity);
						}
						((MvccObject) visibleAllTransactions)
								.__setMvccVersions__(null);
						((MvccObject) domainIdentity).__setMvccVersions__(null);
					}
				}
				// }
			}
		}
	}
}

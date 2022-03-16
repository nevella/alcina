package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;

/**
 * Note that like a TransactionalMap, the owning MvccObject will not be
 * reachable by any transaction started before it is committed to the domain
 * graph (unless the object is created within that transaction).
 *
 * Synchronization - access to transaction versions is synchronized, mostly at
 * the method level. This does not include the fast path access to cached
 * resolution
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

	private static final Object2ObjectAVLTreeMap<Transaction, ObjectVersion> EMPTY = new Object2ObjectAVLTreeMap<Transaction, ObjectVersion>() {
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

	static final Object MVCC_OBJECT__MVCC_OBJECT_VERSIONS_MUTATION_MONITOR = new Object();

	// called in a synchronized block (synchronized on domainIdentity) -- or --
	// unreachable domainIdentity unreachable from other txs
	static <E extends Entity> MvccObjectVersions<E> createEntityVersions(
			E domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		return new MvccObjectVersionsEntity<E>(domainIdentity, transaction,
				initialObjectIsWriteable);
	}

	// called in a synchronized block (synchronized on domainIdentity)
	static MvccObjectVersions<TransactionalTrieEntry> createTrieEntryVersions(
			TransactionalTrieEntry domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		return new MvccObjectVersionsTrieEntry(domainIdentity, transaction,
				initialObjectIsWriteable);
	}

	// concurrency not required
	private Object2ObjectAVLTreeMap<Transaction, ObjectVersion<T>> versions;

	protected T visibleAllTransactions;

	private CachedResolution cachedResolution;

	/*
	 * Only used to determine if a txmap key is not visible to a given
	 * transaction (post vacuum)
	 */
	private TransactionId firstCommittedTransactionId;

	// debugging
	private TransactionId initialTransactionId;

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
		// order is critical here - attach must come late to prevent incomplete
		// object access via Transactions.resolve() - but before
		// onVersionCreation
		putVersion(version);
		attach();
		onVersionCreation(version);
	}

	public T getDomainIdentity() {
		return this.domainIdentity;
	}

	@Override
	public synchronized String toString() {
		try {
			T object = visibleAllTransactions;
			Transaction transaction = null;
			String message = null;
			Iterator<ObjectVersion<T>> itr = versions().values().iterator();
			if (itr.hasNext()) {
				ObjectVersion<T> firstVersion = itr.next();
				object = firstVersion.object;
				transaction = firstVersion.transaction;
			}
			message = Ax.format("versions: %s : base: %s/%s : initial-tx: %s",
					versions().size(), object.getClass(),
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
			return message;
		} catch (Exception e) {
			return "exception..";
		}
	}

	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		vacuum0(vacuumableTransactions);
	}

	/*
	 * Try and return a cached version if possible (if cached version is not
	 * 'writeable' and we're writing, we need to replace it for this tx with a
	 * writeable version). Note that all this is not hit unless the mvcc object
	 * is modified during the jvm lifetime -
	 */
	private synchronized T resolveWithSync(Transaction transaction,
			boolean write) {
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
			removeWithSize(transaction);
			// put before onVersionCreation (which - for entity subtype - will
			// call back into resolve());
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

	protected void attach() {
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

	protected synchronized void debugNotResolved() {
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

	protected synchronized void onResolveNull(boolean write) {
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
	 * Guaranteed that version.transaction does not exist. Synchronized either
	 * by resolve0 or not required since called by constructor
	 */
	protected void putVersion(ObjectVersion<T> version) {
		if (versions == null) {
			versions = new Object2ObjectAVLTreeMap<>(
					Collections.reverseOrder());
		}
		versions().put(version.transaction, version);
		size.incrementAndGet();
		Transactions.get().onAddedVacuumable(version.transaction, this);
		updateCached(version.transaction, version.object, version.writeable);
	}

	/*
	 * Synchronized by resolve0 or vacuum0
	 */
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
	protected synchronized void
			vacuum0(VacuumableTransactions vacuumableTransactions) {
		int maxToRemove = vacuumableTransactions.completedDomainTransactions
				.size()
				+ vacuumableTransactions.completedNonDomainTransactions.size();
		if (size.get() == 0) {
			return;
		}
		/*
		 * clear initialWriteableTransaction if possible
		 */
		boolean hadInitialWriteableTransaction = initialWriteableTransaction != null;
		if (hadInitialWriteableTransaction) {
			if (vacuumableTransactions.completedNonDomainTransactions
					.contains(initialWriteableTransaction)
					|| vacuumableTransactions.completedDomainTransactions
							.contains(initialWriteableTransaction)) {
				initialWriteableTransaction = null;
			}
		}
		/*
		 * loop NonDomainTransactio removal differently, depending on relative
		 * size of the two sets
		 */
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
		/*
		 * try do remove domain transactions
		 */
		if (vacuumableTransactions.oldestVacuumableDomainTransaction != null
				&& versions.size() > 0) {
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
				ObjectBidirectionalIterator<Transaction> bidiItr = versions
						.keySet().iterator(versions.lastKey());
				while (bidiItr.hasPrevious()) {
					removeWithSize(bidiItr.previous());
				}
			}
		}
		/*
		 * Null references if possible (memory)
		 */
		if (size.get() == 0) {
			versions = null;
			cachedResolution = null;
			return;
		}
	}

	protected Object2ObjectAVLTreeMap<Transaction, ObjectVersion<T>>
			versions() {
		Object2ObjectAVLTreeMap versions = this.versions;
		return versions == null ? (Object2ObjectAVLTreeMap) EMPTY : versions;
	}

	boolean hasNoVisibleTransaction() {
		return resolve(false) == null;
	}

	boolean hasVisibleVersion() {
		return resolve(false) != null;
	}

	T resolve(boolean writeableVersion) {
		Transaction transaction = Transaction.current();
		T resolved = resolveWithoutSync(transaction, writeableVersion);
		if (resolved != null) {
			return resolved;
		}
		// try non-cached and update cached
		resolved = resolveWithSync(transaction, writeableVersion);
		if (resolved == null) {
			onResolveNull(writeableVersion);
		}
		updateCached(transaction, resolved, writeableVersion);
		return resolved;
	}

	// sorted maps are nice but also painful (boiling down to "the comparable
	// must be invariant") - this takes care of
	synchronized void resolveInvariantToDomainIdentity() {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = new ObjectVersion<>();
		version.object = domainIdentity;
		version.transaction = transaction;
		version.writeable = true;
		versions().put(transaction, version);
		updateCached(transaction, domainIdentity, true);
	}

	T resolveWithoutSync(Transaction transaction, boolean writeableVersion) {
		if (writeableVersion && transaction.isReadonly()
				&& !TransformManager.get().isIgnorePropertyChanges()) {
			throw new MvccException("Writing within a readonly transaction");
		}
		// try cached
		CachedResolution cachedResolution = this.cachedResolution;
		if (writeableVersion) {
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
		return null;
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

	// T extends MvccObject, but makes the generics harder so not restricted
	// here
	static abstract class MvccObjectVersionsMvccObject<T>
			extends MvccObjectVersions<T> {
		boolean attached;

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
		protected void attach() {
			attached = true;
			((MvccObject) domainIdentity).__setMvccVersions__(this);
		}

		@Override
		protected void putVersion(ObjectVersion<T> version) {
			if (domainIdentity != version.object) {
				((MvccObject) version.object).__setMvccVersions__(this);
			}
			super.putVersion(version);
		}

		@Override
		protected void vacuum0(VacuumableTransactions vacuumableTransactions) {
			synchronized (this) {
				super.vacuum0(vacuumableTransactions);
				if (getSize() == 0) {
					// check not already detached (? should that double-vacuum
					// be possible ?)
					if (attached) {
						// check initial transaction was vacuumed
						if (initialWriteableTransaction == null) {
							// if there's a version visible to all
							// transactions, copy to domainidentity and detach
							//
							//TODO - document when, and when not, visibleAllTransactions == null
							if (visibleAllTransactions != null) {
								/*
								 * The MvccObject has one visible state to all
								 * transactions, so the MvccObjectVersions
								 * instance can be detached from the MvccObject
								 */
								if (visibleAllTransactions != domainIdentity) {
									copyObject(visibleAllTransactions,
											domainIdentity);
									((MvccObject) visibleAllTransactions)
											.__setMvccVersions__(null);
								}
							}
							//removal from the mvccobject will occur in a different (the global) monitor context
							attached = false;
						}
					}
				}
			}
			// !!not!! synchronized on this (avoid deadlock with creation)
			if (!attached) {
				// double-check final detach - an assigning thread may have
				// injected a new MvccObjectVersions, albeit unlikely
				synchronized (MvccObjectVersions.MVCC_OBJECT__MVCC_OBJECT_VERSIONS_MUTATION_MONITOR) {
					if (((MvccObject) domainIdentity)
							.__getMvccVersions__() == this) {
						((MvccObject) domainIdentity).__setMvccVersions__(null);
					}
				}
			}
		}
	}
}

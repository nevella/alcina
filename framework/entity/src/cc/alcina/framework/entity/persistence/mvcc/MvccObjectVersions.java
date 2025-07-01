package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.Configuration;
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
 * Debugging note - attach breakpoints to debugMe() and call it - otherwise the
 * caller will be de-optimised and you'll never hit sync issues
 * 
 *
 * 
 * @param <T>
 */
/*
 * @formatter:off
 * Vacuum notes (for incorporation into doc):
 * * vacuum
    * entity
        * readonly - just discard once tx is finished
        * write - non committing - just discard once tx is finished
        * write - committed - 
            * discard once no tx references the tx of this version (if not latest)
            * if only one version, and creation tx is unreffed (doc), discard via the double-lock-shuffle
 * @formatter:on
 */
public abstract class MvccObjectVersions<T> implements Vacuumable {
	static Logger logger = LoggerFactory.getLogger(MvccObjectVersions.class);

	protected static int notifyResolveNullCount = 100;

	protected static int notifyInvalidReadStateCount = 100;

	private static final Object2ObjectAVLTreeMap<Transaction, ObjectVersion> EMPTY = new Object2ObjectAVLTreeMap<Transaction, ObjectVersion>() {
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectVersion put(Transaction key, ObjectVersion value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void
				putAll(Map<? extends Transaction, ? extends ObjectVersion> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectVersion putIfAbsent(Transaction key, ObjectVersion value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectVersion remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object key, Object value) {
			throw new UnsupportedOperationException();
		}
	};

	/*
	 * This monitor synchronizes operations on the __mvccVersions fields of
	 * *all* domainidentity MvccObjects
	 * 
	 * When checking 'does this versioned object have a new MvccObjects
	 * backing', check the domainIdentity
	 * 
	 * MvccObjects creation (and checking) is avoided where possible via
	 * resolution caching, so this monitor is accessed relatively rarely
	 */
	static final Object MVCC_OBJECT__MVCC_OBJECT_VERSIONS_MUTATION_MONITOR = new Object();

	// called in a synchronized block (synchronized on domainIdentity) -- or --
	// domainIdentity is unreachable(unreachable from other txs)
	static <E extends Entity> MvccObjectVersions<E> createEntityVersions(
			E domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		MvccObjectVersionsEntity<E> versions = new MvccObjectVersionsEntity<E>(
				domainIdentity, transaction, initialObjectIsWriteable);
		ProcessObservers.publish(MvccObservable.VersionsCreationEvent.class,
				() -> new MvccObservable.VersionsCreationEvent(versions,
						initialObjectIsWriteable));
		return versions;
	}

	// called in a synchronized block (synchronized on domainIdentity)
	static MvccObjectVersions<TransactionalTrieEntry> createTrieEntryVersions(
			TransactionalTrieEntry domainIdentity, Transaction transaction,
			boolean initialObjectIsWriteable) {
		MvccObject mvccObject = (MvccObject) domainIdentity;
		return new MvccObjectVersionsTrieEntry(domainIdentity, transaction,
				initialObjectIsWriteable);
	}

	// concurrency not required in the current implementation (where access is
	// synchronized). Stored in reverse order - i.e. the first entry is the most
	// recent tx/version tuple
	private Object2ObjectAVLTreeMap<Transaction, ObjectVersion<T>> versions;

	protected volatile T visibleAllTransactions;

	private volatile CachedResolution cachedResolution;

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
	 * 
	 * NOTE. This constructor should be regarded as effectively final. Because
	 * ordering is so sensitive (in particular, all setup should be performed
	 * before attach()), subclasses should override the called-to methods rather
	 * than the constructor (i.e. they should just call super())
	 */
	MvccObjectVersions(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable,
			// used to pass additional info before subclass constructor is
			// called
			Object context) {
		ObjectVersion<T> version = new ObjectVersion<>();
		version.transaction = initialTransaction;
		domainIdentity = t;
		setVisibleAllTransactions(initialAllTransactionsValueFor(t, context,
				initialTransaction.isBaseTransaction()));
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
		onVersionCreation(version, t);
	}

	//
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

	protected abstract void copyObject(T fromObject, T domainIdenttyObject);

	protected synchronized void debugNotResolved() {
		FormatBuilder fb = new FormatBuilder();
		fb.line("Version count: %s", versions().size());
		fb.line("visibleAllTransactions: %s", visibleAllTransactions != null);
		fb.line("cachedResolution: %s", cachedResolution);
		fb.line("firstCommittedTransactionId: %s", firstCommittedTransactionId);
		fb.line("initialWriteableTransaction: %s", initialWriteableTransaction);
		logger.warn(fb.toString());
	}

	public T getDomainIdentity() {
		return this.domainIdentity;
	}

	protected int getSize() {
		return this.size.get();
	}

	boolean hasNoVisibleTransaction() {
		return resolve(false) == null;
	}

	boolean hasVisibleVersion() {
		return resolve(false) != null;
	}

	protected T initialAllTransactionsValueFor(T t, Object context,
			boolean baseTransaction) {
		return null;
	}

	protected boolean mayBeReachableFromPreCreationTransactions() {
		return false;
	}

	protected synchronized void
			onResolveNull(boolean resolvingWriteableVersion) {
		if (!mayBeReachableFromPreCreationTransactions()) {
			if (notifyResolveNullCount-- >= 0) {
				T debugResolved = resolveWithSync(Transaction.current(),
						resolvingWriteableVersion);
				logger.warn(
						"onResolveNull: \nVersions: {}\nCurrent tx-id: {} - highest visible id: {}\n"
								+ "Visible all tx?: {}\nFirst committed tx-id: {}\nInitial  txid: {}"
								+ "\nInitial writeable tx: {}\nCached resolution: {}\nResolving writeable version: {}",
						versions().keySet(), Transaction.current(),
						Transaction
								.current().highestVisibleCommittedTransactionId,
						visibleAllTransactions != null,
						firstCommittedTransactionId, initialTransactionId,
						initialWriteableTransaction, cachedResolution,
						resolvingWriteableVersion);
				logger.warn("onResolveNull", new Exception());
			}
		}
	}

	protected void onVersionCreation(ObjectVersion<T> version, T copiedFrom) {
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

	protected void removeWithSize(Transaction tx) {
		removeWithSize(tx, true);
	}

	/*
	 * Synchronized by resolve0 or vacuum0
	 */
	protected void removeWithSize(Transaction tx, boolean vacuum) {
		ObjectVersion<T> version = versions().get(tx);
		if (version != null) {
			onVersionRemoval(version);
			if (tx.isToDomainCommitted() && vacuum) {
				setVisibleAllTransactions(version.object);
			}
			versions().remove(tx);
			cachedResolution = null;
			size.decrementAndGet();
			// FIXME - mvcc - cleanup (remove, this is just left here as a
			// reminder )
			// checkIntercept(Type.VERSION_REMOVAL, tx.getId(), null,
			// version.writeable);
		}
	}

	void onVersionRemoval(ObjectVersion<T> version) {
	}

	void onVisibleAllTransactionsUpdated() {
	}

	T resolve(boolean writeableVersion) {
		Transaction transaction = Transaction.current();
		T resolved = resolveWithoutSync(transaction, writeableVersion);
		if (resolved != null) {
			return resolved;
		}
		// try non-cached and update cached
		resolved = resolveWithSync(transaction, writeableVersion);
		if (resolved == null && !mayBeReachableFromPreCreationTransactions()) {
			onResolveNull(writeableVersion);
		}
		updateCached(transaction, resolved, writeableVersion);
		return resolved;
	}

	// sorted maps are nice but also painful (boiling down to "the comparable
	// must be invariant") - this takes care of that
	synchronized void resolveInvariantToDomainIdentity() {
		Transaction transaction = Transaction.current();
		ObjectVersion<T> version = new ObjectVersion<>();
		version.object = domainIdentity;
		version.transaction = transaction;
		version.writeable = true;
		versions().put(transaction, version);
		updateCached(transaction, domainIdentity, true);
	}

	/*
	 * All reached fields must be volatile
	 */
	T resolveWithoutSync(Transaction transaction, boolean writeableVersion) {
		if (writeableVersion && transaction.isReadonly()
				&& !TransformManager.get().isIgnorePropertyChanges()) {
			if (transaction.isBaseTransaction()) {
				if (transaction.isPopulatingPureTransactional()) {
					return visibleAllTransactions;
				}
			}
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
				if (visibleAllTransactions == null) {
					debugMe();
				}
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
			if (visibleAllTransactions == null) {
				/*
				 * This works - I think. But I really need a language to talk
				 * about all the possible cases/states.
				 * 
				 * Repro: jade romcom with tests (once things are compiled I
				 * guess)
				 */
				mostRecentObject = domainIdentity;
			}
		}
		if (write) {
			version = new ObjectVersion<>();
			version.transaction = transaction;
			version.object = copyObject(mostRecentObject);
			version.writeable = true;
			// will remove the readable version, if any
			removeWithSize(transaction, false);
			// put before onVersionCreation (which - for entity subtype - will
			// call back into resolve());
			putVersion(version);
			// }
			onVersionCreation(version, mostRecentObject);
			return version.object;
		} else {
			/*
			 * We could cache - by creating an ObjectVerson for this read call -
			 * but that would require a later vacuum
			 */
			return mostRecentObject;
		}
	}

	void debugMe() {
		System.out.println("debug");
	}

	protected void setVisibleAllTransactions(T value) {
		visibleAllTransactions = value;
		onVisibleAllTransactionsUpdated();
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

	@Override
	public void vacuum(VacuumableTransactions vacuumableTransactions) {
		vacuum0(vacuumableTransactions);
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
		 * loop NonDomainTransaction removal differently, depending on relative
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
		 * try to remove domain transactions
		 */
		if (vacuumableTransactions.oldestVacuumableDomainTransaction != null
				&& versions.size() > 0) {
			Transaction mostRecentCommonVisible = TransactionVersions
					.mostRecentCommonVisible(versions.keySet(),
							vacuumableTransactions.completedDomainTransactions);
			/*
			 * Must change the base before removing transaction (otherwise
			 * there's a narrow window where we'd return an older version)
			 *
			 * Actually no longer true due to synchronisation, but still a good
			 * principle. Also 'change the base'..?
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
			// of accessing incorrect versions (because the removed version is
			// copied to visibleAllTransactions, thence to domainIdentity)
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
					Transaction test = bidiItr.previous();
					if (vacuumableTransactions.completedDomainTransactions
							.contains(test)) {
						removeWithSize(test);
					} else {
						// logically, completedDomainTransactions are
						// sequential, so none more recent than this will be
						// removable
						break;
					}
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
			if (Configuration.is("throwOnInvalidReadState")) {
				throw exception;
			} else {
				notifyInvalidReadStateCount--;
				logger.warn("Invalid state", exception);
			}
		}
	}

	protected Object2ObjectAVLTreeMap<Transaction, ObjectVersion<T>>
			versions() {
		Object2ObjectAVLTreeMap versions = this.versions;
		return versions == null ? (Object2ObjectAVLTreeMap) EMPTY : versions;
	}

	class CachedResolution {
		private volatile T read;

		private volatile T writeable;

		private volatile TransactionId readTransactionId;

		private volatile TransactionId writableTransactionId;

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
		/*
		 * Is this attached to the domain?
		 */
		boolean attached;

		/*
		 * Must be called synchronized on versions
		 */
		boolean isAttached() {
			return attached && domainIdentity != null;
		}

		// See note in super - this constructor should (and does) only call
		// super()
		MvccObjectVersionsMvccObject(T t, Transaction initialTransaction,
				boolean initialObjectIsWriteable) {
			super(t, initialTransaction, initialObjectIsWriteable, null);
			/*
			 * No! See note in super (this caused a a hard-to-track resolution
			 * issue)
			 */
			// if (!initialObjectIsWriteable) {
			// // creating a versions object from a committed domainIdentity
			// // object.
			// //
			// // DOC : this is called either from create()
			// // (Transaction.create(), TransactionalTrieEntry constructor;
			// // both with initialObjectIsWriteable) or resolve( with
			// // false)(from a visible object with no mvccobjectversions i.e.
			// // visible to all txs)
			// setVisibleAllTransactions(domainIdentity);
			// }
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
			boolean initialObjectIsWriteable = this.initialWriteableTransaction != null;
			if (!initialObjectIsWriteable && visibleAllTransactions == null) {
				// creating a versions object from a committed domainIdentity
				// object.
				//
				// DOC : this is called either from create()
				// (Transaction.create(), TransactionalTrieEntry constructor;
				// both with initialObjectIsWriteable) or resolve( with
				// false)(from a visible object with no mvccobjectversions i.e.
				// visible to all txs)
				//
				// Note - moved from constructor
				setVisibleAllTransactions(domainIdentity);
			}
		}

		@Override
		protected void vacuum0(VacuumableTransactions vacuumableTransactions) {
			synchronized (this) {
				super.vacuum0(vacuumableTransactions);
				if (getSize() == 0) {
					// check not already detached (? should that double-vacuum
					// be possible ?)
					/*
					 * tmp - disable as test of job sys issues
					 */
					if (attached) {
						// check initial transaction was vacuumed
						if (initialWriteableTransaction == null) {
							// if there's a version visible to all
							// transactions, copy to domainidentity and detach
							//
							// TODO - document when, and when not,
							// visibleAllTransactions == null
							if (visibleAllTransactions != null) {
								/*
								 * The MvccObject has one visible state to all
								 * transactions, so the MvccObjectVersions
								 * instance can be detached from the MvccObject
								 */
								if (visibleAllTransactions != domainIdentity) {
									copyObject(visibleAllTransactions,
											domainIdentity);
								}
							}
							// removal from the mvccobject will occur in a
							// different (the global) monitor context
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
						publishRemoval();
					}
				}
			}
		}

		void publishRemoval() {
		}

		void onDomainTransactionCommited() {
		}

		void onDomainTransactionDbPersisted() {
		}
	}
}

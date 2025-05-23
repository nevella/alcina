package cc.alcina.framework.entity.persistence.mvcc;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * DOCUMENT - there are some slight differences between 'ensure ended' and
 * 'ensure begun' - particularly around state TO_DB_ABORTED.
 *
 * This behavioural difference serves to make ABORT (transform commit error)
 * handling more intentional - a simple 'ensureBegun' won't be enough - see e.e.
 * JobRegistry.performJob0
 *
 * Note that ordering is only meaningful for committed transactions (all uses of
 * tx ordering must respect that).
 *
 * =================
 *
 * Performance notes:
 *
 * The synchronization of resolvedMostRecentVisibleTransactions (a per-tx mvcc
 * resolution lookup) only kicks in when accessed multithreaded (i.e. in a
 * readonly transaction). resolvedMostRecentVisibleTransactions is only used
 * when a given MvccObject has multiiple versions - which is rare _except_
 * during bulk updates - which are themselves definitely run single-threaded, so
 * the logic of the optimisation holds.
 */
public class Transaction implements Comparable<Transaction> {
	public static final String CONTEXT_ALLOW_ABORTED_TX_ACCESS = Transaction.class
			.getName() + ".CONTEXT_ALLOW_ABORTED_TX_ACCESS";

	static boolean retainStartEndTraces;

	private static ThreadLocal<Supplier<Transaction>> threadLocalSupplier = new ThreadLocal() {
	};

	static Logger logger = LoggerFactory.getLogger(Transaction.class);

	// optimisation check - seems a lot of time spent in the threadlocal check
	private static Map<Thread, Transaction> perThreadTransaction = new ConcurrentHashMap<>(
			1000);

	public static void begin() {
		begin(TransactionPhase.TO_DB_PREPARING);
	}

	static void begin(TransactionPhase initialPhase) {
		begin(initialPhase, null);
	}

	static void begin(TransactionPhase initialPhase,
			Transaction copyVisibleTransactionsFrom) {
		if (!Transactions.isInitialised()) {
			return;
		}
		if (getPerThreadTransaction() != null) {
			throw new MvccException(Ax.format("Begin without end: %s - %s",
					initialPhase, getPerThreadTransaction()));
		}
		switch (initialPhase) {
		case TO_DB_PREPARING:
		case TO_DOMAIN_PREPARING:
		case VACUUM_BEGIN:
			break;
		default:
			throw new UnsupportedOperationException();
		}
		Transaction transaction = new Transaction(initialPhase,
				copyVisibleTransactionsFrom);
		logger.debug("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		transaction.originatingThread = Thread.currentThread();
		setThreadLocalTransaction(transaction);
		transaction.originatingThreadName = Thread.currentThread().getName();
		synchronized (transaction.resolvedMostRecentVisibleTransactions) {
			transaction.threadCount++;
		}
		if (retainStartEndTraces()) {
			transaction.transactionStartTrace = SEUtilities
					.getCurrentThreadStacktraceSlice();
		}
	}

	public static void beginDomainPreparing() {
		begin(TransactionPhase.TO_DOMAIN_PREPARING);
	}

	/**
	 * <p>
	 * Essentially run within another tx to access graph state before any
	 * changes in the current tx were applied
	 *
	 * <p>
	 * <b>Note</b> - don't attempt to get any entities without checking that
	 * they were persisted prior to the current transaction. If working in an
	 * {@code AdjunctTransformCollation}, one way to check that is
	 * {@code QueryResult.hasCreateTransform()}
	 */
	public static <T> T callInSnapshotTransaction(Callable<T> callable)
			throws Exception {
		Transaction preSnapshot = current();
		Preconditions.checkNotNull(preSnapshot);
		removeThreadLocalTransaction();
		begin(TransactionPhase.TO_DB_PREPARING, preSnapshot);
		current().toReadonly(true);
		T t = callable.call();
		end();
		setThreadLocalTransaction(preSnapshot);
		return t;
	}

	public static int commit() {
		Transaction transaction = provideCurrentThreadTransaction();
		try {
			if (transaction == null) {
				Preconditions
						.checkState(!TransformManager.get().hasTransforms());
				return 0;
			} else {
				Preconditions.checkState(transaction.isWriteable()
						|| TransformManager.get().getTransforms().isEmpty());
				int transformCount = TransformCommit.commitTransformsAsRoot();
				return transformCount;
			}
		} catch (IllegalStateException e) {
			logger.info("Uncommitted transforms:");
			logger.info(TransformManager.get().getTransforms().toString());
			throw e;
		}
	}

	public static int commitIfTransformCount(int n) {
		if (TransformManager.get().getTransforms().size() > n) {
			return commit();
		} else {
			return 0;
		}
	}

	public static DomainTransformLayerWrapper commitReturnResults() {
		return TransformCommit.commitTransforms(null, true, true);
	}

	/*
	 * For fancy before-and-after (off-store indexing)
	 */
	public static Transaction createSnapshotTransaction() {
		Transaction preSnapshot = current();
		Preconditions.checkNotNull(preSnapshot);
		removeThreadLocalTransaction();
		begin();
		Transaction snapshot = current();
		snapshot.toReadonly(true);
		split();
		setThreadLocalTransaction(preSnapshot);
		return snapshot;
	}

	public static Transaction current() {
		Transaction transaction = provideCurrentThreadTransaction();
		if (transaction == null
				|| (transaction.getPhase() == TransactionPhase.TO_DB_ABORTED
						&& !LooseContext.is(CONTEXT_ALLOW_ABORTED_TX_ACCESS))) {
			throw new MvccException("No current transaction");
		} else {
			return transaction;
		}
	}

	private static Transaction currentNoThrow() {
		Transaction transaction = provideCurrentThreadTransaction();
		if (transaction == null
				|| (transaction.getPhase() == TransactionPhase.TO_DB_ABORTED
						&& !LooseContext.is(CONTEXT_ALLOW_ABORTED_TX_ACCESS))) {
			return null;
		} else {
			return transaction;
		}
	}

	/*
	 * Called in locations a transaction *should* be active, but isn't
	 */
	public static void debugCurrentThreadTransaction() {
		Transaction currentThreadTransaction = provideCurrentThreadTransaction();
		if (currentThreadTransaction == null) {
			logger.warn("DEVEX - 0 - no current thread transaction");
		} else {
			logger.warn("DEVEX - 0 - current thread transaction:\n{}",
					currentThreadTransaction.toDebugString());
		}
	}

	public static void end() {
		if (!Transactions.isInitialised()) {
			return;
		}
		Transaction perThreadTransaction = getPerThreadTransaction();
		if (perThreadTransaction == null) {
			logger.error(
					"Attempting to end transaction when one is not present");
		}
		perThreadTransaction.endTransaction();
		logger.debug("Removing tx - {} {} {}", perThreadTransaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		removeThreadLocalTransaction();
	}

	public static void endAndBeginNew() {
		/*
		 * Definitely don't 'reuse' - because transactions are sequence
		 * dependent (if we don't start a new transaction, commits between start
		 * of old transaction and now won't be visible)
		 */
		// if (TransformManager.get().getTransforms().size() == 0
		// && current().getPhase() == TransactionPhase.TO_DB_PREPARING) {
		// return;// reuse
		// }
		//
		/*
		 * also, to reduce error checking higher in the stack, use 'ensureEnded'
		 * - the goal of this method is to have a fresh tx
		 */
		ensureEnded();
		begin();
	}

	public static void ensureBegun() {
		Transaction perThreadTransaction = getPerThreadTransaction();
		if (perThreadTransaction != null && perThreadTransaction.isEnded()) {
			removeThreadLocalTransaction();
			perThreadTransaction = null;
		}
		if (perThreadTransaction == null) {
			begin();
		}
	}

	public static void ensureDomainPreparingActive() {
		if (getPerThreadTransaction() == null) {
			begin(TransactionPhase.TO_DOMAIN_PREPARING);
		}
	}

	public static void ensureEnded() {
		if (getPerThreadTransaction() != null) {
			end();
		}
	}

	static class CachedPerThreadTransaction {
		Thread thread;

		CachedPerThreadTransaction(Thread thread, Transaction transaction) {
			this.thread = thread;
			this.transaction = transaction;
		}

		Transaction transaction;
	}

	static CachedPerThreadTransaction cachedPerThreadTransaction;

	/*
	 * This call is cached (to avoid even the low cost of a concurrenthashmap
	 * lookup if possible), since the most common usage pattern is by a single
	 * thread
	 */
	private static Transaction getPerThreadTransaction() {
		Thread currentThread = Thread.currentThread();
		CachedPerThreadTransaction cached = cachedPerThreadTransaction;
		if (cached != null && cached.thread == currentThread) {
			return cached.transaction;
		}
		Transaction transaction = perThreadTransaction.get(currentThread);
		cachedPerThreadTransaction = new CachedPerThreadTransaction(
				currentThread, transaction);
		return transaction;
	}

	public static boolean isInActiveTransaction() {
		Transaction currentThreadTransaction = provideCurrentThreadTransaction();
		return currentThreadTransaction != null
				&& !currentThreadTransaction.getPhase().isComplete();
	}

	public static boolean isInTransaction() {
		return perThreadTransaction.containsKey(Thread.currentThread());
	}

	/*
	 * Note that this means the same transaction graph is visible to multiple
	 * threads. Either make the tx read-only or do all writes on the originating
	 * thread after all worker threads have 'split' from this 'join'.
	 *
	 */
	public static void join(Transaction transaction) {
		logger.debug("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		Preconditions.checkState(!isInTransaction());
		synchronized (transaction.resolvedMostRecentVisibleTransactions) {
			transaction.threadCount++;
		}
		setThreadLocalTransaction(transaction);
	}

	private static Transaction provideCurrentThreadTransaction() {
		Transaction transaction = getPerThreadTransaction();
		if (transaction == null) {
			Supplier<Transaction> supplier = threadLocalSupplier.get();
			if (supplier != null) {
				transaction = supplier.get();
			}
		}
		return transaction;
	}

	static void reapUnreferencedTransactions() {
		perThreadTransaction.keySet().removeIf(t -> !t.isAlive());
		cachedPerThreadTransaction = null;
	}

	public static void removePerThreadContext() {
		removeThreadLocalTransaction();
	}

	private static void removeThreadLocalTransaction() {
		cachedPerThreadTransaction = null;
		perThreadTransaction.remove(Thread.currentThread());
	}

	private static boolean retainStartEndTraces() {
		return retainStartEndTraces;
	}

	public static void setSupplier(Supplier<Transaction> transactionSupplier) {
		threadLocalSupplier.set(transactionSupplier);
	}

	private static void setThreadLocalTransaction(Transaction transaction) {
		cachedPerThreadTransaction = null;
		perThreadTransaction.put(Thread.currentThread(), transaction);
	}

	// inverse of join
	public static void split() {
		Transaction transaction = getPerThreadTransaction();
		logger.debug("Removing tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		removeThreadLocalTransaction();
		synchronized (transaction.resolvedMostRecentVisibleTransactions) {
			transaction.threadCount--;
		}
	}

	// used as a monitor for threadCount modification (not access)
	private Map<MvccObjectVersions, Transaction> resolvedMostRecentVisibleTransactions = new Object2ObjectOpenHashMap<>(
			Hash.DEFAULT_INITIAL_SIZE, Hash.FAST_LOAD_FACTOR);

	Thread originatingThread;

	// a volatile int rather than AtomicInteger to try and fix incorrect sync in
	// mostRecentVisibleCommittedTransaction
	private volatile int threadCount;

	private long transformRequestId;

	private String transactionStartTrace;

	private String transactionEndTrace;

	private String originatingThreadName;

	boolean ended;

	// for debugging
	@SuppressWarnings("unused")
	private Timestamp databaseCommitTimestamp;

	private boolean baseTransaction;

	SortedSet<Transaction> committedTransactions;

	TransactionId id;

	TransactionPhase phase;

	long startTime;

	boolean publishedLongRunningTxWarning;

	TransactionId highestVisibleCommittedTransactionId;

	// if zero, use system default (for abort)
	private long maxAge;

	Boolean emptyCommitted = null;

	DomainTransformCommitPosition commitPosition;

	List<Entity> createdLocalsForEviction = new ArrayList<>();

	/**
	 * Rare case, when a map should be purely transactional for iterator/remove
	 * reasons - during warmup, set this to true while populating
	 */
	private boolean populatingPureTransactional;

	TransactionId dbTransactionId;

	private Transaction(TransactionPhase initialPhase,
			Transaction copyVisibleTransactionsFrom) {
		this.phase = initialPhase;
		Transactions.get().initialiseTransaction(this,
				copyVisibleTransactionsFrom);
		logger.debug("Created tx: {}", this);
	}

	public void clearLocalEvictionList() {
		createdLocalsForEviction.clear();
	}

	@Override
	public int compareTo(Transaction o) {
		return id.compareTo(o.id);
	}

	public <T extends Entity> T create(Class<T> clazz, DomainStore store,
			long objectId, long localId) {
		T t = store.getMvcc().create(clazz);
		if (t == null) {
			try {
				// non-transactional entity
				T newInstance = clazz.getDeclaredConstructor().newInstance();
				newInstance.setId(objectId);
				newInstance.setLocalId(localId);
				return newInstance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		t.setLocalId(localId);
		t.setId(objectId);
		/*
		 * If creating outside the base transaction, we'll immediately want a
		 * writable version - so create it now. No need to synchronize
		 * MvccObjectVersions.ensureEntity since this is a newly created object
		 */
		if (!isBaseTransaction()) {
			MvccObjectVersions<T> versions = MvccObjectVersions
					.createEntityVersions(t, this, true);
		}
		if (t.domain().isLocal()) {
			createdLocalsForEviction.add(t);
		}
		return t;
	}

	void endTransaction() {
		originatingThread = null;
		ended = true;
		TransactionPhase endPhase = getPhase();
		TransformManager transformManager = TransformManager.get();
		if (transformManager == null) {
			return;
		}
		Set<DomainTransformEvent> transforms = transformManager.getTransforms();
		switch (endPhase) {
		case TO_DB_PERSISTED:
		case TO_DB_ABORTED:
		case TO_DOMAIN_COMMITTED:
		case TO_DOMAIN_ABORTED:
		case VACUUM_ENDED:
		case READ_ONLY:
		case TO_DB_PREPARING:
		case NON_COMMITAL:
			break;
		default:
			// we used to throw to an exception if there were
			// uncommitted transforms but we can't allow dangling
			// transactions - we can end up here if an exception is thrown on
			// postProcess()
			if (AppPersistenceBase.isTestServer()) {
				throw new MvccException(
						Ax.format("Ending on invalid phase: %s %s transforms",
								endPhase, transforms.size()));
			} else {
				logger.warn("Ending transaction on invalid phase: {}",
						endPhase);
			}
		}
		if (isWriteable()) {
			if (transforms.size() == 0) {
			} else {
				// FIXME - mvcc.5 - mvcc exception (after cleanup)
				switch (endPhase) {
				case NON_COMMITAL:
					// no warning, transforms allowed - in fact expected
					break;
				default:
					logger.warn(
							"Ending transaction with uncommitted transforms: {} {}",
							endPhase, transforms.size());
					if (Ax.isTest()) {
						throw new RuntimeException(Ax.format(
								"Uncommitted transforms\n\n%s", transforms));
					}
					break;
				}
			}
		}
		// need to do this even if transforms == 0 - to clear listeners
		// setup during the transaction
		//
		// but only if tx is not readonly
		//
		if (endPhase != TransactionPhase.READ_ONLY) {
			try {
				LooseContext.pushWithTrue(CONTEXT_ALLOW_ABORTED_TX_ACCESS);
				switch (endPhase) {
				case NON_COMMITAL:
					ThreadlocalTransformManager.cast().resetTltmNonCommitalTx();
					break;
				default:
					if (createdLocalsForEviction.size() > 0) {
						ThreadlocalTransformManager.cast()
								.evictNonPromotedLocals(
										createdLocalsForEviction);
					}
					ThreadlocalTransformManager.cast().resetTltm(null);
					break;
				}
			} finally {
				LooseContext.pop();
			}
		}
		if (retainStartEndTraces()) {
			transactionEndTrace = SEUtilities.getCurrentThreadStacktraceSlice();
		}
		logger.debug("Ended tx: {}", this);
		Transactions.get().onTransactionEnded(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Transaction) {
			Transaction other = (Transaction) obj;
			return this.id == other.id;
		} else {
			return super.equals(obj);
		}
	}

	public DomainTransformCommitPosition getCommitPosition() {
		return this.commitPosition;
	}

	public TransactionId getId() {
		return this.id;
	}

	TransactionPhase getPhase() {
		return this.phase;
	}

	long getTransformRequestId() {
		return this.transformRequestId;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean isBaseTransaction() {
		return this.baseTransaction;
	}

	public boolean isEmptyCommittedTransactions() {
		if (emptyCommitted == null) {
			emptyCommitted = committedTransactions.isEmpty();
		}
		return emptyCommitted;
	}

	public boolean isEnded() {
		return this.ended;
	}

	public boolean isNonCommital() {
		return this.phase == TransactionPhase.NON_COMMITAL;
	}

	public boolean isPopulatingPureTransactional() {
		return this.populatingPureTransactional;
	}

	public boolean isPreCommit() {
		return phase == TransactionPhase.TO_DB_PREPARING;
	}

	boolean isReadonly() {
		return threadCount != 1 || phase == TransactionPhase.READ_ONLY;
	}

	public boolean isReadOnly() {
		return this.phase == TransactionPhase.READ_ONLY;
	}

	public boolean isToDomainCommitted() {
		return phase == TransactionPhase.TO_DOMAIN_COMMITTED;
	}

	public boolean isToDomainCommitting() {
		return phase == TransactionPhase.TO_DOMAIN_COMMITTING;
	}

	public boolean isVisible(TransactionId committedTransactionId) {
		return committedTransactionId != null
				&& committedTransactionId.id <= highestVisibleCommittedTransactionId.id;
	}

	public boolean isWriteable() {
		return !isReadonly();
	}

	/*
	 * Cache the result (for a given object, and a given transaction, it may be
	 * called many times). Non-synchronized if single-threaded
	 */
	Transaction
			mostRecentVisibleCommittedTransaction(MvccObjectVersions versions) {
		// Defensively synchronized - this sort of logic doesn't work anyway
		// (although a CAS would)
		// if (threadCount == 1) {
		// return resolvedMostRecentVisibleTransactions
		// .computeIfAbsent(versions,
		// v -> TransactionVersions.mostRecentCommonVisible(
		// v.versions().keySet(),
		// committedTransactions));
		// } else {
		synchronized (resolvedMostRecentVisibleTransactions) {
			return resolvedMostRecentVisibleTransactions
					.computeIfAbsent(versions,
							v -> TransactionVersions.mostRecentCommonVisible(
									v.versions().keySet(),
									committedTransactions));
		}
		// }
	}

	public long provideAge() {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * For when an entity will be committed, but on a different (say backend
	 * queue) thread
	 */
	public void removeFromLocalEviction(Entity entity) {
		createdLocalsForEviction.remove(entity);
	}

	/**
	 * It's legal to do this after DomainStore.warmup as long as you can
	 * guarantee that tx object modification is thread-safe
	 */
	public void setBaseTransaction(boolean baseTransaction) {
		this.baseTransaction = baseTransaction;
	}

	void setId(TransactionId id) {
		this.id = id;
	}

	void setPhase(TransactionPhase phase) {
		if (Configuration.is("debugSetPhase")) {
			logger.info("{}->{} ::\n{}", this.phase, phase,
					SEUtilities.getCurrentThreadStacktraceSlice());
		}
		this.phase = phase;
		logger.debug("Transition tx: {}", this);
	}

	/*
	 * Allow modification during a readonly transaction ()
	 */
	public void setPopulatingPureTransactional(
			boolean populatingPureTransactional) {
		this.populatingPureTransactional = populatingPureTransactional;
	}

	public void setMaxAge(long maxAge) {
		logger.debug("{} :: Setting maxAge to {}", this, maxAge);
		this.maxAge = maxAge;
	}

	public void toDbAborted() {
		// allow 'to aborted' when already aborted - for bubbling exception
		// handling
		Preconditions.checkState(getPhase() == TransactionPhase.TO_DB_PERSISTING
				|| getPhase() == TransactionPhase.TO_DB_PREPARING
				|| getPhase() == TransactionPhase.TO_DB_ABORTED
				|| getPhase() == TransactionPhase.READ_ONLY
				|| getPhase() == TransactionPhase.NON_COMMITAL);
		setPhase(TransactionPhase.TO_DB_ABORTED);
	}

	public void toDbPersisted(Timestamp timestamp) {
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DB_PERSISTING);
		this.databaseCommitTimestamp = timestamp;
		setPhase(TransactionPhase.TO_DB_PERSISTED);
		Transactions.get().onDomainTransactionDbPersisted(this);
	}

	public void toDbPersisting() {
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DB_PREPARING);
		setPhase(TransactionPhase.TO_DB_PERSISTING);
	}

	public String toDebugString() {
		String detail = String.format("%10s %14s %14s %s", id, phase,
				new Date(startTime), originatingThreadName);
		if (transactionStartTrace != null) {
			detail = Ax.format("%s\n-------\n%s\n", detail,
					CommonUtils.hangingIndent(transactionStartTrace, false, 2));
		}
		if (transactionEndTrace != null) {
			detail = Ax.format("%s\n-------\n%s\n", detail,
					CommonUtils.hangingIndent(transactionEndTrace, false, 2));
		}
		return detail;
	}

	public void toDomainAborted() {
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DOMAIN_COMMITTING
						|| getPhase() == TransactionPhase.TO_DB_PREPARING);
		setPhase(TransactionPhase.TO_DOMAIN_ABORTED);
	}

	public void
			toDomainCommitted(DomainTransformCommitPosition commitPosition) {
		this.commitPosition = commitPosition;
		Preconditions.checkState(
				getPhase() == TransactionPhase.TO_DOMAIN_COMMITTING);
		setPhase(TransactionPhase.TO_DOMAIN_COMMITTED);
		Transactions.get().onDomainTransactionCommited(this);
	}

	public void toDomainCommitting(Timestamp timestamp, DomainStore store,
			long sequenceId, long transformRequestId,
			TransactionId dbTransactionId) {
		this.transformRequestId = transformRequestId;
		this.dbTransactionId = dbTransactionId;
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DOMAIN_PREPARING
						&& ThreadlocalTransformManager.get().getTransforms()
								.isEmpty());
		this.databaseCommitTimestamp = timestamp;
		setPhase(TransactionPhase.TO_DOMAIN_COMMITTING);
	}

	public void toNonCommital() {
		Preconditions.checkState((phase == TransactionPhase.TO_DB_PREPARING
				&& TransformManager.get().getTransforms().isEmpty()));
		this.phase = TransactionPhase.NON_COMMITAL;
	}

	public void toReadonly() {
		toReadonly(false);
	}

	private void toReadonly(boolean allowExistingTransforms) {
		if (this.phase == TransactionPhase.READ_ONLY) {
			return;
		}
		Preconditions.checkState((phase == TransactionPhase.TO_DB_PREPARING
				|| phase == TransactionPhase.TO_DOMAIN_PREPARING)
				&& (allowExistingTransforms
						|| TransformManager.get().getTransforms().isEmpty()));
		this.phase = TransactionPhase.READ_ONLY;
	}

	@Override
	public String toString() {
		return Ax.format("%s::%s", id, phase);
	}

	public void toTimedOut() {
		logger.warn(
				"Transaction timed out.\n\tId: {}\n\tStart: {}\n\tNow: {}\n\tThread: {}\n\nStack:\n{}\n\nEnd stack:\n{}",
				id, startTime, System.currentTimeMillis(),
				originatingThreadName, transactionStartTrace,
				transactionEndTrace);
		switch (phase) {
		case TO_DB_PREPARING:
			toDbAborted();
			break;
		case TO_DOMAIN_COMMITTING:
		case TO_DOMAIN_PREPARING:
			// this is possible because the corresponding domain tx hasn't been
			// committed;
			toDomainAborted();
			break;
		case TO_DB_PERSISTING:
			// this is ... dodgy, since we *may* be able to cancel (if the db
			// isn't too far along), but may not.
			//
			break;
		case VACUUM_BEGIN:
			// well..we'd be in trouble here;
		default:
			throw new UnsupportedOperationException(Ax.format(
					"Cannot cancel transaction %s in phase %s", id, phase));
		}
	}

	void toVacuumEnded(List<Transaction> vacuumableTransactions) {
		Preconditions.checkState(getPhase() == TransactionPhase.VACUUM_BEGIN);
		Transactions.get().vacuumComplete(vacuumableTransactions);
		setPhase(TransactionPhase.VACUUM_ENDED);
	}

	/*
	 * will be sorted, most recent to now
	 */
	List<Transaction> visibleCommittedTransactions(
			NavigableSet<Transaction> otherCommittedTransactionsSet) {
		return TransactionVersions.commonVisible(committedTransactions,
				otherCommittedTransactionsSet);
	}

	/**
	 * <p>
	 * Intended for multi-threaded objects such as jobs, this debounces 'start a
	 * new tx to check for changes' to prevent 1000s of new transactions per
	 * second, while having a reasonable cross-thread object update period
	 * 
	 * @param ageMs
	 *            the maxage of the transaction before beginning a new one
	 */
	public static void ensureAndRestartIfOlderThan(long ageMs) {
		ensureBegun();
		Transaction current = currentNoThrow();
		if (current == null
				|| !TimeConstants.within(current.startTime, ageMs)) {
			endAndBeginNew();
		}
	}

	public static long getMaxAgeOrDefault() {
		ensureBegun();
		Transaction current = currentNoThrow();
		return current == null || current.maxAge == 0 ? getDefaultMaxAge()
				: current.maxAge;
	}

	private static transient long defaultMaxAge = -1;

	public static long getDefaultMaxAge() {
		if (defaultMaxAge == -1) {
			defaultMaxAge = Configuration.getInt("maxAgeSecs")
					* TimeConstants.ONE_SECOND_MS;
		}
		return defaultMaxAge;
	}

	public long getMaxAge() {
		return maxAge;
	}
}

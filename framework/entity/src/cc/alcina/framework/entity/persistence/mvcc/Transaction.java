package cc.alcina.framework.entity.persistence.mvcc;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class Transaction {
	public static final String CONTEXT_RETAIN_TRANSACTION_TRACES = Transaction.class
			.getName() + ".CONTEXT_RETAIN_TRANSACTION_TRACES";

	private static ThreadLocal<Transaction> threadLocalInstance = new ThreadLocal() {
	};

	static Logger logger = LoggerFactory.getLogger(Transaction.class);

	public static void begin() {
		begin(TransactionPhase.TO_DB_PREPARING);
	}

	public static void beginDomainPreparing() {
		begin(TransactionPhase.TO_DOMAIN_PREPARING);
	}

	public static int commit() {
		int transformCount = TransformCommit.commitTransformsAsRoot();
		return transformCount;
	}

	public static int commitIfTransformCount(int n) {
		if (TransformManager.get().getTransforms().size() > n) {
			return commit();
		} else {
			return 0;
		}
	}

	public static Transaction current() {
		Transaction transaction = threadLocalInstance.get();
		if (transaction == null
				|| transaction.getPhase() == TransactionPhase.TO_DB_ABORTED) {
			throw new MvccException("No current transaction");
		} else {
			return transaction;
		}
	}

	public static void end() {
		if (!Transactions.isInitialised()) {
			return;
		}
		threadLocalInstance.get().endTransaction();
		logger.debug("Removing tx - {} {} {}", threadLocalInstance.get(),
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.remove();
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
		if (threadLocalInstance.get() == null) {
			begin();
		}
	}

	public static void ensureDomainPreparingActive() {
		if (threadLocalInstance.get() == null) {
			begin(TransactionPhase.TO_DOMAIN_PREPARING);
		}
	}

	public static void ensureEnded() {
		if (threadLocalInstance.get() != null) {
			end();
		}
	}

	public static boolean isInTransaction() {
		return threadLocalInstance.get() != null;
	}

	/*
	 * Note that this means the same transaction graph is visible to multiple
	 * threads. Either make the tx read-only or do all writes on the originating
	 * thread after all worker threads have 'split' from this 'join'
	 */
	public static void join(Transaction transaction) {
		logger.debug("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		transaction.threadCount.incrementAndGet();
		threadLocalInstance.set(transaction);
	}

	// inverse of join
	public static void split() {
		Transaction transaction = threadLocalInstance.get();
		logger.debug("Removing tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.remove();
		transaction.threadCount.decrementAndGet();
	}

	private static boolean retainStartEndTraces() {
		return ResourceUtilities.is("retainTraces")
				|| LooseContext.is(CONTEXT_RETAIN_TRANSACTION_TRACES);
	}

	static void begin(TransactionPhase initialPhase) {
		if (!Transactions.isInitialised()) {
			return;
		}
		if (threadLocalInstance.get() != null) {
			throw new MvccException(Ax.format("Begin without end: %s - %s",
					initialPhase, threadLocalInstance.get()));
		}
		switch (initialPhase) {
		case TO_DB_PREPARING:
		case TO_DOMAIN_PREPARING:
		case VACUUM_BEGIN:
			break;
		default:
			throw new UnsupportedOperationException();
		}
		Transaction transaction = new Transaction(initialPhase);
		logger.debug("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		transaction.originatingThread = Thread.currentThread();
		threadLocalInstance.set(transaction);
		transaction.originatingThreadName = Thread.currentThread().getName();
		transaction.threadCount.incrementAndGet();
		if (retainStartEndTraces()) {
			transaction.transactionStartTrace = SEUtilities
					.getCurrentThreadStacktraceSlice();
		}
	}

	Thread originatingThread;

	private AtomicInteger threadCount = new AtomicInteger();

	private long transformRequestId;

	private String transactionStartTrace;

	private String transactionEndTrace;

	private String originatingThreadName;

	boolean ended;

	private Timestamp databaseCommitTimestamp;

	private boolean baseTransaction;

	// field type is the actual type because we call lastKey()
	Object2ObjectLinkedOpenHashMap<TransactionId, Transaction> committedTransactions = new Object2ObjectLinkedOpenHashMap<>();

	private TransactionId id;

	private Map<DomainStore, StoreTransaction> storeTransactions = new LightMap();

	TransactionPhase phase;

	long startTime;

	boolean publishedLongRunningTxWarning;

	public Transaction(TransactionPhase initialPhase) {
		DomainStore.stores().stream().forEach(store -> storeTransactions
				.put(store, new StoreTransaction(store)));
		this.phase = initialPhase;
		Transactions.get().initialiseTransaction(this);
		startTime = System.currentTimeMillis();
		logger.debug("Created tx: {}", this);
	}

	public <T extends Entity> T create(Class<T> clazz, DomainStore store) {
		StoreTransaction storeTransaction = storeTransactions.get(store);
		T t = storeTransaction.getMvcc().create(clazz);
		if (t == null) {
			try {
				// non-transactional entity
				return clazz.newInstance();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		if (!isBaseTransaction()) {
			MvccObjectVersions<T> versions = MvccObjectVersions.ensureEntity(t,
					this, true);
			((MvccObject<T>) t).__setMvccVersions__(versions);
		}
		return t;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Transaction) {
			Transaction other = (Transaction) obj;
			return Objects.equals(this.id, other.id);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean isBaseTransaction() {
		return this.baseTransaction;
	}

	public boolean isEnded() {
		return this.ended;
	}

	public boolean isPreCommit() {
		return phase == TransactionPhase.TO_DB_PREPARING;
	}

	public boolean isToDomainCommitting() {
		return phase == TransactionPhase.TO_DOMAIN_COMMITTING;
	}

	public long provideAge() {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * It's legal to do this after DomainStore.warmup as long as you can
	 * guarantee that tx object modification is thread-safe
	 */
	public void setBaseTransaction(boolean baseTransaction) {
		this.baseTransaction = baseTransaction;
	}

	public void toDbAborted() {
		// allow 'to aborted' when already aborted - for bubbling exception
		// handling
		Preconditions.checkState(getPhase() == TransactionPhase.TO_DB_PERSISTING
				|| getPhase() == TransactionPhase.TO_DB_PREPARING
				|| getPhase() == TransactionPhase.TO_DB_ABORTED);
		setPhase(TransactionPhase.TO_DB_ABORTED);
	}

	public void toDbPersisted(Timestamp timestamp) {
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DB_PERSISTING);
		this.databaseCommitTimestamp = timestamp;
		setPhase(TransactionPhase.TO_DB_PERSISTED);
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

	public void toDomainCommitted() {
		Preconditions.checkState(
				getPhase() == TransactionPhase.TO_DOMAIN_COMMITTING);
		setPhase(TransactionPhase.TO_DOMAIN_COMMITTED);
		Transactions.get().onDomainTransactionCommited(this);
	}

	public void toDomainCommitting(Timestamp timestamp, DomainStore store,
			long sequenceId, long transformRequestId) {
		this.transformRequestId = transformRequestId;
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DOMAIN_PREPARING
						&& ThreadlocalTransformManager.get().getTransforms()
								.isEmpty());
		this.databaseCommitTimestamp = timestamp;
		storeTransactions.get(store).committingSequenceId = sequenceId;
		setPhase(TransactionPhase.TO_DOMAIN_COMMITTING);
	}

	public void toNoActiveTransaction() {
		if (this.phase == TransactionPhase.NO_ACTIVE_TRANSACTION) {
			return;
		}
		Preconditions.checkState((phase == TransactionPhase.TO_DB_PREPARING
				|| phase == TransactionPhase.TO_DOMAIN_PREPARING)
				&& TransformManager.get().getTransforms().isEmpty());
		this.phase = TransactionPhase.NO_ACTIVE_TRANSACTION;
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
			throw new UnsupportedOperationException(
					"Cannot cancel transaction in phase " + phase);
		}
	}

	private boolean hasHigherCommitIdThan(Transaction otherTransaction,
			DomainStore store) {
		long thisStoreTxId = storeTransactions.get(store).committingSequenceId;
		long otherStoreTxId = otherTransaction.storeTransactions
				.get(store).committingSequenceId;
		return thisStoreTxId > otherStoreTxId;
	}

	void endTransaction() {
		originatingThread = null;
		ended = true;
		switch (getPhase()) {
		case TO_DB_PERSISTED:
		case TO_DB_ABORTED:
		case TO_DOMAIN_COMMITTED:
		case TO_DOMAIN_ABORTED:
		case VACUUM_ENDED:
		case NO_ACTIVE_TRANSACTION:
		case TO_DB_PREPARING:
			break;
		default:
			// we used to throw to an exception if there were
			// uncommitted transforms but we can't allow dangling
			// transactions - we can end up here if an exception is thrown on
			// postProcess()
			if (AppPersistenceBase.isTestServer()) {
				throw new MvccException(Ax.format(
						"Ending on invalid phase: %s %s transforms", getPhase(),
						TransformManager.get().getTransforms().size()));
			} else {
				logger.warn("Ending transaction on invalid phase: {}",
						getPhase());
			}
		}
		if (TransformManager.get().getTransforms().size() == 0) {
		} else {
			// FIXME - mvcc.4 - mvcc exception
			logger.warn("Ending transaction with uncommitted transforms: {} {}",
					getPhase(), TransformManager.get().getTransforms().size());
		}
		// need to do this even if transforms == 0 - to clear listeners setup
		// during the transaction
		// the transaction
		ThreadlocalTransformManager.cast().resetTltm(null);
		if (ResourceUtilities.is("retainTransactionTraces")) {
			transactionEndTrace = SEUtilities.getCurrentThreadStacktraceSlice();
		}
		logger.debug("Ended tx: {}", this);
		Transactions.get().onTransactionEnded(this);
	}

	TransactionId getId() {
		return this.id;
	}

	TransactionPhase getPhase() {
		return this.phase;
	}

	long getTransformRequestId() {
		return this.transformRequestId;
	}

	boolean isReadonly() {
		return threadCount.get() != 1;
	}

	Transaction mostRecentPriorTransaction(Enumeration<Transaction> keys,
			DomainStore store) {
		Transaction result = null;
		while (keys.hasMoreElements()) {
			Transaction element = keys.nextElement();
			if (element.phase == TransactionPhase.TO_DOMAIN_COMMITTED) {
				if (committedTransactions.containsKey(element.id)
						|| element.isBaseTransaction()) {
					if (result == null
							|| element.hasHigherCommitIdThan(result, store)) {
						result = element;
					}
				}
			}
		}
		return result;
	}

	void setId(TransactionId id) {
		this.id = id;
	}

	void setPhase(TransactionPhase phase) {
		if (ResourceUtilities.is("debugSetPhase")) {
			logger.info("{}->{} ::\n{}", this.phase, phase,
					SEUtilities.getCurrentThreadStacktraceSlice());
		}
		this.phase = phase;
		logger.debug("Transition tx: {}", this);
	}

	void toVacuumEnded(List<Transaction> vacuumableTransactions) {
		Preconditions.checkState(getPhase() == TransactionPhase.VACUUM_BEGIN);
		Transactions.get().vacuumComplete(vacuumableTransactions);
		setPhase(TransactionPhase.VACUUM_ENDED);
	}

	/*
	 * Domain-committed transactions are 'before' non-committed - this ordering
	 * only makes sense for visible transactions
	 */
	static class TransactionComparator implements Comparator<Transaction> {
		@Override
		public int compare(Transaction o1, Transaction o2) {
			if (o1.phase.isDomain() && o2.phase.isDomain()) {
				return CommonUtils.compareLongs(
						o1.databaseCommitTimestamp.getTime(),
						o2.databaseCommitTimestamp.getTime());
			}
			if (o1.phase.isDomain()) {
				return -1;
			}
			if (o2.phase.isDomain()) {
				return 1;
			}
			return CommonUtils.compareLongs(o1.id.id, o2.id.id);
		}
	}
}

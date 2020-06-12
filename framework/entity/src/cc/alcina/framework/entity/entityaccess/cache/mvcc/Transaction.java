package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;

public class Transaction {
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
		if (transaction == null) {
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
		end();
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

	public static void join(Transaction transaction) {
		logger.debug("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.set(transaction);
	}

	// inverse of join
	public static void split() {
		logger.debug("Removing tx - {} {} {}", threadLocalInstance.get(),
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.remove();
	}

	static void begin(TransactionPhase initialPhase) {
		if (!Transactions.isInitialised()) {
			return;
		}
		if (threadLocalInstance.get() != null) {
			throw Ax.runtimeException("Begin without end: %s - %s",
					initialPhase, threadLocalInstance.get());
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
		threadLocalInstance.set(transaction);
		transaction.originatingThreadName = Thread.currentThread().getName();
		if (ResourceUtilities.is("retainTransactionStartTrace")) {
			transaction.transactionStartTrace = SEUtilities
					.getCurrentThreadStacktraceSlice();
		}
	}

	private long transformRequestId;

	private String transactionStartTrace;

	private String originatingThreadName;

	boolean ended;

	private Timestamp databaseCommitTimestamp;

	private boolean baseTransaction;

	Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

	private TransactionId id;

	private Map<DomainStore, StoreTransaction> storeTransactions = new LightMap();

	TransactionPhase phase;

	long startTime;

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

	public Map<TransactionId, Transaction> getCommittedTransactions() {
		return committedTransactions;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean isBaseTransaction() {
		return this.baseTransaction;
	}

	public boolean isPreCommit() {
		return phase == TransactionPhase.TO_DB_PREPARING;
	}

	public Transaction mostRecentPriorTransaction(Enumeration<Transaction> keys,
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

	public void setBaseTransaction(boolean baseTransaction, DomainStore store) {
		this.baseTransaction = baseTransaction;
		if (baseTransaction) {
			Transactions.registerBaseTransaction(store, this);
		}
	}

	public void setCommittedTransactions(
			Map<TransactionId, Transaction> committedTransactions) {
		this.committedTransactions = committedTransactions;
	}

	public void toDbAborted() {
		Preconditions.checkState(getPhase() == TransactionPhase.TO_DB_PERSISTING
				|| getPhase() == TransactionPhase.TO_DB_PREPARING);
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
		return transactionStartTrace == null ? detail
				: Ax.format("%s\n-------\n%s\n", detail, CommonUtils
						.hangingIndent(transactionStartTrace, false, 2));
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
		// FIXME - tx phases
		Preconditions.checkState((phase == TransactionPhase.TO_DB_PREPARING
				|| phase == TransactionPhase.TO_DOMAIN_PREPARING)
				&& TransformManager.get().getTransforms().isEmpty());
		this.phase = TransactionPhase.NO_ACTIVE_TRANSACTION;
	}

	@Override
	public String toString() {
		return Ax.format("%s::%s", id, phase);
	}

	private boolean hasHigherCommitIdThan(Transaction otherTransaction,
			DomainStore store) {
		long thisStoreTxId = storeTransactions.get(store).committingSequenceId;
		long otherStoreTxId = otherTransaction.storeTransactions
				.get(store).committingSequenceId;
		return thisStoreTxId > otherStoreTxId;
	}

	void endTransaction() {
		switch (getPhase()) {
		case TO_DB_PERSISTED:
		case TO_DB_ABORTED:
		case TO_DOMAIN_COMMITTED:
		case TO_DOMAIN_ABORTED:
		case VACUUM_ENDED:
		case NO_ACTIVE_TRANSACTION:
			break;
		case TO_DB_PREPARING:
			if (TransformManager.get().getTransforms().size() == 0) {
				break;
			} else {
				// we used to fallthrough to an exception if there were
				// uncommitted transforms but we can't allow dangling
				// transactions
				logger.warn(
						"Ending transaction with uncommitted transforms: {}",
						TransformManager.get().getTransforms().size());
				ThreadlocalTransformManager.cast().resetTltm(null);
				// fallthrough if test server
				if (!AppPersistenceBase.isTestServer()) {
					break;
				}
			}
		default:
			throw new MvccException(Ax.format(
					"Ending on invalid phase: %s %s transforms", getPhase(),
					TransformManager.get().getTransforms().size()));
		}
		ended = true;
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

	void setId(TransactionId id) {
		this.id = id;
	}

	void setPhase(TransactionPhase phase) {
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

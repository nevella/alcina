package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

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
		if (TransformManager.get().getTransforms().size() > 0) {
			throw new MvccException(
					"Ending transaction with uncommitted transforms");
		}
		threadLocalInstance.get().endTransaction();
		logger.trace("Removing tx - {} {} {}", threadLocalInstance.get(),
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
		logger.trace("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.set(transaction);
	}

	// inverse of join
	public static void separate() {
		logger.trace("Removing tx - {} {} {}", threadLocalInstance.get(),
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.remove();
	}

	static void begin(TransactionPhase initialPhase) {
		if (!Transactions.isInitialised()) {
			return;
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
		logger.trace("Joining tx - {} {} {}", transaction,
				Thread.currentThread().getName(),
				Thread.currentThread().getId());
		threadLocalInstance.set(transaction);
	}

	boolean ended;

	private Timestamp databaseCommitTimestamp;

	private boolean baseTransaction;

	Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

	private TransactionId id;

	private Map<DomainStore, StoreTransaction> storeTransactions = new LightMap();

	TransactionPhase phase;

	public Transaction(TransactionPhase initialPhase) {
		DomainStore.stores().stream().forEach(store -> storeTransactions
				.put(store, new StoreTransaction(store)));
		this.phase = initialPhase;
		Transactions.get().initialiseTransaction(this);
		logger.trace("Created tx: {}", this);
	}

	public <T extends Entity> T create(Class<T> clazz,
			DomainStore store) {
		StoreTransaction storeTransaction = storeTransactions.get(store);
		T t = storeTransaction.getMvcc().create(clazz);
		if (!isBaseTransaction()) {
			MvccObjectVersions<T> versions = new MvccObjectVersions<>(t, this,
					true);
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

	public void toDomainCommitting(Timestamp timestamp) {
		Preconditions
				.checkState(getPhase() == TransactionPhase.TO_DOMAIN_PREPARING
						&& ThreadlocalTransformManager.get().getTransforms()
								.isEmpty());
		this.databaseCommitTimestamp = timestamp;
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

	public void toVacuumEnded() {
		Preconditions.checkState(getPhase() == TransactionPhase.VACUUM_BEGIN);
		setPhase(TransactionPhase.VACUUM_ENDED);
	}

	private boolean hasHigherCommitIdThan(Transaction otherTransaction,
			DomainStore store) {
		long thisStoreTxId = storeTransactions
				.get(store).committedDbDomainTransformRequestId;
		long otherStoreTxId = otherTransaction.storeTransactions
				.get(store).committedDbDomainTransformRequestId;
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
			}
			// fallthrough
		default:
			throw new MvccException("Ending on invalid phase");
		}
		ended = true;
		logger.trace("Ended tx: {}", this);
		Transactions.get().onTransactionEnded(this);
	}

	TransactionId getId() {
		return this.id;
	}

	TransactionPhase getPhase() {
		return this.phase;
	}

	void setId(TransactionId id) {
		this.id = id;
	}

	void setPhase(TransactionPhase phase) {
		this.phase = phase;
		logger.trace("Transition tx: {}", this);
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

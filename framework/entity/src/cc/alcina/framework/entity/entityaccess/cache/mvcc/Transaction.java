package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class Transaction {
	private static ThreadLocal<Transaction> threadLocalInstance = new ThreadLocal() {
	};

	public static void begin() {
		threadLocalInstance.set(new Transaction());
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
		if (TransformManager.get().getTransforms().size() > 0) {
			throw new MvccException(
					"Ending transaction with uncommitted transforms");
		}
		threadLocalInstance.get().endTransaction();
		threadLocalInstance.remove();
	}

	public static void endAndBeginNew() {
		end();
		begin();
	}

	public static void ensureActive() {
		if (threadLocalInstance.get() == null) {
			begin();
		}
	}

	public static void ensureEnded() {
		if (threadLocalInstance.get() != null) {
			end();
		}
	}

	public static void join(Transaction transaction) {
		threadLocalInstance.set(transaction);
	}

	public static void separate() {
		threadLocalInstance.remove();
	}

	private Timestamp databaseCommitTimestamp;

	private boolean baseTransaction;

	Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

	private TransactionId id;

	private Map<DomainStore, StoreTransaction> storeTransactions = new LightMap();

	TransactionPhase phase;

	public Transaction() {
		DomainStore.stores().stream().forEach(store -> storeTransactions
				.put(store, new StoreTransaction(store)));
		Transactions.get().initialiseTransaction(this);
	}

	public <T extends HasIdAndLocalId> T create(Class<T> clazz,
			DomainStore store) {
		StoreTransaction storeTransaction = storeTransactions.get(store);
		T t = storeTransaction.getMvcc().create(clazz);
		if (!isBaseTransaction()) {
			MvccObjectVersions<T> versions = new MvccObjectVersions<>(t, this);
			((MvccObject<T>) t).__setMvccVersions__(versions);
		}
		return t;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Transaction) {
			Transaction other = (Transaction) obj;
			return Objects.equals(this.id, other.id)
					&& this.phase == other.phase;
		} else {
			return super.equals(obj);
		}
	}

	public Map<TransactionId, Transaction> getCommittedTransactions() {
		return committedTransactions;
	}

	@Override
	public int hashCode() {
		return id.hashCode() ^ phase.hashCode();
	}

	public boolean isBaseTransaction() {
		return this.baseTransaction;
	}

	public boolean isPreCommit() {
		return phase == TransactionPhase.PREPARING;
	}

	public Transaction mostRecentPriorTransaction(Enumeration<Transaction> keys,
			DomainStore store) {
		Transaction result = null;
		while (keys.hasMoreElements()) {
			Transaction element = keys.nextElement();
			if (element.phase == TransactionPhase.COMMITTED) {
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

	public void toAborted() {
		setPhase(TransactionPhase.ABORTED);
	}

	public void toCommitted(Timestamp timestamp) {
		this.databaseCommitTimestamp = timestamp;
		setPhase(TransactionPhase.COMMITTED);
		Transactions.get().onTransactionCommited(this);
	}

	public void toCommitting() {
		setPhase(TransactionPhase.COMMITING);
	}

	@Override
	public String toString() {
		return Ax.format("%s::%s", id, phase);
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
	}

	/*
	 * Committed transactions are 'before' non-committed
	 */
	static class TransactionComparator implements Comparator<Transaction> {
		@Override
		public int compare(Transaction o1, Transaction o2) {
			if (o1.phase == TransactionPhase.COMMITTED
					&& o2.phase == TransactionPhase.COMMITTED) {
				return o1.databaseCommitTimestamp
						.compareTo(o2.databaseCommitTimestamp);
			}
			if (o1.phase == TransactionPhase.COMMITTED) {
				return -1;
			}
			if (o2.phase == TransactionPhase.COMMITTED) {
				return 1;
			}
			return CommonUtils.compareLongs(o1.id.id, o2.id.id);
		}
	}
}

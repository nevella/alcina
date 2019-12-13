package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;

public class Transactions {
	private static Transactions instance;

	private static Map<DomainStore, Transaction> baseTransactions = new LightMap<>();

	public static <T extends HasIdAndLocalId> boolean checkResolved(T t) {
		return resolve(t, false) == t;
	}

	public static <T extends HasIdAndLocalId> T copyObject(T object) {
		// FIXME - write some byteassist classes to do direct copying
		return ResourceUtilities.fieldwiseClone(object, false, true);
	}

	public static synchronized void ensureInitialised() {
		if (instance == null) {
			instance = new Transactions();
		}
	}

	public static synchronized boolean isInitialised() {
		return instance != null;
	}

	public static <T extends HasIdAndLocalId> T resolve(T t, boolean write) {
		if (t instanceof MvccObject) {
			MvccObject mvccObject = (MvccObject) t;
			MvccObjectVersions<T> versions = mvccObject.__getMvccVersions__();
			if (versions == null && !write) {
				// no transactional versions, return base
				return t;
			} else {
				Transaction transaction = Transaction.current();
				if (transaction.isBaseTransaction()) {
					return t;
				} else {
					if (versions == null) {
						versions = MvccObjectVersions.ensure(t, transaction,
								false);
					}
					return versions.resolve(write);
				}
			}
		} else {
			return t;
		}
	}

	static Transaction baseTransaction(DomainStore store) {
		return baseTransactions.get(store);
	}

	static Transactions get() {
		return instance;
	}

	static synchronized void registerBaseTransaction(DomainStore store,
			Transaction transaction) {
		baseTransactions.put(store, transaction);
	}

	private Vacuum vacuum = new Vacuum();

	private AtomicLong transactionIdCounter = new AtomicLong();

	// these will be in commit order
	private Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

	private List<Transaction> completedNonDomainCommittedTransactions = new ArrayList<>();

	// these will be in start order
	private Map<TransactionId, Transaction> activeTransactions = new LinkedHashMap<>();

	private Object transactionMetadataLock = new Object();

	public List<Transaction>
			getCommittedTransactionsBeforeOrAt(TransactionId transactionId) {
		synchronized (transactionMetadataLock) {
			return committedTransactions.values().stream()
					.filter(t -> t.getId().id <= transactionId.id)
					.collect(Collectors.toList());
		}
	}

	public List<Transaction> getCompletedNonDomainTransactions() {
		synchronized (transactionMetadataLock) {
			List<Transaction> result = completedNonDomainCommittedTransactions;
			completedNonDomainCommittedTransactions = new ArrayList<>();
			return result;
		}
	}

	public void onDomainTransactionCommited(Transaction transaction) {
		synchronized (transactionMetadataLock) {
			committedTransactions.put(transaction.getId(), transaction);
		}
	}

	public void onTransactionEnded(Transaction transaction) {
		synchronized (transactionMetadataLock) {
			activeTransactions.remove(transaction.getId());
			switch (transaction.phase) {
			case TO_DOMAIN_COMMITTED:
			case VACUUM_ENDED:
				break;
			default:
				completedNonDomainCommittedTransactions.add(transaction);
				break;
			}
			if (transaction.phase != TransactionPhase.VACUUM_ENDED) {
				vacuum.enqueueVacuum();
			}
		}
	}

	/*
	 * To determine which transactions can be vacuumed - if all active
	 * transactions include a given transaction in their set of visible
	 * completed transactions, it can be compacted with the base layer
	 */
	Optional<TransactionId> getHighestCommonComittedTransactionId() {
		if (committedTransactions.isEmpty()) {
			return Optional.empty();
		}
		// FIXME - can probably optimise sync here (not sure if need to tho')
		TransactionId highest = CommonUtils
				.last(committedTransactions.keySet().iterator());
		synchronized (transactionMetadataLock) {
			for (Transaction t : activeTransactions.values()) {
				TransactionId transactionHighest = CommonUtils
						.last(committedTransactions.keySet().iterator());
				if (transactionHighest == null) {
					return Optional.empty();
				}
				if (transactionHighest.id < highest.id) {
					highest = transactionHighest;
				}
			}
		}
		return Optional.of(highest);
	}

	void initialiseTransaction(Transaction transaction) {
		synchronized (transactionMetadataLock) {
			TransactionId transactionId = new TransactionId(
					this.transactionIdCounter.getAndIncrement());
			transaction.setId(transactionId);
			transaction.setCommittedTransactions(
					new LinkedHashMap<>(committedTransactions));
			activeTransactions.put(transactionId, transaction);
		}
	}

	void onAddedVacuumable(Vacuumable vacuumable) {
		vacuum.addVacuumable(vacuumable);
	}
}

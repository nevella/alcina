package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;

public class Transactions {
	private static Transactions instance;

	private static Map<DomainStore, Transaction> baseTransactions = new LightMap<>();

	private static Multimap removedVersions = new Multimap();

	public static <T extends Entity> boolean checkResolved(T t) {
		return resolve(t, false, false) == t;
	}

	public static synchronized <T extends Entity> void
			debugRemoveVersion(T baseObject, ObjectVersion<T> version) {
		// removedVersions.add(baseObject, version);
	}

	public static synchronized void ensureInitialised() {
		if (instance == null) {
			instance = new Transactions();
		}
	}

	public static synchronized <T extends Entity> List<ObjectVersion<T>>
			getRemovedVersions(T baseObject) {
		List<ObjectVersion<T>> list = removedVersions.get(baseObject);
		if (list != null) {
			list.forEach(ObjectVersion::debugObjectHash);
		}
		return list;
	}

	public static synchronized boolean isInitialised() {
		return instance != null;
	}

	public static void pauseVacuum(boolean paused) {
		get().vacuum.paused = paused;
	}

	public static <T extends Entity> T resolve(T t, boolean write,
			boolean base) {
		if (t instanceof MvccObject) {
			MvccObject mvccObject = (MvccObject) t;
			MvccObjectVersions<T> versions = mvccObject.__getMvccVersions__();
			if (versions == null && !write) {
				// no transactional versions, return base
				return t;
			} else {
				Transaction transaction = Transaction.current();
				// TODO - possibly optimise (app level 'in warmup')
				// although - doesn't warmup write fields, not via setters? In
				// which case this isn't called in warmup?
				if (transaction.isBaseTransaction()) {
					return t;
				} else {
					//
					synchronized (t) {
						versions = mvccObject.__getMvccVersions__();
						if (versions == null) {
							versions = MvccObjectVersions.ensureEntity(t,
									transaction, false);
						}
						if (base) {
							return versions.getBaseObject();
						}
						return versions.resolve(write);
					}
				}
			}
		} else {
			return t;
		}
	}

	public static <E extends Entity> TransactionalSet<E>
			resolveTransactionalSet(TransactionalSet<E> t, boolean write) {
		MvccObject mvccObject = (MvccObject) t;
		MvccObjectVersions<TransactionalSet> versions = mvccObject
				.__getMvccVersions__();
		if (versions == null && !write) {
			// no transactional versions, return base
			return t;
		} else {
			Transaction transaction = Transaction.current();
			if (transaction.isBaseTransaction()) {
				return t;
			} else {
				//
				synchronized (t) {
					versions = mvccObject.__getMvccVersions__();
					if (versions == null) {
						versions = MvccObjectVersions.ensureTransactionalSet(t,
								transaction, false);
					}
					return versions.resolve(write);
				}
			}
		}
	}

	public static TransactionsStats stats() {
		return get().createStats();
	}

	// debug/testing only!
	public static void waitForAllToCompleteExSelf() {
		get().waitForAllToCompleteExSelf0();
	}

	static Transaction baseTransaction(DomainStore store) {
		return baseTransactions.get(store);
	}

	static <T extends MvccObject> T copyObject(T from) {
		// synchronized (debugMonitor) {
		T clone = null;
		try {
			if (from instanceof TransactionalSet) {
				clone = (T) new TransactionalSet();
			} else if (from instanceof TransactionalTrieEntry) {
				clone = (T) new TransactionalTrieEntry();
			} else {
				Constructor<T> constructor = (Constructor<T>) from.getClass()
						.getConstructor(new Class[0]);
				constructor.setAccessible(true);
				clone = constructor.newInstance();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		ResourceUtilities.fieldwiseCopy(from, clone, false, true);
		MvccObjectVersions __getMvccVersions__ = from.__getMvccVersions__();
		clone.__setMvccVersions__(__getMvccVersions__);
		return clone;
		// }
	}

	static <T> void copyObjectFields(T from, T to) {
		// synchronized (debugMonitor) {
		ResourceUtilities.fieldwiseCopy(from, to, false, true);
		// }
	}

	static Transactions get() {
		return instance;
	}

	static synchronized void registerBaseTransaction(DomainStore store,
			Transaction transaction) {
		baseTransactions.put(store, transaction);
	}

	static <K, V> TransactionalTrieEntry<K, V>
			resolveTrieEntry(TransactionalTrieEntry<K, V> t, boolean write) {
		MvccObject mvccObject = (MvccObject) t;
		MvccObjectVersions<TransactionalTrieEntry> versions = mvccObject
				.__getMvccVersions__();
		if (versions == null && !write) {
			// no transactional versions, return base
			return t;
		} else {
			Transaction transaction = Transaction.current();
			if (transaction.isBaseTransaction()) {
				return t;
			} else {
				//
				synchronized (t) {
					versions = mvccObject.__getMvccVersions__();
					if (versions == null) {
						versions = MvccObjectVersions.ensureTrieEntry(t,
								transaction, false);
					}
					return versions.resolve(write);
				}
			}
		}
	}

	private Vacuum vacuum = new Vacuum();

	private AtomicLong transactionIdCounter = new AtomicLong();

	// these will be in commit order
	private Map<TransactionId, Transaction> committedTransactions = new LinkedHashMap<>();

	private List<Transaction> completedNonDomainCommittedTransactions = new ArrayList<>();

	// these will be in start order
	private Map<TransactionId, Transaction> activeTransactions = new LinkedHashMap<>();

	private Object transactionMetadataLock = new Object();

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

	private TransactionsStats createStats() {
		return new TransactionsStats();
	}

	private void waitForAllToCompleteExSelf0() {
		while (true) {
			synchronized (transactionMetadataLock) {
				Transaction current = Transaction.current();
				if (activeTransactions.size() == 0) {
					return;
				}
				if (activeTransactions.containsKey(current.getId())
						&& activeTransactions.size() == 1) {
					return;
				}
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
	// static Object debugMonitor = new Object();

	/*
	 * Returns committed transactions which can be vacuumed, and prunes
	 * "committedTransactions" - if all active transactions include a given
	 * transaction in their set of visible completed transactions, it can be
	 * compacted with the base layer
	 * 
	 */
	List<Transaction> getVacuumableCommittedTransactions() {
		synchronized (transactionMetadataLock) {
			List<Transaction> result = new ArrayList<>();
			if (committedTransactions.isEmpty()) {
				return result;
			}
			// // FIXME - can probably optimise sync here (not sure if need to
			// // tho')
			TransactionId highestVacuumableId = CommonUtils
					.last(committedTransactions.keySet().iterator());
			for (Transaction activeTransaction : activeTransactions.values()) {
				// 'highest vacuumable' commit was visible to this transaction,
				// so ... good, continue
				if (activeTransaction.committedTransactions
						.containsKey(highestVacuumableId)) {
					continue;
				} else {
					// lower our sights
					highestVacuumableId = CommonUtils
							.last(activeTransaction.committedTransactions
									.keySet().iterator());
					if (highestVacuumableId == null) {
						return result;
					}
				}
			}
			Iterator<Entry<TransactionId, Transaction>> itr = committedTransactions
					.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<TransactionId, Transaction> next = itr.next();
				result.add(next.getValue());
				// itr.remove();
				// nope! remove after vacuum completes
				if (next.getKey().equals(highestVacuumableId)) {
					break;
				}
			}
			return result;
		}
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

	void vacuumComplete(List<Transaction> vacuumableTransactions) {
		synchronized (transactionMetadataLock) {
			vacuumableTransactions
					.forEach(tx -> committedTransactions.remove(tx.getId()));
		}
	}

	public class TransactionsStats {
		public String describeTransactions() {
			FormatBuilder fb = new FormatBuilder();
			synchronized (transactionMetadataLock) {
				fb.line("");
				fb.line("Active Transactions: (%s)", activeTransactions.size());
				fb.line("===========================");
				fb.indent(2);
				activeTransactions.values()
						.forEach(tx -> fb.line(tx.toDebugString()));
				fb.line("");
				fb.indent(0);
				fb.line("Committed Transactions: (%s)",
						committedTransactions.size());
				fb.line("===========================");
				fb.indent(2);
				committedTransactions.values()
						.forEach(tx -> fb.line(tx.toDebugString()));
				return fb.toString();
			}
		}

		public long getOldestTxStartTime() {
			synchronized (transactionMetadataLock) {
				return committedTransactions.isEmpty() ? 0L
						: committedTransactions.values().iterator()
								.next().startTime;
			}
		}

		public long getUncollectedTxCount() {
			synchronized (transactionMetadataLock) {
				return committedTransactions.size() + activeTransactions.size();
			}
		}

		public long getVacuumQueueLength() {
			return vacuum.vacuumables.size();
		}
	}
}

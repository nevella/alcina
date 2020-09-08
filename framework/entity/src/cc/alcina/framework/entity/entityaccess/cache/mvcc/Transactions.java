package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Vacuum.Vacuumable;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class Transactions {
	private static Transactions instance;

	public static <T extends Entity> boolean checkResolved(T t) {
		return resolve(t, false, false) == t;
	}

	public static synchronized void ensureInitialised() {
		if (instance == null) {
			instance = new Transactions();
		}
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
	private Object2ObjectLinkedOpenHashMap<TransactionId, Transaction> committedTransactions = new Object2ObjectLinkedOpenHashMap<>();

	private List<Transaction> completedNonDomainCommittedTransactions = new ArrayList<>();

	// these will be in start order
	private Map<TransactionId, Transaction> activeTransactions = new LinkedHashMap<>();

	private Object transactionMetadataLock = new Object();

	private List<EntityLocator> enqueuedLazyLoads = new ArrayList<>();

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

	void cancelTimedOutTransactions() {
		synchronized (transactionMetadataLock) {
			if (activeTransactions.size() > 0) {
				Transaction oldest = activeTransactions.values().iterator()
						.next();
				if ((System.currentTimeMillis()
						- oldest.startTime) > ResourceUtilities
								.getInteger(Transaction.class, "maxAgeSecs")
								* TimeConstants.ONE_SECOND_MS) {
					try {
						Transaction.logger.error(
								"Cancelling timed out transaction :: {}",
								oldest);
						oldest.toTimedOut();
						// only the tx thread should end the transaction (calls
						// to Transaction.current() will throw)
						// oldest.endTransaction();
					} catch (Exception e) {
						Transaction.logger.error("Cancel exception",
								new MvccException(e));
						// ignore phase checks
						onTransactionEnded(oldest);
					}
				}
			}
		}
	}

	/*
	 * Returns committed transactions which can be vacuumed, and prunes
	 * "committedTransactions" - if all active transactions include a given
	 * transaction in their set of visible completed transactions, it can be
	 * compacted with the base layer
	 * 
	 * This code relies on the commit ordering of
	 * Transactions.committedTransactions and Transaction.committedTransactions.
	 * 
	 */
	List<Transaction> getVacuumableCommittedTransactions() {
		synchronized (transactionMetadataLock) {
			List<Transaction> result = new ArrayList<>();
			if (committedTransactions.isEmpty()) {
				return result;
			}
			TransactionId highestVacuumableId = committedTransactions.lastKey();
			for (Transaction activeTransaction : activeTransactions.values()) {
				if (activeTransaction.committedTransactions
						.containsKey(highestVacuumableId)) {
					// 'highest vacuumable' commit was visible to this
					// transaction,
					// so ... good, continue
					continue;
				} else {
					// lower our sights
					highestVacuumableId = activeTransaction.committedTransactions
							.isEmpty() ? null
									: activeTransaction.committedTransactions
											.lastKey();
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
			transaction.committedTransactions = new Object2ObjectLinkedOpenHashMap<>(
					committedTransactions);
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
				fb.line("");
				fb.indent(0);
				fb.line("Vacuum queue transactions: (%s)",
						vacuum.vacuumables.size());
				fb.line("===========================");
				fb.indent(2);
				vacuum.vacuumables.keySet()
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

		public long getTimeInVacuum() {
			return vacuum.getVacuumStarted() == 0 ? 0
					: System.currentTimeMillis() - vacuum.getVacuumStarted();
		}

		public long getUncollectedTxCount() {
			synchronized (transactionMetadataLock) {
				return committedTransactions.size() + activeTransactions.size();
			}
		}

		public long getVacuumQueueLength() {
			return vacuum.vacuumables.size();
		}

		public Thread getVacuumThread() {
			return vacuum.getVacuumThread();
		}
	}

	public static void enqueueLazyLoad(EntityLocator locator) {
		synchronized (get().enqueuedLazyLoads) {
			get().enqueuedLazyLoads.add(locator);
		}
	}

	public static List<EntityLocator> getEnqueuedLazyLoads() {
		return get().getEnqueuedLazyLoads0();
	}

	private List<EntityLocator> getEnqueuedLazyLoads0() {
		synchronized (enqueuedLazyLoads) {
			List<EntityLocator> result = enqueuedLazyLoads.stream()
					.collect(Collectors.toList());
			enqueuedLazyLoads.clear();
			return result;
		}
	}
}

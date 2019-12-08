package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;

class Vacuum {
	ConcurrentHashMap<Transaction, ConcurrentHashMap<Vacuumable, Vacuumable>> vacuumables = new ConcurrentHashMap<>();

	Object queueCreationMonitor = new Object();

	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(1,
					new NamedThreadFactory("domainstore-mvcc-vacuum"));

	Logger logger = LoggerFactory.getLogger(getClass());

	public void addVacuumable(Vacuumable vacuumable) {
		Transaction transaction = Transaction.current();
		if (!vacuumables.containsKey(transaction)) {
			synchronized (queueCreationMonitor) {
				if (!vacuumables.containsKey(transaction)) {
					vacuumables.put(transaction, new ConcurrentHashMap<>());
				}
			}
		}
		vacuumables.get(transaction).put(vacuumable, vacuumable);
	}

	public void enqueueVacuum() {
		if (executor.getQueue().size() > 10) {
			return;
		}
		executor.execute(() -> vacuum());
	}

	/*
	 * synchronized semantic - only called from single-thread executor
	 */
	private synchronized void vacuum() {
		Transaction.begin(TransactionPhase.VACUUM_BEGIN);
		logger.warn("transactions with vacuumables: {}", vacuumables.size());
		Optional<TransactionId> vacuumableTransactionId = Transactions.get()
				.getHighestCommonComittedTransactionId();
		if (!vacuumableTransactionId.isPresent()) {
			return;
		}
		List<Transaction> vacuumableTransactions = Transactions.get()
				.getCommittedTransactionsBeforeOrAt(
						vacuumableTransactionId.get());
		for (Transaction transaction : vacuumableTransactions) {
			if (vacuumables.containsKey(transaction)) {
				vacuumables.get(transaction).keySet()
						.forEach(v -> this.vacuum(v, transaction));
			}
		}
		Transaction.current().toVacuumEnded();
		Transaction.end();
	}

	private void vacuum(Vacuumable vacuumable, Transaction transaction) {
		logger.warn("would vacuum: {}", vacuumable);
		vacuumable.vacuum(transaction);
	}

	interface Vacuumable {
		void vacuum(Transaction transaction);
	}
}

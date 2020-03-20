package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;

class Vacuum {
	ConcurrentHashMap<Transaction, ConcurrentHashMap<Vacuumable, Vacuumable>> vacuumables = new ConcurrentHashMap<>();

	Object queueCreationMonitor = new Object();

	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(1,
					new NamedThreadFactory("domainstore-mvcc-vacuum"));

	Logger logger = LoggerFactory.getLogger(getClass());

	public void addVacuumable(Vacuumable vacuumable) {
		if (vacuumable instanceof Entity) {
			if (((Entity) vacuumable).getId() <= 0) {
				int debug = 3;
			}
		}
		Transaction transaction = Transaction.current();
		if (!vacuumables.containsKey(transaction)) {
			synchronized (queueCreationMonitor) {
				if (!vacuumables.containsKey(transaction)) {
					logger.warn("added vacuumable transaction: {}",
							transaction);
					vacuumables.put(transaction, new ConcurrentHashMap<>());
				}
			}
		}
		if (vacuumables.get(transaction).put(vacuumable, vacuumable) == null) {
			logger.trace("added vacuumable object: {}=>{}:{}", transaction,
					vacuumable.getClass().getSimpleName(), vacuumable);
		}
	}

	public void enqueueVacuum() {
		/*
		 * really, there will only ever be one truly 'active' (because of the
		 * synchronized block), but this lets us keep calling
		 */
		if (executor.getActiveCount() > 2) {
			return;
		}
		executor.execute(() -> vacuum());
	}

	/*
	 * synchronized semantic - only called from single-thread executor
	 */
	private synchronized void vacuum() {
		Transaction.begin(TransactionPhase.VACUUM_BEGIN);
		logger.debug("transactions with vacuumables: {} : {}",
				vacuumables.size(), vacuumables.keySet());
		Optional<TransactionId> vacuumableTransactionId = Transactions.get()
				.getHighestCommonComittedTransactionId();
		if (!vacuumableTransactionId.isPresent()) {
			return;
		}
		List<Transaction> vacuumableTransactions = Transactions.get()
				.getCommittedTransactionsBeforeOrAt(
						vacuumableTransactionId.get());
		vacuumableTransactions
				.addAll(Transactions.get().getCompletedNonDomainTransactions());
		for (Transaction transaction : vacuumableTransactions) {
			if (vacuumables.containsKey(transaction)) {
				logger.debug("vaccuming transaction: {}", transaction);
				vacuumables.get(transaction).keySet()
						.forEach(v -> this.vacuum(v, transaction));
				vacuumables.remove(transaction);
				logger.debug("removed vacuumable transaction: {}", transaction);
			}
		}
		Transaction.current().toVacuumEnded();
		Transaction.end();
	}

	private void vacuum(Vacuumable vacuumable, Transaction transaction) {
		logger.trace("would vacuum: {}", vacuumable);
		vacuumable.vacuum(transaction);
	}

	interface Vacuumable {
		void vacuum(Transaction transaction);
	}
}

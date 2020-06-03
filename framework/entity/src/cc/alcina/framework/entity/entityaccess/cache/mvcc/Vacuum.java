package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.List;
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

	boolean paused;

	public void addVacuumable(Vacuumable vacuumable) {
		Transaction transaction = Transaction.current();
		if (!vacuumables.containsKey(transaction)) {
			synchronized (queueCreationMonitor) {
				if (!vacuumables.containsKey(transaction)) {
					logger.debug("added vacuumable transaction: {}",
							transaction);
					vacuumables.put(transaction, new ConcurrentHashMap<>());
				}
			}
		}
		if (vacuumables.get(transaction).put(vacuumable, vacuumable) == null) {
			// logger.trace("added vacuumable object: {}=>{}:{}", transaction,
			// vacuumable.getClass().getSimpleName(), vacuumable);
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
		executor.execute(() -> {
			vacuum();
		});
	}

	/*
	 * synchronized semantic - only called from single-thread executor
	 */
	private synchronized void vacuum() {
		while (paused) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Transaction.begin(TransactionPhase.VACUUM_BEGIN);
		logger.debug("vacuum: transactions with vacuumables: {} : {}",
				vacuumables.size(), vacuumables.keySet());
		List<Transaction> vacuumableTransactions = Transactions.get()
				.getVacuumableCommittedTransactions();
		vacuumableTransactions
				.addAll(Transactions.get().getCompletedNonDomainTransactions());
		for (Transaction transaction : vacuumableTransactions) {
			if (vacuumables.containsKey(transaction)) {
				logger.debug("vacuuming transaction: {}", transaction);
				vacuumables.get(transaction).keySet()
						.forEach(v -> this.vacuum(v, transaction));
				vacuumables.remove(transaction);
				logger.debug("removed vacuumable transaction: {}", transaction);
			}
		}
		Transaction.current().toVacuumEnded(vacuumableTransactions);
		logger.debug("vacuum: end");
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

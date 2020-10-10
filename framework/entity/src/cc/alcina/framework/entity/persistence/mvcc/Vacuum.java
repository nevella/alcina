package cc.alcina.framework.entity.persistence.mvcc;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;

class Vacuum {
	ConcurrentHashMap<Transaction, ConcurrentHashMap<Vacuumable, Vacuumable>> vacuumables = new ConcurrentHashMap<>();

	Object queueCreationMonitor = new Object();

	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(1,
					new NamedThreadFactory("domainstore-mvcc-vacuum"));

	Logger logger = LoggerFactory.getLogger(getClass());

	boolean paused;

	private long vacuumStarted = 0;

	private Thread vacuumThread = null;

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
			try {
				vacuum();
			} catch (Throwable t) {
				EntityLayerLogging.log(LogMessageType.WORKER_THREAD_EXCEPTION,
						"Vacuum exception", t);
			}
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
		try {
			Transaction.begin(TransactionPhase.VACUUM_BEGIN);
			vacuumThread = Thread.currentThread();
			vacuumStarted = System.currentTimeMillis();
			boolean debugLevelLogging = vacuumables.size() > 0;
			if (debugLevelLogging) {
				logger.debug("vacuum: transactions with vacuumables: {} : {}",
						vacuumables.size(), vacuumables.keySet());
			} else {
				logger.trace("vacuum: removing txs without vacuumables");
			}
			Transactions.get().cancelTimedOutTransactions();
			List<Transaction> vacuumableTransactions = Transactions.get()
					.getVacuumableCommittedTransactions();
			vacuumableTransactions.addAll(
					Transactions.get().getCompletedNonDomainTransactions());
			for (Transaction transaction : vacuumableTransactions) {
				if (vacuumables.containsKey(transaction)) {
					String dtrIdClause = transaction
							.getTransformRequestId() == 0 ? ""
									: Ax.format("- %s ", transaction
											.getTransformRequestId());
					logger.debug("vacuuming transaction: {} {}- {} vacuumables",
							transaction, dtrIdClause,
							vacuumables.get(transaction).size());
					vacuumables.get(transaction).keySet()
							.forEach(v -> this.vacuum(v, transaction));
					vacuumables.remove(transaction);
					logger.debug("removed vacuumable transaction: {}",
							transaction);
				}
			}
			Transaction.current().toVacuumEnded(vacuumableTransactions);
			vacuumStarted = 0;
			vacuumThread = null;
			if (debugLevelLogging) {
				logger.debug("vacuum: end");
			}
		} catch (Exception e) {
			logger.warn("Vacuum exception", new MvccException(e));
		} finally {
			Transaction.end();
		}
	}

	private void vacuum(Vacuumable vacuumable, Transaction transaction) {
		logger.trace("would vacuum: {}", vacuumable);
		vacuumable.vacuum(transaction);
	}

	long getVacuumStarted() {
		return this.vacuumStarted;
	}

	Thread getVacuumThread() {
		return this.vacuumThread;
	}

	interface Vacuumable {
		void vacuum(Transaction transaction);
	}
}

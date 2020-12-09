package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

class Vacuum {
	/*
	 * The per-transaction vacuumables will only be accessed during write by the
	 * transaction thread
	 */
	ConcurrentHashMap<Transaction, ReferenceOpenHashSet<Vacuumable>> vacuumables = new ConcurrentHashMap<>();

	Object queueCreationMonitor = new Object();

	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(1,
					new NamedThreadFactory("domainstore-mvcc-vacuum"));

	Logger logger = LoggerFactory.getLogger(getClass());

	BlockingQueue<Transaction> events = new LinkedBlockingQueue<>();

	boolean paused;

	private long vacuumStarted = 0;

	private Thread vacuumThread = null;

	volatile boolean finished = false;

	public Vacuum() {
		vacuumThread = new Thread(new EventHandler(),
				"domainstore-mvcc-vacuum");
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
			List<Transaction> vacuumableTransactionList = Transactions.get()
					.getVacuumableCommittedTransactions();
			vacuumableTransactionList.addAll(
					Transactions.get().getCompletedNonDomainTransactions());
			vacuumableTransactionList.retainAll(vacuumables.keySet());
			for (Transaction transaction : vacuumableTransactionList) {
				String dtrIdClause = transaction.getTransformRequestId() == 0
						? ""
						: Ax.format("- %s ",
								transaction.getTransformRequestId());
				logger.debug("vacuuming transaction: {} {}- {} vacuumables",
						transaction, dtrIdClause,
						vacuumables.get(transaction).size());
			}
			ReferenceOpenHashSet<Vacuumable> toVacuum = new ReferenceOpenHashSet<>();
			vacuumableTransactionList.stream().map(vacuumables::get)
					.flatMap(Collection::stream).forEach(toVacuum::add);
			VacuumableTransactions vacuumableTransactions = new VacuumableTransactions(
					vacuumableTransactionList);
			toVacuum.forEach(v -> this.vacuum(v, vacuumableTransactions));
			vacuumableTransactionList.forEach(vacuumables::remove);
			Transaction.current().toVacuumEnded(vacuumableTransactionList);
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

	private void vacuum(Vacuumable vacuumable,
			VacuumableTransactions vacuumableTransactions) {
		logger.trace("would vacuum: {}", vacuumable);
		vacuumable.vacuum(vacuumableTransactions);
	}

	void addVacuumable(Transaction transaction, Vacuumable vacuumable) {
		vacuumables.computeIfAbsent(transaction,
				tx -> new ReferenceOpenHashSet<>()).add(vacuumable);
	}

	long getVacuumStarted() {
		return this.vacuumStarted;
	}

	Thread getVacuumThread() {
		return this.vacuumThread;
	}

	void shutdown() {
		finished = true;
		Transaction.ensureBegun();
		// will trigger an event, which will terminate the thread
		Transaction.end();
	}

	class EventHandler implements Runnable {
		@Override
		public void run() {
			while (!finished) {
				try {
					Transaction tx = events.poll(2, TimeUnit.SECONDS);
					if (tx != null) {
						vacuum();
					}
				} catch (Exception e) {
					logger.warn("DEVEX::1", e);
				}
			}
		}
	}

	interface Vacuumable {
		void vacuum(VacuumableTransactions vacuumableTransactions);
	}

	static class VacuumableTransactions {
		Set<Transaction> completedNonDomainTransactions = new ReferenceOpenHashSet<>();

		ObjectAVLTreeSet<Transaction> completedDomainTransactions = new ObjectAVLTreeSet<>(
				Collections.reverseOrder());

		public VacuumableTransactions(
				List<Transaction> vacuumableTransactionList) {
			vacuumableTransactionList.forEach(t -> {
				if (t.phase == TransactionPhase.TO_DOMAIN_COMMITTED) {
					completedDomainTransactions.add(t);
				} else {
					completedNonDomainTransactions.add(t);
				}
			});
		}

		/*
		 * reliant on both completedDomainTransactions and mostRecentOrderedSet
		 * are mostRecent===first
		 */
		public Optional<Transaction> mostRecentCommonDomainTransaction(
				SortedSet<Transaction> mostRecentOrderedSet) {
			Transaction tx = null;
			if (mostRecentOrderedSet.size() == 1) {
				Transaction next = mostRecentOrderedSet.iterator().next();
				tx = completedDomainTransactions.contains(next) ? next : null;
			}
			Set<Transaction> larger = null;
			Set<Transaction> smaller = null;
			if (completedDomainTransactions.size() <= mostRecentOrderedSet
					.size()) {
				larger = mostRecentOrderedSet;
				smaller = completedDomainTransactions;
			} else {
				larger = completedDomainTransactions;
				smaller = mostRecentOrderedSet;
			}
			Iterator<Transaction> itr = smaller.iterator();
			while (itr.hasNext()) {
				Transaction test = itr.next();
				if (larger.contains(test)) {
					tx = test;
					break;
				}
			}
			return Optional.ofNullable(tx);
		}
	}
}

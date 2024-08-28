package cc.alcina.framework.entity.persistence.mvcc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

class Vacuum {
	public static final int MAX_DEBUG_EVENTS = 500000;

	/*
	 * The per-transaction vacuumables will only be accessed during write by the
	 * transaction thread
	 */
	ConcurrentHashMap<Transaction, List<Vacuumable>> vacuumables = new ConcurrentHashMap<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	BlockingQueue<Transaction> events = new LinkedBlockingQueue<>();

	boolean paused = false;

	private volatile long vacuumStarted = 0;

	private Thread vacuumThread = null;

	private Thread activeThread = null;

	volatile boolean finished = false;

	public Vacuum() {
		vacuumThread = new Thread(new EventHandler(),
				"domainstore-mvcc-vacuum");
	}

	void addVacuumable(Transaction transaction, Vacuumable vacuumable) {
		vacuumables.computeIfAbsent(transaction, tx -> new ArrayList<>())
				.add(vacuumable);
	}

	private void emitDebugEvent(String message) {
		// noop - but there's support in the file history
	}

	public void enqueueVacuum(Transaction transaction) {
		events.add(transaction);
	}

	public Thread getActiveThread() {
		return this.activeThread;
	}

	long getVacuumStarted() {
		return this.vacuumStarted;
	}

	void setActiveThread(Thread thread) {
		activeThread = thread;
		vacuumStarted = thread == null ? 0 : System.currentTimeMillis();
		emitDebugEvent("active thread changed");
	}

	void shutdown() {
		finished = true;
		Transaction.ensureBegun();
		// will trigger an event, which will terminate the thread
		Transaction.end();
	}

	void start() {
		vacuumThread.start();
	}

	/*
	 * synchronized is purely semantic - since this is only called from
	 * single-thread executor anyway
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
			Transaction.reapUnreferencedTransactions();
			setActiveThread(Thread.currentThread());
			boolean hasVacuumables = vacuumables.size() > 0;
			if (hasVacuumables) {
				String message = Ax.format(
						"vacuum: transactions with vacuumables: %s",
						vacuumables.size());
				emitDebugEvent(message);
			} else {
				logger.trace("vacuum: removing txs without vacuumables");
			}
			Transactions.get().cancelTimedOutTransactions();
			List<Transaction> vacuumableTransactionList = Transactions.get()
					.getVacuumableCommittedTransactions();
			vacuumableTransactionList.addAll(
					Transactions.get().getCompletedNonDomainTransactions());
			List<Transaction> withVacuumableObjectsList = new ArrayList<>(
					vacuumableTransactionList);
			withVacuumableObjectsList.retainAll(vacuumables.keySet());
			int vacuumableObjectCount = 0;
			for (Transaction transaction : withVacuumableObjectsList) {
				String dtrIdClause = transaction.getTransformRequestId() == 0
						? ""
						: Ax.format("- %s ",
								transaction.getTransformRequestId());
				logger.debug("vacuuming transaction: {} {}- {} vacuumables",
						transaction, dtrIdClause,
						vacuumables.get(transaction).size());
			}
			ReferenceOpenHashSet<Vacuumable> toVacuum = new ReferenceOpenHashSet<>();
			withVacuumableObjectsList.stream().map(vacuumables::get)
					.flatMap(Collection::stream).forEach(toVacuum::add);
			VacuumableTransactions vacuumableTransactions = new VacuumableTransactions(
					vacuumableTransactionList);
			toVacuum.forEach(v -> this.vacuum(v, vacuumableTransactions));
			vacuumableTransactionList.forEach(vacuumables::remove);
			Transaction.current().toVacuumEnded(vacuumableTransactionList);
			if (System.currentTimeMillis() - vacuumStarted > 500) {
				String message = Ax.format(
						"Long-running vacuum - %s transactions; %s objects; thread %s",
						vacuumableTransactionList.size(), toVacuum.size(),
						activeThread);
				emitDebugEvent(message);
				logger.warn(message);
			}
			setActiveThread(null);
		} catch (Throwable e) {
			emitDebugEvent(SEUtilities.getFullExceptionMessage(e));
			e.printStackTrace();
			logger.warn("DEVEX-1 - Vacuum exception", new MvccException(e));
		} finally {
			Transaction.end();
		}
	}

	private void vacuum(Vacuumable vacuumable,
			VacuumableTransactions vacuumableTransactions) {
		logger.trace("would vacuum: {}", vacuumable);
		vacuumable.vacuum(vacuumableTransactions);
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
				} catch (Throwable e) {
					logger.warn("DEVEX::0 - vacuum issue");
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * Try to override hashcode - it seems that causes a lot of the performance
	 * cost
	 */
	interface Vacuumable {
		default void onAddToVacuumQueue() {
		}

		void vacuum(VacuumableTransactions vacuumableTransactions);
	}

	static class VacuumableTransactions {
		/*
		 * Not a ReferenceOpenHashSet due to System.identityHashCode performance
		 * issues
		 */
		ObjectOpenHashSet<Transaction> completedNonDomainTransactions = new ObjectOpenHashSet<>();

		/*
		 * Newest-id tx is first (and to-domain is sequential, so this will be
		 * chronologically the newest too)
		 */
		ObjectAVLTreeSet<Transaction> completedDomainTransactions = new ObjectAVLTreeSet<>(
				Collections.reverseOrder());

		Transaction oldestVacuumableDomainTransaction;

		public VacuumableTransactions(
				List<Transaction> vacuumableTransactionList) {
			vacuumableTransactionList.forEach(t -> {
				if (t.phase == TransactionPhase.TO_DOMAIN_COMMITTED) {
					completedDomainTransactions.add(t);
				} else {
					completedNonDomainTransactions.add(t);
				}
			});
			/*
			 * completedDomainTransactions ordered by most-recent to oldest
			 */
			oldestVacuumableDomainTransaction = completedDomainTransactions
					.iterator().hasNext() ? completedDomainTransactions.last()
							: null;
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

	List<Vacuumable> getVacuumables(Transaction transaction) {
		return vacuumables.getOrDefault(transaction, List.of());
	}
}

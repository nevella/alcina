package cc.alcina.framework.entity.persistence.mvcc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.Vacuumable;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class Transactions {
	private static Transactions instance;

	private static ConcurrentHashMap<Class, Constructor> copyConstructors = new ConcurrentHashMap<>();

	public static <T> int callWithCommits(long size, String streamName,
			Stream<T> stream, Consumer<T> consumer,
			int commitEveryNTransforms) {
		AtomicInteger counter = new AtomicInteger();
		SystemoutCounter ticks = SystemoutCounter.standardJobCounter((int) size,
				streamName);
		stream.forEach(t -> {
			consumer.accept(t);
			int delta = Transaction
					.commitIfTransformCount(commitEveryNTransforms);
			counter.addAndGet(delta);
			ticks.tick();
		});
		int delta = Transaction.commit();
		counter.addAndGet(delta);
		return counter.get();
	}

	public static void copyIdFieldsToCurrentVersion(Entity entity) {
		MvccObjectVersions versions = ((MvccObject) entity)
				.__getMvccVersions__();
		((MvccObjectVersionsEntity) versions).copyIdFieldsToCurrentVersion();
	}

	public static void enqueueLazyLoad(EntityLocator locator) {
		synchronized (get().enqueuedLazyLoads) {
			get().enqueuedLazyLoads.add(locator);
		}
	}

	public static synchronized void ensureInitialised() {
		if (instance == null) {
			instance = new Transactions();
			instance.vacuum.start();
		}
	}

	public static List<EntityLocator> getEnqueuedLazyLoads() {
		return get().getEnqueuedLazyLoads0();
	}

	public static boolean isCommitted(TransactionId committingTxId) {
		return get().isCommitted0(committingTxId);
	}

	public static synchronized boolean isInitialised() {
		return instance != null;
	}

	public static void pauseVacuum(boolean paused) {
		get().vacuum.paused = paused;
	}

	/*
	 * The 'domainIdentity' parameter is used by ClassTransformer rewritten
	 * classes (to obtain the domainIdentity version of the object)
	 */
	public static <T extends Entity> T resolve(T t, ResolvedVersionState state,
			boolean domainIdentity) {
		if (t instanceof MvccObject) {
			MvccObject mvccObject = (MvccObject) t;
			MvccObjectVersions<T> versions = mvccObject.__getMvccVersions__();
			if (versions == null && state == ResolvedVersionState.READ) {
				// no transactional versions, return base
				return t;
			} else {
				// if returning 'domainIdentity', no need to synchronize (it's
				// being
				// returned as domainIdentity(), not for fields - and the
				// identity itself is immutable)
				if (domainIdentity) {
					return versions.domainIdentity;
				}
				Transaction transaction = Transaction.current();
				// TODO - possibly optimise (app level 'in warmup')
				// although - doesn't warmup write fields, not via setters? In
				// which case this isn't called in warmup?
				// FIXME - mvcc.5 - yep, setters (so resolve) shouldn't be
				// called _at_all during warmup. Precondition me
				if (transaction.isBaseTransaction() || (versions != null
						&& transaction == versions.initialWriteableTransaction)) {
					return t;
				} else {
					// FIXME - mvcc.5 - this synchronization means that (a) a
					// bunch of entities have issues with
					// System.identityHashCode and that all the concurrency in
					// MvccObjectVersions is unneccesary. But it's safe, at
					// least...
					synchronized (t) {
						versions = mvccObject.__getMvccVersions__();
						if (versions == null) {
							versions = MvccObjectVersions.ensureEntity(t,
									transaction, false);
						}
						boolean writeableVersion = state == ResolvedVersionState.WRITE;
						/*
						 * see docs for READ_INVALID
						 */
						if (state == ResolvedVersionState.READ_INVALID) {
							versions.verifyWritable(transaction);
						}
						return versions.resolve(writeableVersion);
					}
				}
			}
		} else {
			return t;
		}
	}

	public static void revertToDefaultFieldValues(Entity entity) {
		Entity defaults = (Entity) Reflections
				.newInstance(entity.entityClass());
		// because copying fields without resolution, entity will be the
		// domainVersion
		copyObjectFields(defaults, entity);
	}

	public static void shutdown() {
		Transactions transactions = get();
		if (transactions != null) {
			transactions.vacuum.shutdown();
		}
	}

	public static TransactionsStats stats() {
		return get().createStats();
	}

	// debug/testing only!
	public static void waitForAllToCompleteExSelf() {
		get().waitForAllToCompleteExSelf0();
	}

	static <T extends MvccObject> T copyObject(T from,
			boolean withFieldValues) {
		T clone = null;
		try {
			if (from instanceof TransactionalSet) {
				clone = (T) new TransactionalSet();
			} else if (from instanceof TransactionalTrieEntry) {
				clone = (T) new TransactionalTrieEntry();
			} else {
				Constructor<T> constructor = copyConstructors
						.computeIfAbsent(from.getClass(), clazz -> {
							try {
								Constructor<T> constructor0 = (Constructor<T>) from
										.getClass()
										.getConstructor(new Class[0]);
								constructor0.setAccessible(true);
								return constructor0;
							} catch (Exception e) {
								throw new WrappedRuntimeException(e);
							}
						});
				clone = constructor.newInstance();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		if (withFieldValues) {
			ResourceUtilities.fieldwiseCopy(from, clone, false, true);
		}
		MvccObjectVersions __getMvccVersions__ = from.__getMvccVersions__();
		clone.__setMvccVersions__(__getMvccVersions__);
		return clone;
	}

	static <T> void copyObjectFields(T from, T to) {
		ResourceUtilities.fieldwiseCopy(from, to, false, true);
	}

	static Transactions get() {
		return instance;
	}

	// FIXME - mvcc.5 - only implement if performance warrants (as opposed to
	// current 'synchronize on the object' logic)
	static Object identityMutex(Object o) {
		// to avoid synchronizing on o - slower, but means that object can
		// maintain an identity hashcode
		throw new UnsupportedOperationException();
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

	// these will be in reverse commit order
	private ConcurrentSkipListSet<Transaction> committedTransactions = new ConcurrentSkipListSet<>(
			Collections.reverseOrder());

	private List<Transaction> completedNonDomainCommittedTransactions = new ArrayList<>();

	// these will be in start order
	private Map<TransactionId, Transaction> activeTransactions = new LinkedHashMap<>();

	private Object transactionMetadataLock = new Object();

	private List<EntityLocator> enqueuedLazyLoads = new ArrayList<>();

	private TransactionId highestVisibleCommittedTransactionId;

	private Set<TransactionId> committedTransactionIds = new ObjectOpenHashSet<>();

	public List<Transaction> getCompletedNonDomainTransactions() {
		synchronized (transactionMetadataLock) {
			List<Transaction> result = completedNonDomainCommittedTransactions;
			completedNonDomainCommittedTransactions = new ArrayList<>();
			return result;
		}
	}

	public void onDomainTransactionCommited(Transaction transaction) {
		synchronized (transactionMetadataLock) {
			committedTransactions.add(transaction);
			/*
			 * these occur sequentially, so transaction will always have the
			 * highest visible id (for a tx of this type)
			 */
			highestVisibleCommittedTransactionId = transaction.getId();
			committedTransactionIds.add(transaction.getId());
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
				vacuum.enqueueVacuum(transaction);
			}
		}
	}

	private TransactionsStats createStats() {
		return new TransactionsStats();
	}

	private List<EntityLocator> getEnqueuedLazyLoads0() {
		synchronized (enqueuedLazyLoads) {
			List<EntityLocator> result = enqueuedLazyLoads.stream()
					.collect(Collectors.toList());
			enqueuedLazyLoads.clear();
			return result;
		}
	}

	private boolean isCommitted0(TransactionId committingTxId) {
		synchronized (transactionMetadataLock) {
			return committedTransactionIds.contains(committingTxId);
		}
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

	void cancelTimedOutTransactions() {
		if (!ResourceUtilities.is(Transactions.class,
				"cancelTimedoutTransactions")) {
			return;
		}
		synchronized (transactionMetadataLock) {
			if (activeTransactions.size() > 0) {
				Iterator<Transaction> iterator = activeTransactions.values()
						.iterator();
				boolean seenStandardTransactionTimeout = false;
				while (!seenStandardTransactionTimeout && iterator.hasNext()) {
					Transaction transaction = iterator.next();
					long age = System.currentTimeMillis()
							- transaction.startTime;
					if (!transaction.publishedLongRunningTxWarning
							&& age > ResourceUtilities.getInteger(
									Transaction.class, "warnAgeSecs")
									* TimeConstants.ONE_SECOND_MS) {
						transaction.publishedLongRunningTxWarning = true;
						Transaction.logger.warn(
								"Long running mvcc transaction :: {}",
								transaction);
						if (transaction.originatingThread != null) {
							try {
								Transaction.logger
										.info(SEUtilities.getStacktraceSlice(
												transaction.originatingThread));
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							Transaction.logger.warn(
									"No originating thread :: {}", transaction);
						}
					}
					long timeout = ResourceUtilities
							.getInteger(Transaction.class, "maxAgeSecs")
							* TimeConstants.ONE_SECOND_MS;
					if (transaction.getTimeout() == 0) {
						seenStandardTransactionTimeout = true;
					} else {
						timeout = transaction.getTimeout();
					}
					if (age > timeout) {
						try {
							Transaction.logger.error(
									"Cancelling timed out transaction :: {} :: timeout {}",
									transaction, timeout);
							transaction.toTimedOut();
							// only the tx thread should end the transaction
							// (otherwise calls
							// to Transaction.current() will throw)
							// so - we let the Tx stay in the threadlocalmap,
							// but remove from the app lookups in the finally
							// clause
							// oldest.endTransaction();
						} catch (Exception e) {
							Transaction.logger.error("Cancel exception",
									new MvccException(e));
						} finally {
							onTransactionEnded(transaction);
						}
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
	 * TODO - maybe 'reference' counting would be more optimal?
	 * 
	 * also - just remove txid (since txs are sorted)(albeit in reverse)
	 * 
	 */
	List<Transaction> getVacuumableCommittedTransactions() {
		synchronized (transactionMetadataLock) {
			List<Transaction> result = new ArrayList<>();
			if (committedTransactions.isEmpty()) {
				return result;
			}
			Transaction highestVacuumable = committedTransactions.first();
			for (Transaction activeTransaction : activeTransactions.values()) {
				if (activeTransaction.committedTransactions
						.contains(highestVacuumable)) {
					// 'highest vacuumable' commit was visible to this
					// transaction,
					// so ... good, continue
					continue;
				} else {
					// lower our sights
					highestVacuumable = activeTransaction.committedTransactions
							.isEmpty() ? null
									: activeTransaction.committedTransactions
											.first();
					if (highestVacuumable == null) {
						return result;
					}
				}
			}
			committedTransactions.tailSet(highestVacuumable)
					.forEach(result::add);
			return result;
		}
	}

	void initialiseTransaction(Transaction transaction) {
		synchronized (transactionMetadataLock) {
			TransactionId transactionId = new TransactionId(
					this.transactionIdCounter.getAndIncrement());
			transaction.setId(transactionId);
			/*
			 * Oooh, a cunning optimisation. This 'subset view' of committed
			 * transactions will be valid for the lifetime of the transaction,
			 * since it's exactly the set that's preventing those transactions
			 * from being vacuumed
			 */
			transaction.committedTransactions = committedTransactions.isEmpty()
					? new ObjectAVLTreeSet<>()
					: committedTransactions
							.tailSet(committedTransactions.first());
			transaction.startTime = System.currentTimeMillis();
			transaction.highestVisibleCommittedTransactionId = highestVisibleCommittedTransactionId;
			activeTransactions.put(transactionId, transaction);
		}
	}

	void onAddedVacuumable(Transaction transaction, Vacuumable vacuumable) {
		vacuum.addVacuumable(transaction, vacuumable);
	}

	void vacuumComplete(List<Transaction> vacuumableTransactions) {
		synchronized (transactionMetadataLock) {
			committedTransactions.removeAll(vacuumableTransactions);
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
				committedTransactions
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
						: committedTransactions.last().startTime;
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
}

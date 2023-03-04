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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ObjectUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.MvccObjectVersions.MvccObjectVersionsMvccObject;
import cc.alcina.framework.entity.persistence.mvcc.Vacuum.Vacuumable;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@Registration(ClearStaticFieldsOnAppShutdown.class)
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

	public static Map<String, String> primitiveFieldValues(Object object) {
		return ObjectUtil.primitiveFieldValues(object);
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
				// if returning 'domainIdentity', no need to synchronize (the
				// MvccObjects is
				// being used as an identity marker, its fields are still only
				// internally accessible
				// - and the
				// identity itself is immutable). This is true even if during
				// the vacuum copy-to-domain-identity phase
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
					//
					// there's an interplay of locks here that dovetails with
					// MvccObjectVersionsMvccObject.vacuum0
					//
					// our order: get - if non-null, lock MvccObjectVersions
					// instance, validate, resolve.
					//
					// if invalid or null, global lock, ensure (vacuum will not
					// hold both)
					//
					// mutation/concurrent access to instances of
					// MvccObjectVersions handled in-class
					//
					boolean writeableVersion = state == ResolvedVersionState.WRITE;
					if (versions != null) {
						T resolved = versions.resolveWithoutSync(transaction,
								writeableVersion);
						if (resolved != null) {
							return resolved;
						}
						synchronized (versions) {
							if (((MvccObjectVersionsMvccObject) versions).attached) {
								// valid
								return versions.resolve(writeableVersion);
							}
						}
					}
					// fallthrough, invalid or null
					synchronized (MvccObjectVersions.MVCC_OBJECT__MVCC_OBJECT_VERSIONS_MUTATION_MONITOR) {
						// double-check, guard synchronous creation
						versions = mvccObject.__getMvccVersions__();
						if (versions == null
								|| !((MvccObjectVersionsMvccObject) versions).attached) {
							versions = MvccObjectVersions.createEntityVersions(
									t, transaction, false);
						}
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

	public static <K, V> TransactionalTrieEntry<K, V>
			resolve(TransactionalTrieEntry<K, V> t, boolean write) {
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
				// see logic for resolve()
				boolean writeableVersion = write;
				versions = mvccObject.__getMvccVersions__();
				if (versions != null) {
					synchronized (versions) {
						if (versions.domainIdentity != null) {
							// valid
							return versions.resolve(write);
						}
					}
				}
				// fallthrough, invalid or null
				synchronized (MvccObjectVersions.MVCC_OBJECT__MVCC_OBJECT_VERSIONS_MUTATION_MONITOR) {
					if (versions == null || versions.domainIdentity == null) {
						versions = MvccObjectVersions.createTrieEntryVersions(t,
								transaction, false);
					}
					return versions.resolve(write);
				}
			}
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
			ObjectUtil.fieldwiseCopy(from, clone, false, true);
		}
		MvccObjectVersions __getMvccVersions__ = from.__getMvccVersions__();
		clone.__setMvccVersions__(__getMvccVersions__);
		return clone;
	}

	static <T> void copyObjectFields(T from, T to) {
		ObjectUtil.fieldwiseCopy(from, to, false, true);
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

	private Transactions() {
		Configuration.properties.topicInvalidated
				.add(this::configurationInvalidated);
		this.configurationInvalidated();
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

	private void configurationInvalidated() {
		Transaction.retainStartEndTraces = Configuration.is(Transaction.class,
				"retainTraces");
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
		if (!Configuration.is(Transactions.class,
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
							&& age > Configuration.getInt(Transaction.class,
									"warnAgeSecs")
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
					long timeout = Configuration.getInt(Transaction.class,
							"maxAgeSecs") * TimeConstants.ONE_SECOND_MS;
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

	void initialiseTransaction(Transaction transaction,
			Transaction copyVisibleTransactionsFrom) {
		synchronized (transactionMetadataLock) {
			TransactionId transactionId = new TransactionId(
					this.transactionIdCounter.getAndIncrement());
			transaction.setId(transactionId);
			transaction.startTime = System.currentTimeMillis();
			if (copyVisibleTransactionsFrom == null) {
				/*
				 * Oooh, a cunning optimisation. This 'subset view' of committed
				 * transactions will be valid for the lifetime of the
				 * transaction, since it's exactly the set that's preventing
				 * those transactions from being vacuumed
				 */
				transaction.committedTransactions = committedTransactions
						.isEmpty() ? new ObjectAVLTreeSet<>()
								: committedTransactions
										.tailSet(committedTransactions.first());
				transaction.highestVisibleCommittedTransactionId = highestVisibleCommittedTransactionId;
			} else {
				transaction.committedTransactions = copyVisibleTransactionsFrom.committedTransactions;
				transaction.highestVisibleCommittedTransactionId = copyVisibleTransactionsFrom.highestVisibleCommittedTransactionId;
			}
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

		public Thread getActiveVacuumThread() {
			return vacuum.getActiveThread();
		}

		public long getOldestTxStartTime() {
			synchronized (transactionMetadataLock) {
				return committedTransactions.isEmpty() ? 0L
						: committedTransactions.last().startTime;
			}
		}

		public long getTimeInVacuum() {
			long vacuumStarted = vacuum.getVacuumStarted();
			return vacuumStarted == 0 ? 0
					: System.currentTimeMillis() - vacuumStarted;
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

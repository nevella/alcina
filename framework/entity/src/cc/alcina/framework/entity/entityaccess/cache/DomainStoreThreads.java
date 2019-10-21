package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore.DomainStoreException;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreWaitStats.DomainStoreWaitOnLockStat;

/*
 * Public just for inner class access, not to be used outside this package
 */
public class DomainStoreThreads {
	static final int LONG_LOCK_TRACE_LENGTH = 99999;

	private static final int MAX_QUEUED_TIME = 500;

	int maxLockQueueLength;

	long maxLockQueueTimeForNoDisablement;

	boolean checkModificationWriteLock = false;

	Map<Long, Long> threadQueueTimes = new ConcurrentHashMap<>();

	boolean expectLongRunning = false;

	AtomicInteger dumpLocksCount = new AtomicInteger();

	AtomicInteger longLocksCount = new AtomicInteger();

	long lastQueueDumpTime = 0;

	Thread mainLockWriteLock;

	private DomainStore domainStore;

	boolean lockingDisabled;

	long lastLockingDisabledMessage;

	/**
	 * 
	 * synchronize on this for any operations on
	 * activeThreads/activeThreadAcquireTimes
	 */
	CountingMap<Thread> activeThreads = new CountingMap<>();

	Map<Thread, Long> activeThreadAcquireTimes = new LinkedHashMap<>();

	Thread postProcessWriterThread;

	boolean dumpLocks;

	private Set<Thread> waitingOnWriteLock = Collections
			.synchronizedSet(new LinkedHashSet<Thread>());

	private Set<Thread> mainLockReadLock = Collections
			.synchronizedSet(new LinkedHashSet<Thread>());

	/**
	 * Certain post-list triggers can writeLock() without causing readlock
	 * issues (because they deal with areas of the subgraph that the app
	 * guarantees won't cause problems with other reads) - but they do block
	 * writeLock acquisition
	 */
	volatile Object writeLockSubLock = null;

	ReentrantReadWriteLockWithThreadAccess mainLock = new ReentrantReadWriteLockWithThreadAccess(
			true);

	ReentrantReadWriteLock subgraphLock = new ReentrantReadWriteLock(true);

	private ConcurrentHashMap<Thread, Long> lockStartTime = new ConcurrentHashMap<>();

	DomainStoreHealth health = new DomainStoreHealth();

	DomainStoreInstrumentation instrumentation = new DomainStoreInstrumentation();

	private Timer longLockHolderCheckTimer;

	public DomainStoreThreads(DomainStore domainStore) {
		this.domainStore = domainStore;
	}

	public void checkModificationLock(String key) {
		if (!checkModificationWriteLock) {
			return;
		}
		if (!lockingDisabled && key.equals("fire")
				&& !mainLock.isWriteLockedByCurrentThread()
				&& (subgraphLock == null
						|| !subgraphLock.isWriteLockedByCurrentThread())) {
			throw new DomainStoreException(
					"Modification of graph object outside writer thread - "
							+ key);
		}
	}

	public void lock(boolean write) {
		if (LooseContext.is(DomainStore.CONTEXT_NO_LOCKS)) {
			return;
		}
		if (lockingDisabled) {
			if (System.currentTimeMillis()
					- lastLockingDisabledMessage > TimeConstants.ONE_MINUTE_MS) {
				domainStore.logger.error(
						"domain store - lock {} - locking disabled\n", write);
			}
			lastLockingDisabledMessage = System.currentTimeMillis();
			return;
		}
		try {
			if (mainLock.getQueueLength() > maxLockQueueLength && health
					.getMaxQueuedTime() > maxLockQueueTimeForNoDisablement) {
				domainStore.logger.error(
						"Disabling locking due to deadlock:\n***************\n");
				mainLock.getQueuedThreads()
						.forEach(t -> domainStore.logger.info(t + "\n"
								+ CommonUtils.join(t.getStackTrace(), "\n")));
				AlcinaTopics.notifyDevWarning(new DomainStoreException(
						"Disabling locking owing to long queue/deadlock"));
				lockingDisabled = true;
				for (Thread t : waitingOnWriteLock) {
					t.interrupt();
				}
				waitingOnWriteLock.clear();
				return;
			}
			maybeLogLock(DomainStoreLockAction.PRE_LOCK, write);
			if (write) {
				int readHoldCount = mainLock.getReadHoldCount();
				if (readHoldCount > 0) {
					throw new RuntimeException(
							"Trying to acquire write lock from read-locked thread");
				}
				try {
					waitingOnWriteLock.add(Thread.currentThread());
					mainLock.writeLock().lockInterruptibly();
					mainLockWriteLock = Thread.currentThread();
					waitingOnWriteLock.remove(Thread.currentThread());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				mainLock.readLock().lock();
				mainLockReadLock.add(Thread.currentThread());
			}
			lockStartTime.put(Thread.currentThread(),
					System.currentTimeMillis());
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock(DomainStoreLockAction.MAIN_LOCK_ACQUIRED, write);
	}

	public void setupLockedAccessCheck() {
		Registry.impl(DomainStoreLockedAccessChecker.class).start(this);
	}

	public void startLongLockHolderCheck() {
		this.longLockHolderCheckTimer = new Timer(
				"Timer-Domain-Store-check-stats");
		longLockHolderCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				maybeDebugLongLockHolders();
			}
		}, 0, 100);
	}

	/**
	 * Given sublock-guarded code should be able to be run concurrently (as long
	 * as the sublock objects are different), will rework this
	 */
	public void sublock(Object sublock, boolean lock) {
		if (lockingDisabled || LooseContext.is(DomainStore.CONTEXT_NO_LOCKS)) {
			return;
		}
		if (lock) {
			maybeLogLock(DomainStoreLockAction.PRE_LOCK, lock);
			subgraphLock.writeLock().lock();
			writeLockSubLock = sublock;
		} else {
			if (sublock == writeLockSubLock) {
				subgraphLock.writeLock().unlock();
				sublock = null;
			} else {
				// should not be possible
				throw new RuntimeException(String.format(
						"releasing incorrect writer sublock: %s %s", sublock,
						writeLockSubLock));
			}
		}
		maybeLogLock(lock ? DomainStoreLockAction.SUB_LOCK_ACQUIRED
				: DomainStoreLockAction.UNLOCK, lock);
	}

	public void unlock(boolean write) {
		if (lockingDisabled || LooseContext.is(DomainStore.CONTEXT_NO_LOCKS)) {
			return;
		}
		try {
			if (write) {
				if (mainLock.writeLock().isHeldByCurrentThread()) {
					// if not held, we had an exception acquiring the
					// lock...ignore
					mainLock.writeLock().unlock();
					mainLockWriteLock = null;
				}
			} else {
				mainLockReadLock.remove(Thread.currentThread());
				mainLock.readLock().unlock();
			}
			lockStartTime.remove(Thread.currentThread());
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock(DomainStoreLockAction.UNLOCK, write);
	}

	private void maybeLogLock(DomainStoreLockAction action, boolean write) {
		long time = System.currentTimeMillis();
		Thread currentThread = Thread.currentThread();
		if (action == DomainStoreLockAction.PRE_LOCK) {
			threadQueueTimes.put(currentThread.getId(), time);
		} else {
			synchronized (activeThreads) {
				switch (action) {
				case MAIN_LOCK_ACQUIRED:
				case SUB_LOCK_ACQUIRED:
					activeThreads.add(currentThread);
					if (!activeThreadAcquireTimes.containsKey(currentThread)) {
						activeThreadAcquireTimes.put(currentThread, time);
					}
					break;
				case UNLOCK:
					activeThreads.add(currentThread, -1);
					if (activeThreads.get(currentThread) == 0) {
						activeThreads.remove(currentThread);
						activeThreadAcquireTimes.remove(currentThread);
					}
					break;
				}
			}
			threadQueueTimes.remove(currentThread.getId());
		}
		long queuedTime = health.getMaxQueuedTime();
		String lockDumpCause = String.format("DomainStore lock - %s - %s\n",
				write ? "write" : "read", action);
		if (dumpLocks || (write || queuedTime > MAX_QUEUED_TIME)) {
			if (dumpLocksCount.get() > 100) {
				dumpLocks = false;
				return;
			}
			String log = getLockStats();
			lockDumpCause += log;
			if (dumpLocks || (queuedTime > MAX_QUEUED_TIME)) {
				dumpLocksCount.incrementAndGet();
				domainStore.logger.info(getLockDumpString(lockDumpCause, time
						- lastQueueDumpTime > 5 * TimeConstants.ONE_MINUTE_MS));
			}
		}
	}

	protected void maybeDebugLongLockHolders() {
		if (expectLongRunning) {
			return;
		}
		long time = System.currentTimeMillis();
		for (Entry<Thread, Long> e : lockStartTime.entrySet()) {
			long duration = time - e.getValue();
			if (duration > 250 || (duration > 50
					&& e.getKey() == postProcessWriterThread)) {
				if (ResourceUtilities.is(DomainStore.class, "debugLongLocks")) {
					if (longLocksCount.incrementAndGet() > 200) {
						return;
					}
					domainStore.logger.info(
							"Long lock holder - {} ms - {}\n{}\n\n", duration,
							e.getKey(), SEUtilities.getStacktraceSlice(
									e.getKey(), LONG_LOCK_TRACE_LENGTH, 0));
				}
			}
		}
	}

	void appShutdown() {
		if (longLockHolderCheckTimer != null) {
			longLockHolderCheckTimer.cancel();
			longLockHolderCheckTimer = null;
		}
	}

	void dumpLocks() {
		domainStore.logger.info("DomainStore - main: " + mainLock);
		domainStore.logger.info("DomainStore - subgraph: " + subgraphLock);
	}

	String getLockDumpString(String lockDumpCause, boolean full) {
		FormatBuilder fullLockDump = new FormatBuilder();
		Thread writerThread = postProcessWriterThread;
		if (writerThread != null) {
			fullLockDump.format(
					"DomainStore log debugging----------\n"
							+ "Writer thread trace:----------\n" + "%s\n",
					SEUtilities.getStacktraceSlice(writerThread, 999, 0));
			if (full) {
				try {
					fullLockDump.format("Writer thread transforms:\n%s\n\n",
							domainStore.postProcessEvent
									.getDomainTransformLayerWrapper().persistentEvents);
				} catch (Exception e) {
					// outside chance of a race and npe here
					domainStore.logger.info(
							"could not print writer thread transforms - probably inconsequential race");
				}
			}
		}
		fullLockDump.line(lockDumpCause);
		long time = System.currentTimeMillis();
		if (full) {
			fullLockDump.line("Current locked thread dump:\n***************\n");
			mainLock.getQueuedThreads()
					.forEach(t2 -> fullLockDump.line("id:%s %s\n%s", t2.getId(),
							t2, SEUtilities.getStacktraceSlice(t2,
									LONG_LOCK_TRACE_LENGTH, 0)));
			fullLockDump.line("\n\nThread pause times:\n***************\n");
			threadQueueTimes.forEach((id, t2) -> fullLockDump
					.format("id: %s - time: %s\n", id, time - t2));
			synchronized (activeThreads) {
				fullLockDump.line("\n\nActive threads:\n***************\n");
				activeThreads.keySet().forEach(t2 -> {
					long elapsed = System.currentTimeMillis()
							- activeThreadAcquireTimes.get(t2);
					fullLockDump.line(
							"id:%s %s" + "\n\tlock held time: %sms\n%s",
							t2.getId(), t2, elapsed,
							SEUtilities.getStacktraceSlice(t2,
									LONG_LOCK_TRACE_LENGTH, 0));
				});
			}
			lastQueueDumpTime = time;
		}
		return fullLockDump.toString();
	}

	String getLockStats() {
		Thread t = Thread.currentThread();
		String log = CommonUtils.formatJ(
				"\tid:%s\n\ttime: %s\n\treadHoldCount:"
						+ " %s\n\twriteHoldcount: %s\n\tsublock: %s\n\n ",
				t.getId(), new Date(), mainLock.getQueuedReaderThreads().size(),
				mainLock.getQueuedWriterThreads().size(), subgraphLock);
		log += SEUtilities.getStacktraceSlice(t);
		return log;
	}

	boolean isCurrentThreadHoldingLock() {
		if (mainLock.isWriteLockedByCurrentThread()) {
			return true;
		}
		if (subgraphLock.isWriteLockedByCurrentThread()) {
			return true;
		}
		return mainLockReadLock.contains(Thread.currentThread());
	}

	void readLockExpectLongRunning(boolean lock) {
		expectLongRunning = lock;
		if (lock) {
			lock(false);
		} else {
			unlock(false);
		}
	}

	void runWithWriteLock(Runnable runnable) {
		try {
			lock(true);
			runnable.run();
		} finally {
			unlock(true);
		}
	}

	public class DomainStoreHealth {
		public long domainStoreMaxPostProcessTime;

		public long domainStorePostProcessStartTime;

		AtomicInteger domainStoreExceptionCount = new AtomicInteger();

		public AtomicInteger getDomainStoreExceptionCount() {
			return this.domainStoreExceptionCount;
		}

		public int getDomainStoreQueueLength() {
			return mainLock.getQueueLength();
		}

		public long getMaxQueuedTime() {
			return threadQueueTimes.values().stream()
					.min(Comparator.naturalOrder())
					.map(t -> System.currentTimeMillis() - t).orElse(0L);
		}

		public long getTimeInDomainStoreWriter() {
			return domainStorePostProcessStartTime == 0 ? 0
					: System.currentTimeMillis()
							- domainStorePostProcessStartTime;
		}

		public boolean isLockingDisabled() {
			return lockingDisabled;
		}
	}

	public class DomainStoreInstrumentation {
		public long getActiveDomainStoreLockTime(Thread thread) {
			synchronized (activeThreads) {
				return activeThreadAcquireTimes.getOrDefault(thread, 0L);
			}
		}

		public Map<Thread, Long> getActiveDomainStoreLockTimes() {
			synchronized (activeThreads) {
				return activeThreadAcquireTimes.entrySet().stream().collect(
						Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
			}
		}

		public DomainStoreLockState getDomainStoreLockState(Thread thread) {
			synchronized (activeThreads) {
				if (threadQueueTimes.containsKey(thread.getId())) {
					return DomainStoreLockState.WAITING_FOR_LOCK;
				} else {
					if (activeThreadAcquireTimes.containsKey(thread)) {
						if (isWriteLockedByThread(thread)) {
							return DomainStoreLockState.HOLDING_WRITE_LOCK;
						} else {
							return DomainStoreLockState.HOLDING_READ_LOCK;
						}
					} else {
						return DomainStoreLockState.NO_LOCK;
					}
				}
			}
		}

		public DomainStoreWaitStats getDomainStoreWaitStats(Thread thread) {
			DomainStoreWaitStats stats = new DomainStoreWaitStats();
			DomainStoreLockState lockState = getDomainStoreLockState(thread);
			switch (lockState) {
			case HOLDING_READ_LOCK:
			case HOLDING_WRITE_LOCK:
			case NO_LOCK:
				return stats;
			}
			synchronized (activeThreads) {
				stats.waitingOnLockStats = activeThreadAcquireTimes.entrySet()
						.stream().map(e -> {
							DomainStoreWaitOnLockStat stat = new DomainStoreWaitOnLockStat();
							stat.lockTimeMs = System.currentTimeMillis()
									- e.getValue();
							stat.threadId = e.getKey().getId();
							stat.threadName = e.getKey().getName();
							return stat;
						}).collect(Collectors.toList());
			}
			return stats;
		}

		public long getDomainStoreWaitTime(Thread thread) {
			return threadQueueTimes.getOrDefault(thread.getId(), 0L);
		}

		public boolean isLockedByThread(Thread thread) {
			return mainLock.isLockedByThread(thread);
		}

		public boolean isWriteLockedByThread(Thread thread) {
			return mainLock.isWriteLockedByThread(thread);
		}
	}

	@RegistryLocation(registryPoint = DomainStoreLockedAccessChecker.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStoreLockedAccessChecker
			implements TopicListener<Void> {
		Set<String> seenTraces = new LinkedHashSet<>();

		int maxPublished = 1000;

		private DomainStoreThreads threads;

		public DomainStoreLockedAccessChecker() {
		}

		public void start(DomainStoreThreads threads) {
			this.threads = threads;
			DomainStore.topicNonLoggedAccess().add(this);
		}

		@Override
		public synchronized void topicPublished(String key, Void message) {
			if (maxPublished-- <= 0) {
				return;
			}
			String stacktrace = SEUtilities
					.getStacktraceSlice(Thread.currentThread(), 99, 0);
			if (seenTraces.add(stacktrace)) {
				notifyNewLockAccessTrace(stacktrace);
			}
		}

		protected void notifyNewLockAccessTrace(String stacktrace) {
			threads.domainStore.logger.warn(
					"Domain store cache/projection access without lock:\n{}",
					stacktrace);
		}
	}

	final class ReentrantReadWriteLockWithThreadAccess
			extends ReentrantReadWriteLock {
		private ReentrantReadWriteLockWithThreadAccess(boolean fair) {
			super(fair);
		}

		@Override
		public Collection<Thread> getQueuedReaderThreads() {
			return super.getQueuedReaderThreads();
		}

		@Override
		public java.util.Collection<Thread> getQueuedThreads() {
			return super.getQueuedThreads();
		}

		@Override
		public Collection<Thread> getQueuedWriterThreads() {
			return super.getQueuedWriterThreads();
		}

		public boolean isLockedByThread(Thread thread) {
			return mainLockReadLock.contains(thread)
					|| isWriteLockedByThread(thread);
		}

		public boolean isWriteLockedByThread(Thread thread) {
			return mainLockWriteLock == thread;
		}
	}
}
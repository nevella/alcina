package cc.alcina.framework.entity.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsManagerState;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

public abstract class AlcinaChildRunnable implements Runnable {
	public static void launchWithCurrentThreadContext(String threadName,
			ThrowingRunnable runnable) {
		AlcinaChildRunnable wrappingRunnable = new AlcinaChildRunnable(
				threadName) {
			@Override
			protected void run0() throws Exception {
				runnable.run();
			}
		}.withContextSnapshot();
		new Thread(wrappingRunnable).start();
	}

	public static <T> void parallelStream(String name, List<T> items,
			Consumer<T> consumer) {
		CountDownLatch latch = new CountDownLatch(items.size());
		items.stream().forEach(i -> {
			Runnable itemRunnable = () -> consumer.accept(i);
			new AlcinaChildContextRunner(
					Ax.format("%s-%s", name, items.indexOf(i)))
							.callNewThread(itemRunnable, latch);
		});
		try {
			latch.await();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// FIXME - mvcc.jobs - try to avoid this for jobs - declarative jobs and/or
	// alcinachildrunnables
	public static void runInTransaction(String threadName,
			ThrowingRunnable runnable) {
		runInTransaction(threadName, runnable, false, true);
	}

	public static void runInTransaction(String threadName,
			ThrowingRunnable runnable, boolean asRoot,
			boolean throwExceptions) {
		runInTransaction(threadName, runnable, asRoot, throwExceptions, false);
	}

	public static void runInTransaction(String threadName,
			ThrowingRunnable runnable, boolean asRoot, boolean throwExceptions,
			boolean inNewThread) {
		AlcinaChildRunnable wrappingRunnable = new AlcinaChildRunnable(
				threadName) {
			@Override
			protected void run0() throws Exception {
				if (asRoot) {
					ThreadedPermissionsManager.cast()
							.runThrowingWithPushedSystemUserIfNeeded(
									() -> runnable.run());
				} else {
					runnable.run();
				}
			}
		};
		if (inNewThread) {
			Preconditions.checkArgument(!throwExceptions,
					"Can't throw exceptions in a new thread");
			wrappingRunnable.startInNewThread();
		} else {
			try {
				wrappingRunnable.run();
			} catch (RuntimeException e) {
				if (throwExceptions) {
					throw e;
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	public static void runInTransaction(ThrowingRunnable runnable) {
		runInTransaction(null, runnable);
	}

	public static void runInTransactionNewThread(String threadName,
			ThrowingRunnable runnable) {
		runInTransaction(threadName, runnable, false, false, true);
	}

	public static <T> Consumer<T>
			wrapWithCurrentThreadContext(Consumer<T> consumer) {
		LooseContextInstance snapshot = LooseContext.getContext().snapshot();
		return t -> {
			try {
				LooseContext.push();
				LooseContext.putSnapshotProperties(snapshot);
				consumer.accept(t);
			} finally {
				LooseContext.pop();
			}
		};
	}

	public static Runnable wrapWithCurrentThreadContext(Runnable runnable) {
		LooseContextInstance snapshot = LooseContext.getContext().snapshot();
		return () -> {
			try {
				LooseContext.push();
				LooseContext.putSnapshotProperties(snapshot);
				runnable.run();
			} finally {
				LooseContext.pop();
			}
		};
	}

	private boolean inTransaction;

	private PermissionsManagerState permissionsManagerState;

	private String threadName;

	private ClassLoader contextClassLoader;

	protected RunContext runContext = new RunContext();

	Map<String, Object> copyContext = new LinkedHashMap<>();

	private boolean runAsRoot;

	public AlcinaChildRunnable(String name) {
		this.threadName = name;
		this.permissionsManagerState = PermissionsManager.get().snapshotState();
		this.contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (ResourceUtilities.is(AlcinaChildRunnable.class,
				"traceConstruction")) {
			Ax.out("Constructing AlcinaChildRunnable - thread id: %s name: %s\n\n%s",
					Thread.currentThread().getId(), name, SEUtilities
							.getStacktraceSlice(Thread.currentThread(), 30, 2));
		}
	}

	public AlcinaChildRunnable copyContext(String key) {
		copyContext.put(key, LooseContext.get(key));
		return this;
	}

	public AlcinaChildRunnable logExceptions() {
		getRunContext().logExceptions = true;
		return this;
	}

	@Override
	public void run() {
		if (threadName != null) {
			Thread.currentThread().setName(threadName);
		}
		inTransaction = Transaction.isInTransaction();
		try {
			LooseContext.push();
			// different thread-local
			getRunContext().tLooseContextDepth = LooseContext.depth();
			this.permissionsManagerState.copyTo(PermissionsManager.get());
			Thread.currentThread().setContextClassLoader(contextClassLoader);
			copyContext.forEach((k, v) -> LooseContext.set(k, v));
			if (runAsRoot) {
				ThreadedPermissionsManager.cast()
						.pushSystemOrCurrentUserAsRoot();
			}
			Transaction.ensureBegun();
			run0();
		} catch (OutOfMemoryError e) {
			SEUtilities.dumpAllThreads();
			throw e;
		} catch (Throwable throwable) {
			if (getRunContext().logExceptions) {
				throwable.printStackTrace();
				EntityLayerLogging.persistentLog(
						LogMessageType.WORKER_THREAD_EXCEPTION,
						SEUtilities.getFullExceptionMessage(throwable));
			}
			if (throwable instanceof RuntimeException) {
				throw ((RuntimeException) throwable);
			}
			throw new RuntimeException(throwable);
		} finally {
			if (!inTransaction) {
				Transaction.ensureEnded();
			}
			if (runAsRoot) {
				ThreadedPermissionsManager.cast().popSystemOrCurrentUser();
			}
			LooseContext.confirmDepth(getRunContext().tLooseContextDepth);
			LooseContext.pop();
		}
	}

	public Thread startInNewThread() {
		Thread thread = new Thread(this);
		thread.start();
		return thread;
	}

	public AlcinaChildRunnable withContext(String key, Object value) {
		copyContext.put(key, value);
		return this;
	}

	public AlcinaChildRunnable withContextSnapshot() {
		copyContext.putAll(LooseContext.getContext().properties);
		return this;
	}

	public AlcinaChildRunnable withRunAsRoot() {
		runAsRoot = true;
		return this;
	}

	protected RunContext getRunContext() {
		return runContext;
	}

	protected abstract void run0() throws Exception;

	public static class AlcinaChildContextRunner extends AlcinaChildRunnable {
		ThreadLocal<RunContext> contexts = new ThreadLocal<AlcinaChildRunnable.RunContext>() {
			@Override
			protected RunContext initialValue() {
				return new RunContext();
			}
		};

		private long launcherThreadId;

		public Throwable thrown;

		public Object result;

		public AlcinaChildContextRunner(String name) {
			super(name);
			launcherThreadId = Thread.currentThread().getId();
		}

		public Object call(Runnable runnable) {
			if (Thread.currentThread().getId() == launcherThreadId) {
				// don't do anything fancy to the context (e.g. fork/join pool)
				runnable.run();
				return null;
			} else {
				getRunContext().runnable = runnable;
				run();
				return null;
			}
		}

		public Object callNewThread(Runnable runnable) {
			return callNewThread(runnable, null);
		}

		public Object callNewThread(Runnable runnable, CountDownLatch latch) {
			new Thread() {
				@Override
				public void run() {
					getRunContext().runnable = runnable;
					try {
						AlcinaChildContextRunner.this.run();
					} finally {
						if (latch != null) {
							latch.countDown();
						}
					}
				}
			}.start();
			return null;
		}

		public Object callNewThreadOrCurrent(Runnable runnable,
				CountDownLatch latch, boolean newThread) {
			if (newThread) {
				callNewThread(runnable, latch);
			} else {
				try {
					runnable.run();
				} finally {
					if (latch != null) {
						latch.countDown();
					}
				}
			}
			return null;
		}

		@Override
		public AlcinaChildContextRunner copyContext(String key) {
			super.copyContext(key);
			return this;
		}

		@Override
		protected void run0() throws Exception {
			getRunContext().runnable.run();
		}
	}

	class RunContext {
		private int tLooseContextDepth;

		private boolean logExceptions = true;

		private Runnable runnable;
	}
}
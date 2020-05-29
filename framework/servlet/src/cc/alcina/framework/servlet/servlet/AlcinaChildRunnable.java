package cc.alcina.framework.servlet.servlet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

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
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.logic.EntityLayerLogging;

public abstract class AlcinaChildRunnable implements Runnable {
	public static void launchWithCurrentThreadContext(String threadName,
			Runnable runnable) {
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

	// FIXME.mvcc.2 - try to avoid this - declarative jobs and/or
	// alcinachildrunnables
	public static void runInTransaction(ThrowingRunnable runnable) {
		AlcinaChildRunnable wrappingRunnable = new AlcinaChildRunnable(null) {
			@Override
			protected void run0() throws Exception {
				runnable.run();
			}
		};
		wrappingRunnable.run();
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

	private PermissionsManagerState permissionsManagerState;

	private String threadName;

	private ClassLoader contextClassLoader;

	protected RunContext runContext = new RunContext();

	Map<String, Object> copyContext = new LinkedHashMap<>();

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
		try {
			LooseContext.push();
			// different thread-local
			getRunContext().tLooseContextDepth = LooseContext.depth();
			this.permissionsManagerState.copyTo(PermissionsManager.get());
			Thread.currentThread().setContextClassLoader(contextClassLoader);
			copyContext.forEach((k, v) -> LooseContext.set(k, v));
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
			Transaction.ensureEnded();
			LooseContext.confirmDepth(getRunContext().tLooseContextDepth);
			LooseContext.pop();
		}
	}

	public AlcinaChildRunnable withContext(String key, Object value) {
		copyContext.put(key, value);
		return this;
	}

	public AlcinaChildRunnable withContextSnapshot() {
		copyContext.putAll(LooseContext.getContext().properties);
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
package cc.alcina.framework.servlet.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsManagerState;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

public abstract class AlcinaChildRunnable implements Runnable {
	private PermissionsManagerState permissionsManagerState;

	private String threadName;

	private ClassLoader contextClassLoader;

	protected RunContext runContext = new RunContext();

	class RunContext {
		private int tLooseContextDepth;

		private boolean logExceptions = false;

		private Runnable runnable;
	}

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

	public static class AlcinaChildContextRunner extends AlcinaChildRunnable {
		public AlcinaChildContextRunner(String name) {
			super(name);
			launcherThreadId = Thread.currentThread().getId();
		}

		ThreadLocal<RunContext> contexts = new ThreadLocal<AlcinaChildRunnable.RunContext>() {
			protected RunContext initialValue() {
				return new RunContext();
			}
		};

		private long launcherThreadId;

		@Override
		protected void run0() throws Exception {
			getRunContext().runnable.run();
		}

		public Object callNewThread(Runnable runnable) {
			new Thread() {
				@Override
				public void run() {
					getRunContext().runnable = runnable;
					AlcinaChildContextRunner.this.run();
				}
			}.start();
			return null;
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
	}

	protected RunContext getRunContext() {
		return runContext;
	}

	public AlcinaChildRunnable logExceptions() {
		getRunContext().logExceptions = true;
		return this;
	}

	Map<String, Object> copyContext = new LinkedHashMap<>();

	public AlcinaChildRunnable copyContext(String key) {
		copyContext.put(key, LooseContext.get(key));
		return this;
	}

	protected abstract void run0() throws Exception;

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
			run0();
		} catch (OutOfMemoryError e) {
			SEUtilities.threadDump();
			throw e;
		} catch (Throwable e) {
			if (getRunContext().logExceptions) {
				e.printStackTrace();
			}
			if (e instanceof RuntimeException) {
				throw ((RuntimeException) e);
			}
			throw new RuntimeException(e);
		} finally {
			LooseContext.confirmDepth(getRunContext().tLooseContextDepth);
			LooseContext.pop();
		}
	}
}
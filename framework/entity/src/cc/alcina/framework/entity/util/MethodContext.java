package cc.alcina.framework.entity.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissions;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

public class MethodContext {
	public static MethodContext instance() {
		return new MethodContext();
	}

	public static <T> T uncheckExceptions(Callable<T> callable) {
		return instance().call(callable);
	}

	private boolean rootPermissions;

	private Map<String, Object> context = new LinkedHashMap<>();

	private String metricKey;

	private Class metricKeyClass;

	private boolean wrappingTransaction;

	private ClassLoader contextClassLoader;

	private ClassLoader entryClassLoader;

	private boolean executeOutsideTransaction;

	private boolean runInNewThread;

	private String threadName;

	public <T> T call(Callable<T> callable) {
		if (runInNewThread) {
			String name = callable.getClass().getName();
			if (name.contains("")) {
				name = "(lambda)";
			}
			AlcinaChildRunnable.runInTransactionNewThread(
					Ax.format("child-thread-%s", name), () -> callable.call());
			return null;
		}
		Thread currentThread = Thread.currentThread();
		entryClassLoader = currentThread.getContextClassLoader();
		boolean pushedRoot = false;
		boolean inTransaction = Transaction.isInTransaction();
		int looseContextDepth = 0;
		String entryThreadName = currentThread.getName();
		try {
			if (wrappingTransaction && !inTransaction) {
				Transaction.begin();
			}
			if (executeOutsideTransaction && inTransaction) {
				Transaction.end();
			}
			if (!context.isEmpty()) {
				LooseContext.push();
				looseContextDepth = LooseContext.depth();
				context.entrySet().forEach(e -> LooseContext.getContext()
						.set(e.getKey(), e.getValue()));
			}
			if (threadName != null) {
				currentThread.setName(threadName);
			}
			if (metricKey != null) {
				MetricLogging.get().start(metricKey);
			}
			if (rootPermissions && !Permissions.isRoot()) {
				Permissions.pushSystemOrCurrentUserAsRoot();
				pushedRoot = true;
			}
			if (contextClassLoader != null) {
				currentThread.setContextClassLoader(contextClassLoader);
			}
			return callable.call();
		} catch (Throwable e) {
			e.printStackTrace();
			throw WrappedRuntimeException.wrap(e);
		} finally {
			try {
				currentThread.setContextClassLoader(entryClassLoader);
				if (pushedRoot) {
					Permissions.popContext();
				}
				if (metricKey != null) {
					Logger logger = metricKeyClass == null ? null
							: LoggerFactory.getLogger(metricKeyClass);
					MetricLogging.get().end(metricKey, logger);
				}
				if (threadName != null) {
					currentThread.setName(entryThreadName);
				}
				if (!context.isEmpty()) {
					LooseContext.confirmDepth(looseContextDepth);
					LooseContext.pop();
				}
				if (wrappingTransaction && !inTransaction) {
					Transaction.end();
				}
				if (executeOutsideTransaction && inTransaction) {
					/*
					 * a transaction may have been (incorrectly) started during
					 * the method code
					 */
					Transaction.ensureEnded();
					Transaction.begin();
				}
			} catch (Throwable e) {
				Ax.out("DEVEX::0 - Exception in methodcontext/finally");
				e.printStackTrace();
				throw e;
			}
		}
	}

	public void run(ThrowingRunnable runnable) {
		call(() -> {
			runnable.run();
			return null;
		});
	}

	public MethodContext
			withContextClassloader(ClassLoader contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
		return this;
	}

	public MethodContext withContextTrue(String key) {
		context.put(key, Boolean.TRUE);
		return this;
	}

	public MethodContext withContextValue(String key, Object value) {
		context.put(key, value);
		return this;
	}

	public MethodContext
			withExecuteOutsideTransaction(boolean executeOutsideTransaction) {
		this.executeOutsideTransaction = executeOutsideTransaction;
		return this;
	}

	public MethodContext withMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	/**
	 * 
	 * @param metricKeyClass
	 *            will be used as the Logger source class
	 * @return
	 */
	public MethodContext withMetricKeyClass(Class metricKeyClass) {
		withMetricKey(NestedName.get(metricKeyClass));
		this.metricKeyClass = metricKeyClass;
		return this;
	}

	public MethodContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}

	public MethodContext withRunInNewThread(boolean runInNewThread) {
		this.runInNewThread = runInNewThread;
		return this;
	}

	public MethodContext withWrappingTransaction() {
		this.wrappingTransaction = true;
		return this;
	}

	public MethodContext withThreadName(String threadName) {
		this.threadName = threadName;
		return this;
	}

	public MethodContext withContextTrue(LooseContext.Key contextKey) {
		return withContextTrue(contextKey.getPath());
	}
}

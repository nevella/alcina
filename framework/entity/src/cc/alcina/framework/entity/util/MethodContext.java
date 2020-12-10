package cc.alcina.framework.entity.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
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

	private boolean wrappingTransaction;

	private ClassLoader contextClassLoader;

	private ClassLoader entryClassLoader;

	private boolean executeOutsideTransaction;

	public <T> T call(Callable<T> callable) {
		entryClassLoader = Thread.currentThread().getContextClassLoader();
		boolean inTransaction = Transaction.isInTransaction();
		try {
			if (wrappingTransaction && !inTransaction) {
				Transaction.begin();
			}
			if (executeOutsideTransaction && inTransaction) {
				Transaction.end();
			}
			if (!context.isEmpty()) {
				LooseContext.push();
				context.entrySet().forEach(e -> LooseContext.getContext()
						.set(e.getKey(), e.getValue()));
			}
			if (metricKey != null) {
				MetricLogging.get().start(metricKey);
			}
			if (rootPermissions
					&& !ThreadedPermissionsManager.cast().isRoot()) {
				ThreadedPermissionsManager.cast()
						.pushSystemOrCurrentUserAsRoot();
			}
			if (contextClassLoader != null) {
				Thread.currentThread()
						.setContextClassLoader(contextClassLoader);
			}
			return callable.call();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(entryClassLoader);
			if (rootPermissions
					&& !ThreadedPermissionsManager.cast().isRoot()) {
				ThreadedPermissionsManager.cast().popUser();
			}
			if (metricKey != null) {
				MetricLogging.get().end(metricKey);
			}
			if (!context.isEmpty()) {
				LooseContext.pop();
			}
			if (wrappingTransaction && !inTransaction) {
				Transaction.end();
			}
			if (executeOutsideTransaction && inTransaction) {
				Transaction.begin();
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

	public MethodContext withExecuteOutsideTransaction() {
		this.executeOutsideTransaction = true;
		return this;
	}

	public MethodContext withMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public MethodContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}

	public MethodContext withWrappingTransaction() {
		this.wrappingTransaction = true;
		return this;
	}
}

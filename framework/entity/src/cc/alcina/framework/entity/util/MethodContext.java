package cc.alcina.framework.entity.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;

public class MethodContext {
	public static MethodContext instance() {
		return new MethodContext();
	}

	private boolean rootPermissions;

	private Map<String, Object> context = new LinkedHashMap<>();

	private String metricKey;

	private boolean wrappingTransaction;

	public <T> T call(Callable<T> callable) {
		try {
			if (wrappingTransaction) {
				Transaction.ensureBegun();
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
			return callable.call();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
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
			if (wrappingTransaction) {
				Transaction.endAndBeginNew();
			}
		}
	}

	public void run(ThrowingRunnable runnable) {
		call(() -> {
			runnable.run();
			return null;
		});
	}

	public MethodContext withContextTrue(String key) {
		context.put(key, Boolean.TRUE);
		return this;
	}

	public MethodContext withMetricKey(String metricKey) {
		this.metricKey = metricKey;
		return this;
	}

	public MethodContext withRootPermissions() {
		this.rootPermissions = true;
		return this;
	}

	public MethodContext withWrappingTransaction() {
		this.wrappingTransaction = true;
		return this;
	}
}

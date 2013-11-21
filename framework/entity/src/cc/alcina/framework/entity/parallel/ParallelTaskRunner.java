package cc.alcina.framework.entity.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;

public abstract class ParallelTaskRunner {
	protected long threadId;

	protected ArrayList<Callable<Object>> tasks;

	protected PermissionsManager pm;

	protected abstract Logger getLogger();

	public ParallelTaskRunner() {
	}

	protected ExecutorService getExecutorService() {
		return Registry.impl(ParallelTaskPool.class).getExecutorService();
	}

	protected void init() {
		MetricLogging.get().start(getMetricKey());
		this.threadId = Thread.currentThread().getId();
		tasks = new ArrayList<Callable<Object>>();
		pm = PermissionsManager.get();
	}

	protected void threadStartup(PermissionsManager pm) {
		if (runningAsync) {
			pm.copyTo(PermissionsManager.get());
		}
	}

	protected boolean runningAsync = true;

	protected abstract String getMetricKey();

	protected abstract String getTaskName();

	public void runTasks() throws WebException {
		try {
			List<Future<Object>> futures = getExecutorService()
					.invokeAll(tasks);
			// manky logic here, but unable to get stacktraces for
			// futureExceptions
			try {
				SEUtilities.throwFutureException(futures);
			} catch (Exception e) {
				handleAsyncException(e);
			}
		} catch (Exception e) {
			handleSyncException(e);
		} finally {
			MetricLogging.get().end(getMetricKey());
		}
	}

	protected void handleAsyncException(Exception e) throws Exception {
		runningAsync = false;
		System.out.format("rerunning %s() to catch stacktraces\n",
				getTaskName());
		for (Callable<Object> callable : tasks) {
			callable.call();
		}
	}

	protected void handleSyncException(Exception e) throws WebException {
		throw new WebException(e.getMessage());
	}
}

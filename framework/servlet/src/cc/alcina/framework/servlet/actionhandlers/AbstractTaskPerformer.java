package cc.alcina.framework.servlet.actionhandlers;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.entity.MetricLogging;

public abstract class AbstractTaskPerformer implements Runnable {
	public Logger actionLogger;

	public JobTracker jobTracker;

	public String value;

	public String result;

	public void runLogged() {
		try {
			MetricLogging.get().start(getClass().getSimpleName());
			System.out
					.format("Starting task: %s\n", getClass().getSimpleName());
			run();
		} finally {
			System.out.format("Ended task: %s\n", getClass().getSimpleName());
			MetricLogging.get().end(getClass().getSimpleName());
		}
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void run0() throws Exception;
}

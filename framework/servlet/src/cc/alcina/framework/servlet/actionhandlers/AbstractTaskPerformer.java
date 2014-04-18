package cc.alcina.framework.servlet.actionhandlers;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;

public abstract class AbstractTaskPerformer implements Runnable {
	public Logger actionLogger;

	public JobTracker jobTracker;

	public String value;

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

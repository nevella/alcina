package cc.alcina.framework.servlet.actionhandlers;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class AbstractTaskPerformer implements Runnable {
	public Logger actionLogger;

	public JobTracker jobTracker;

	public String value;

	public String result;

	public AbstractTaskPerformer asSubTask(AbstractTaskPerformer parent) {
		actionLogger = parent.actionLogger;
		jobTracker = parent.jobTracker;
		return this;
	}

	@Override
	public void run() {
		run(true);
	}

	public void runLogged() {
		try {
			MetricLogging.get().start(getClass().getSimpleName());
			System.out.format("Starting task: %s\n",
					getClass().getSimpleName());
			run();
		} finally {
			System.out.format("Ended task: %s\n", getClass().getSimpleName());
			MetricLogging.get().end(getClass().getSimpleName());
		}
	}

	public void runNoThrow() {
		run(false);
	}

	public AbstractTaskPerformer withValue(String value) {
		this.value = value;
		return this;
	}

	private void run(boolean throwExceptions) {
		KnownJob knownJob = getKnownJob();
		try {
			LooseContext.push();
			if (knownJob != null) {
				knownJob.startJob();
			}
			run0();
			if (knownJob != null) {
				getKnownJob().jobOk(result);
			}
		} catch (Exception e) {
			if (knownJob != null) {
				getKnownJob().jobError(e);
			}
			if (throwExceptions) {
				throw new WrappedRuntimeException(e);
			} else {
				e.printStackTrace();
			}
		} finally {
			LooseContext.pop();
		}
	}

	protected KnownJob getKnownJob() {
		return null;
	}

	protected abstract void run0() throws Exception;
}

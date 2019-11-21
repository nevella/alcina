package cc.alcina.framework.servlet.actionhandlers;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class AbstractTaskPerformer implements Runnable {
	public Logger actionLogger;

	protected org.slf4j.Logger slf4jLogger = LoggerFactory
			.getLogger(getClass());

	public JobTracker jobTracker;

	public String value;

	public String result;

	protected boolean cancelled;

	public AbstractTaskPerformer asSubTask(AbstractTaskPerformer parent) {
		actionLogger = parent.actionLogger;
		jobTracker = parent.jobTracker;
		return this;
	}

	public AbstractTaskPerformer asSubTask(BaseRemoteActionPerformer parent) {
		actionLogger = parent.getLogger();
		jobTracker = parent.getJobTracker();
		return this;
	}

	public void cancel() {
		this.cancelled = true;
	}

	@Override
	public void run() {
		run(true);
	}

	public void runAsSubtask(AbstractTaskPerformer parent) {
		asSubTask(parent).run();
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
			String message = Ax.format("Started job: %s", getClass().getName());
			if (actionLogger != null) {
				actionLogger.info(message);
			} else {
				Ax.out(message);
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

	protected void logDebug(String template, Object... args) {
		actionLogger.debug(Ax.format(template, args));
	}

	protected void logInfo(String template, Object... args) {
		actionLogger.info(Ax.format(template, args));
	}

	protected void logWarn(String template, Object... args) {
		actionLogger.warn(Ax.format(template, args));
	}

	protected abstract void run0() throws Exception;
}

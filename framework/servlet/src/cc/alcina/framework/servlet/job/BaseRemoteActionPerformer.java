package cc.alcina.framework.servlet.job;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.entity.SEUtilities;

public abstract class BaseRemoteActionPerformer<R extends RemoteAction>
		implements RemoteActionPerformer<R> {
	// FIXME - mvcc.jobs - switch to slf4j. Also - logstash logging - put a
	// counter in the log record?
	protected Logger logger;

	protected org.slf4j.Logger slf4jLogger = LoggerFactory
			.getLogger(getClass());

	protected JobTracker jobTracker;

	boolean started;

	public JobTracker getJobTracker() {
		return jobTracker;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public synchronized void updateJob(String message) {
		updateJob(message, 1);
	}

	public void updateJob(String message, int completedDelta) {
		JobRegistry.get().updateJob(message, completedDelta);
	}

	protected void finishJob() {
	}

	protected void jobError(Exception exception) {
		JobRegistry.get().jobError(exception);
	}

	protected void jobError(String message) {
		JobRegistry.get().jobError(message);
	}

	protected String jobName() {
		return SEUtilities.friendlyClassName(getClass());
	}

	protected void jobOk(String message) {
		JobRegistry.get().jobOk(message);
	}

	protected void jobStarted() {
		if (started) {
			throw new RuntimeException("Already started");
		}
		started = true;
		jobTracker = JobRegistry.get().startJob(getClass(), jobName(), null);
		logger = JobRegistry.get().getContextLogger();
	}
}

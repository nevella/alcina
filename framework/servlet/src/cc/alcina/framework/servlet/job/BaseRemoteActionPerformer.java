package cc.alcina.framework.servlet.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.entity.SEUtilities;

public abstract class BaseRemoteActionPerformer<R extends RemoteAction>
		implements TaskPerformer<R> {
	boolean started;

	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	public synchronized void updateJob(String message) {
		updateJob(message, 1);
	}

	public void updateJob(String message, int completedDelta) {
		if (JobContext.has()) {
			JobContext.get().updateJob(message, completedDelta);
		} else {
			logger.info("Update job: {} {}", message, completedDelta);
		}
	}

	protected void finishJob() {
	}

	protected void jobError(Exception exception) {
		if (JobContext.has()) {
			JobContext.get().onJobException(exception);
		} else {
			exception.printStackTrace();
		}
	}

	protected void jobError(String message) {
		if (JobContext.has()) {
			JobContext.get().onJobException(new Exception(message));
		} else {
			new Exception(message).printStackTrace();
		}
	}

	protected String jobName() {
		return SEUtilities.friendlyClassName(getClass());
	}

	protected void jobOk(String message) {
		if (JobContext.has()) {
			JobContext.setResultMessage(message);
		} else {
			logger.info("Job OK: {} {}", message);
		}
	}
}

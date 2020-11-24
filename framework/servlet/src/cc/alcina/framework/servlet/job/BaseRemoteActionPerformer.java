package cc.alcina.framework.servlet.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.entity.SEUtilities;

public abstract class BaseRemoteActionPerformer<R extends RemoteAction>
		implements TaskPerformer<R> {
	boolean started;

	protected Logger logger = LoggerFactory.getLogger(getClass());

	public synchronized void updateJob(String message) {
		updateJob(message, 1);
	}

	public void updateJob(String message, int completedDelta) {
		JobContext.get().updateJob(message, completedDelta);
	}

	protected void finishJob() {
	}

	protected void jobError(Exception exception) {
		JobContext.get().onJobException(exception);
	}

	protected void jobError(String message) {
		JobContext.get().onJobException(new Exception(message));
	}

	protected String jobName() {
		return SEUtilities.friendlyClassName(getClass());
	}

	protected void jobOk(String message) {
		JobContext.get().setResultMessage(message);
	}
}

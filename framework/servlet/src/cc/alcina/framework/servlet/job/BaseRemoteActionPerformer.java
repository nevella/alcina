package cc.alcina.framework.servlet.job;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.entity.SEUtilities;

public abstract class BaseRemoteActionPerformer<R extends RemoteAction>
		implements RemoteActionPerformer<R> {
	protected Logger logger;

	protected JobTracker jobTracker;

	boolean started;

	public JobTracker getJobTracker() {
		return jobTracker;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public void updateJob(String message) {
		JobRegistry.get().updateJob(message);
		updateJob(message, jobTracker.provideIsRoot() ? 1 : 0);
	}
	public void updateJob(String message, int completedDelta) {
		long itemsCompleted = jobTracker.getItemsCompleted();
		long itemCount = jobTracker.getItemCount();
		itemsCompleted += completedDelta;
		jobTracker.setItemsCompleted(itemsCompleted);
		double progress = ((double) itemsCompleted) / ((double) itemCount);
		JobRegistry.get().jobProgress(
				String.format("(%s/%s) -  %s", itemsCompleted, itemCount,
						message), progress);
	}

	protected void finishJob() {
	}

	protected void jobError(Exception exception) {
		JobRegistry.get().jobError(exception);
	}

	protected void jobError(String message) {
		JobRegistry.get().jobError(message);
	}

	protected void jobOk(String message) {
		JobRegistry.get().jobOk(message);
	}

	protected void jobStarted() {
		if(started){
			throw new RuntimeException("Already started");
		}
		started=true;
		jobTracker = JobRegistry.get().startJob(getClass(),
				SEUtilities.friendlyClassName(getClass()), null);
		logger = JobRegistry.get().getContextLogger();
	}
}

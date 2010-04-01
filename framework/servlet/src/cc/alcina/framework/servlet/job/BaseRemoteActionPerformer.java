package cc.alcina.framework.servlet.job;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.ServerLayerLocator;


public abstract class BaseRemoteActionPerformer {

	protected Logger logger;

	public Logger getLogger() {
		return this.logger;
	}

	protected JobInfo jobInfo;
	protected long itemCount;
	public long getItemCount() {
		return this.itemCount;
	}
	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}

	protected long itemsCompleted;
	public void updateJob(String message) {
		updateJob(message,1);
	}
	public void updateJob(String message, int completedDelta) {
		itemsCompleted+=completedDelta;
		double progress = ((double) itemsCompleted)
				/ ((double) itemCount);
		JobRegistry.get().jobProgress(
				jobInfo,
				String.format("(%s/%s) -  %s", itemsCompleted, itemCount,
						message), progress);
	}
	protected void startJob(){
		logger = ServerLayerLocator.get().remoteActionLoggerProvider()
		.getLogger(this.getClass());
		jobInfo = JobRegistry.get().startJob(getClass(),
				SEUtilities.friendlyClassName(getClass()), null);
	}
	protected void finishJob(){
		
		
	}
}

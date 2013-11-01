package cc.alcina.framework.servlet.job;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;


public abstract class BaseRemoteActionPerformer {

	protected Logger logger;

	public Logger getLogger() {
		return this.logger;
	}

	protected JobInfo jobInfo;
	private long itemCount;
	public long getItemCount() {
		return this.itemCount;
	}
	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}

	protected long itemsCompleted;
	public void updateJob(String message) {
		
		updateJob(message,JobRegistry.get().isTopLevel(jobInfo)?1:0);
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
		logger = Registry.impl(RemoteActionLoggerProvider.class)
		.getLogger(this.getClass());
		jobInfo = JobRegistry.get().startJob(getClass(),
				SEUtilities.friendlyClassName(getClass()), null);
	}
	protected void finishJob(){
		
		
	}
}

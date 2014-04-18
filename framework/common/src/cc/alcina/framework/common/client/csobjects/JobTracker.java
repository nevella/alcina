package cc.alcina.framework.common.client.csobjects;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.gwt.client.logic.LogLevel;

public interface JobTracker {
	public abstract long getItemCount();

	public abstract void setItemCount(long itemCount);

	public abstract List<JobTracker> getChildren();

	public abstract Date getEndTime();

	public abstract String getId();

	public abstract double getJobDuration();

	public abstract Exception getjobError();

	public abstract String getJobLauncher();

	public abstract String getJobName();

	public abstract String getJobResult();

	public abstract JobResultType getJobResultType();

	public abstract JobTracker getParent();

	public abstract String getProgressMessage();

	public abstract Date getStartTime();

	public abstract boolean isCancelled();

	public abstract boolean isComplete();

	public abstract boolean provideIsRoot();

	public abstract JobTracker root();

	public abstract void setCancelled(boolean cancelled);

	public abstract void setChildren(List<JobTracker> children);

	public abstract void setComplete(boolean complete);

	public abstract void setEndTime(Date endTime);

	public abstract void setId(String id);

	public abstract void setjobError(Exception jobException);

	public abstract void setJobLauncher(String jobLauncher);

	public abstract void setJobName(String jobName);

	public abstract void setJobResult(String jobResult);

	public abstract void setJobResultType(JobResultType jobResultType);

	public abstract void setParent(JobTracker parent);

	public abstract void setProgressMessage(String progressMessage);

	public abstract void setStartTime(Date startTime);

	public abstract String getLog();

	public abstract void setLog(String log);

	public abstract Object getLogger();

	public abstract void setLogger(Object logger);

	public abstract LogLevel getLogLevel();

	public abstract void setLogLevel(LogLevel logLevel);

	public abstract long getItemsCompleted();

	public abstract void setItemsCompleted(long itemsCompleted);

	public abstract double getPercentComplete();

	public abstract void setPercentComplete(double percentComplete);

	public abstract void updateJob(int completedDelta);

	public abstract void setJobResultObject(Object jobResultObject);

	public abstract Object getJobResultObject();
}
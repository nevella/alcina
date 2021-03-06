package cc.alcina.framework.common.client.csobjects;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.LogLevel;

public interface JobTracker {
	public abstract void childComplete(JobTracker tracker);

	public abstract JobTracker exportableForm();

	public abstract List<JobTracker> getChildren();

	public abstract Date getEndTime();

	public abstract String getId();

	public abstract long getItemCount();

	public abstract long getItemsCompleted();

	public abstract double getJobDuration();

	public abstract Exception getJobException();

	public abstract String getJobLauncher();

	public abstract String getJobName();

	public abstract String getJobResult();

	public abstract Object getJobResultObject();

	public abstract JobResultType getJobResultType();

	public abstract String getLog();

	public abstract Object getLogger();

	public abstract LogLevel getLogLevel();

	public abstract JobTracker getParent();

	public abstract double getPercentComplete();

	public abstract String getProgressMessage();

	public abstract Date getStartTime();

	public abstract String getSubProgressMessage();

	public abstract boolean isCancelled();

	public abstract boolean isComplete();

	public abstract boolean provideIsRoot();

	public abstract JobTracker root();

	public abstract void setCancelled(boolean cancelled);

	public abstract void setChildren(List<JobTracker> children);

	public abstract void setComplete(boolean complete);

	public abstract void setEndTime(Date endTime);

	public abstract void setId(String id);

	public abstract void setItemCount(long itemCount);

	public abstract void setItemsCompleted(long itemsCompleted);

	public abstract void setJobException(Exception jobException);

	public abstract void setJobLauncher(String jobLauncher);

	public abstract void setJobName(String jobName);

	public abstract void setJobResult(String jobResult);

	public abstract void setJobResultObject(Object jobResultObject);

	public abstract void setJobResultType(JobResultType jobResultType);

	public abstract void setLog(String log);

	public abstract void setLogger(Object logger);

	public abstract void setLogLevel(LogLevel logLevel);

	public abstract void setParent(JobTracker parent);

	public abstract void setPercentComplete(double percentComplete);

	public abstract void setProgressMessage(String progressMessage);

	public abstract void setStartTime(Date startTime);

	public abstract void setSubProgressMessage(String subProgressMessage);

	public abstract void updateJob(int completedDelta);

	default void startup(Class jobClass, String jobName, String message) {
		setComplete(false);
		setJobName(jobName == null ? CommonUtils.simpleClassName(jobClass)
				: jobName);
		setPercentComplete(0);
		setProgressMessage(message != null ? message : "Starting job...");
		setStartTime(new Date());
	}

	default void updatePercent() {
		setPercentComplete((getItemCount() == 0 ? 0.0
				: ((double) getItemsCompleted()) / ((double) getItemCount())));
	}
}
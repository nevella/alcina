package cc.alcina.framework.common.client.csobjects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.model.Model;

public interface JobTracker {
	public abstract Date getEndTime();

	public abstract String getId();

	public abstract String getJobName();

	public abstract String getJobResult();

	public abstract JobResultType getJobResultType();

	public abstract String getLog();

	public abstract double getPercentComplete();

	public abstract String getProgressMessage();

	public abstract Date getStartTime();

	public abstract boolean isCancelled();

	public abstract boolean isComplete();

	public abstract void setCancelled(boolean cancelled);

	public abstract void setComplete(boolean complete);

	public abstract void setEndTime(Date endTime);

	public abstract void setId(String id);

	public abstract void setJobName(String jobName);

	public abstract void setJobResult(String jobResult);

	public abstract void setJobResultType(JobResultType jobResultType);

	public abstract void setLog(String log);

	public abstract void setPercentComplete(double percentComplete);

	public abstract void setProgressMessage(String progressMessage);

	public abstract void setStartTime(Date startTime);

	String getSerializedResult();

	public static class Request extends Model {
		private List<Long> ids = new ArrayList<>();

		public List<Long> getIds() {
			return this.ids;
		}

		public void setIds(List<Long> ids) {
			this.ids = ids;
		}
	}

	public static class Response extends Model {
		private List<JobTracker> trackers = new ArrayList<>();

		public List<JobTracker> getTrackers() {
			return this.trackers;
		}

		public void setTrackers(List<JobTracker> trackers) {
			this.trackers = trackers;
		}
	}
}
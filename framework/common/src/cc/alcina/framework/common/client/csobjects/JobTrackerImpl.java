/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.LogLevel;

import com.google.gwt.user.client.rpc.GwtTransient;

/**
 * 
 * @author Nick Reddel
 */
public class JobTrackerImpl implements Serializable, Cloneable, JobTracker {
	static final transient long serialVersionUID = -3L;

	private Date startTime;

	private Date endTime;

	private String progressMessage = "...pending";

	private String jobName;

	private String jobResult;

	private String id;

	private String jobLauncher;

	private boolean complete;

	private JobResultType jobResultType;

	private List<JobTracker> children = new ArrayList<JobTracker>();

	private JobTracker parent;

	private double percentComplete;

	private transient Exception jobException;

	private boolean cancelled;

	private long itemCount;
	
	private transient Object jobResultObject;

	@Override
	public long getItemCount() {
		return this.itemCount;
	}

	@Override
	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}

	private long itemsCompleted;

	@GwtTransient
	private String log = "";

	private transient Object logger;

	@GwtTransient
	private LogLevel logLevel = LogLevel.DEBUG;

	public JobTrackerImpl() {
	}

	public JobTrackerImpl(String id) {
		this.id = id;
	}

	@Override
	public List<JobTracker> getChildren() {
		return this.children;
	}

	@Override
	public Date getEndTime() {
		return endTime;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public double getJobDuration() {
		if (startTime == null || endTime == null) {
			return 0;
		}
		return (double) getEndTime().getTime() - getStartTime().getTime();
	}

	@Override
	public Exception getjobError() {
		return this.jobException;
	}

	@Override
	public String getJobLauncher() {
		return this.jobLauncher;
	}

	@Override
	public String getJobName() {
		return this.jobName;
	}

	@Override
	public String getJobResult() {
		return this.jobResult;
	}

	@Override
	public JobResultType getJobResultType() {
		return this.jobResultType;
	}

	@Override
	public JobTracker getParent() {
		return this.parent;
	}

	@Override
	public String getProgressMessage() {
		return this.progressMessage;
	}

	@Override
	public Date getStartTime() {
		return startTime;
	}

	@Override
	public boolean isCancelled() {
		return provideIsRoot() ? cancelled : root().isCancelled();
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}

	@Override
	public boolean provideIsRoot() {
		return parent == null;
	}

	@Override
	public JobTracker root() {
		if (getParent() == null) {
			return this;
		}
		return getParent().root();
	}

	@Override
	public void setCancelled(boolean cancelled) {
		if (provideIsRoot()) {
			this.cancelled = cancelled;
		}
		root().setCancelled(cancelled);
	}

	@Override
	public void setChildren(List<JobTracker> children) {
		this.children = children;
	}

	@Override
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	@Override
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setjobError(Exception jobException) {
		this.jobException = jobException;
	}

	@Override
	public void setJobLauncher(String jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	@Override
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	@Override
	public void setJobResult(String jobResult) {
		this.jobResult = jobResult;
	}

	@Override
	public void setJobResultType(JobResultType jobResultType) {
		this.jobResultType = jobResultType;
	}

	@Override
	public void setParent(JobTracker parent) {
		this.parent = parent;
	}

	@Override
	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	@Override
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("JobTracker: %s %s %s", getJobName(),
				getJobResult(), getId());
	}

	@Override
	public String getLog() {
		return this.log;
	}

	@Override
	public void setLog(String log) {
		this.log = log;
	}

	@Override
	public Object getLogger() {
		return this.logger;
	}

	@Override
	public void setLogger(Object logger) {
		this.logger = logger;
	}

	@Override
	public LogLevel getLogLevel() {
		return this.logLevel;
	}

	@Override
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public long getItemsCompleted() {
		return this.itemsCompleted;
	}

	@Override
	public void setItemsCompleted(long itemsCompleted) {
		this.itemsCompleted = itemsCompleted;
		updatePercent();
	}

	protected void updatePercent() {
		percentComplete = (getItemCount() == 0 ? 0.0
				: (getItemsCompleted() * 100.0) / getItemCount());
	}

	@Override
	public double getPercentComplete() {
		return getJobResultType() != null ? 1.0 : percentComplete;
	}

	@Override
	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}

	@Override
	public void updateJob(int completedDelta) {
		itemsCompleted += completedDelta;
		updatePercent();
		double progress = ((double) itemsCompleted) / ((double) itemCount);
	}

	public void startup(Class jobClass, String jobName, String message) {
		setComplete(false);
		setJobName(jobName == null ? CommonUtils.simpleClassName(jobClass)
				: jobName);
		setPercentComplete(0);
		setProgressMessage(message != null ? message : "Starting job...");
	}

	@Override
	public Object getJobResultObject() {
		return this.jobResultObject;
	}

	@Override
	public void setJobResultObject(Object jobResultObject) {
		this.jobResultObject = jobResultObject;
	}
}

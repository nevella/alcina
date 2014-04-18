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
public class JobTracker implements Serializable, Cloneable {
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

	public long getItemCount() {
		return this.itemCount;
	}

	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}

	private long itemsCompleted;

	@GwtTransient
	private String log = "";

	private transient Object logger;

	@GwtTransient
	private LogLevel logLevel = LogLevel.DEBUG;

	public JobTracker() {
	}

	public JobTracker(String id) {
		this.id = id;
	}

	public List<JobTracker> getChildren() {
		return this.children;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getId() {
		return this.id;
	}

	public double getJobDuration() {
		if (startTime == null || endTime == null) {
			return 0;
		}
		return (double) getEndTime().getTime() - getStartTime().getTime();
	}

	public Exception getjobError() {
		return this.jobException;
	}

	public String getJobLauncher() {
		return this.jobLauncher;
	}

	public String getJobName() {
		return this.jobName;
	}

	public String getJobResult() {
		return this.jobResult;
	}

	public JobResultType getJobResultType() {
		return this.jobResultType;
	}

	public JobTracker getParent() {
		return this.parent;
	}

	public String getProgressMessage() {
		return this.progressMessage;
	}

	public Date getStartTime() {
		return startTime;
	}

	public boolean isCancelled() {
		return root().cancelled;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public boolean provideIsRoot() {
		return parent == null;
	}

	public JobTracker root() {
		if (parent == null) {
			return this;
		}
		return parent.root();
	}

	public void setCancelled(boolean cancelled) {
		root().cancelled = cancelled;
	}

	public void setChildren(List<JobTracker> children) {
		this.children = children;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setjobError(Exception jobException) {
		this.jobException = jobException;
	}

	public void setJobLauncher(String jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setJobResult(String jobResult) {
		this.jobResult = jobResult;
	}

	public void setJobResultType(JobResultType jobResultType) {
		this.jobResultType = jobResultType;
	}

	public void setParent(JobTracker parent) {
		this.parent = parent;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("JobTracker: %s %s %s", getJobName(),
				getJobResult(), getId());
	}

	public String getLog() {
		return this.log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public Object getLogger() {
		return this.logger;
	}

	public void setLogger(Object logger) {
		this.logger = logger;
	}

	public LogLevel getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public long getItemsCompleted() {
		return this.itemsCompleted;
	}

	public void setItemsCompleted(long itemsCompleted) {
		this.itemsCompleted = itemsCompleted;
		updatePercent();
	}

	protected void updatePercent() {
		percentComplete = (getItemCount() == 0 ? 0.0
				: (getItemsCompleted() * 100.0) / getItemCount());
	}

	public double getPercentComplete() {
		return getJobResultType() != null ? 1.0 : percentComplete;
	}

	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}

	public void updateJob(int completedDelta) {
		itemsCompleted += completedDelta;
		updatePercent();
		double progress = ((double) itemsCompleted) / ((double) itemCount);
	}
}

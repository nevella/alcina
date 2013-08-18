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
import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class JobInfo implements Serializable, Cloneable {
	private long threadId;

	private Date startTime;

	private Date endTime;

	private String progressMessage = "...pending";

	private String jobName;

	private String jobResult;

	private double percentComplete;

	private boolean complete;

	private String errorMessage;
	
	private boolean completeInThread;

	public JobInfo combineWithChild(JobInfo kid) {
		JobInfo combo = gClone();
		combo.setProgressMessage(CommonUtils.formatJ("%s >> %s: %s",
				progressMessage, kid.getJobName(), kid.progressMessage));
		return combo;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public double getJobDuration() {
		if (startTime == null || endTime == null) {
			return 0;
		}
		return (double) getEndTime().getTime() - getStartTime().getTime();
	}

	public String getJobName() {
		return this.jobName;
	}

	public String getJobResult() {
		return this.jobResult;
	}

	public double getPercentComplete() {
		return this.percentComplete;
	}

	public String getProgressMessage() {
		return this.progressMessage;
	}

	public Date getStartTime() {
		return startTime;
	}

	public long getThreadId() {
		return this.threadId;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setJobResult(String jobResult) {
		this.jobResult = jobResult;
	}

	public void setPercentComplete(double pctComplete) {
		this.percentComplete = pctComplete;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	// literal for gwt
	protected JobInfo gClone() {
		JobInfo clone = new JobInfo();
		clone.complete = complete;
		clone.endTime = endTime;
		clone.errorMessage = errorMessage;
		clone.jobName = jobName;
		clone.percentComplete = percentComplete;
		clone.progressMessage = progressMessage;
		clone.startTime = startTime;
		clone.threadId = threadId;
		return clone;
	}

	public boolean isCompleteInThread() {
		return this.completeInThread;
	}

	public void setCompleteInThread(boolean completeInThread) {
		this.completeInThread = completeInThread;
	}
}

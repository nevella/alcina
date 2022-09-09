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

import java.util.Date;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Status.StatusReason;

/**
 *
 * @author Nick Reddel
 */
public class JobTrackerImpl extends Model
		implements JobTracker, TreeSerializable {
	private boolean cancelled;

	private boolean complete;

	private Date endTime;

	private String id;

	private String jobName;

	private String jobResult;

	private JobResultType jobResultType;

	private double percentComplete;

	private String progressMessage;

	private Date startTime;

	@GwtTransient
	private String log = "";

	private String serializedResult;

	public JobTrackerImpl() {
	}

	public JobTrackerImpl(String id) {
		this.id = id;
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
	@AlcinaTransient
	public String getLog() {
		return this.log;
	}

	@Override
	public double getPercentComplete() {
		return getJobResultType() != null ? 1.0 : percentComplete;
	}

	@Override
	public String getProgressMessage() {
		return this.progressMessage;
	}

	@Override
	public String getSerializedResult() {
		return this.serializedResult;
	}

	@Override
	public Date getStartTime() {
		return startTime;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		boolean old_cancelled = this.cancelled;
		this.cancelled = cancelled;
		propertyChangeSupport().firePropertyChange("cancelled", old_cancelled,
				cancelled);
	}

	@Override
	public void setComplete(boolean complete) {
		boolean old_complete = this.complete;
		this.complete = complete;
		propertyChangeSupport().firePropertyChange("complete", old_complete,
				complete);
	}

	@Override
	public void setEndTime(Date endTime) {
		Date old_endTime = this.endTime;
		this.endTime = endTime;
		propertyChangeSupport().firePropertyChange("endTime", old_endTime,
				endTime);
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	@Override
	public void setJobResult(String jobResult) {
		String old_jobResult = this.jobResult;
		this.jobResult = jobResult;
		propertyChangeSupport().firePropertyChange("jobResult", old_jobResult,
				jobResult);
	}

	@Override
	public void setJobResultType(JobResultType jobResultType) {
		JobResultType old_jobResultType = this.jobResultType;
		this.jobResultType = jobResultType;
		propertyChangeSupport().firePropertyChange("jobResultType",
				old_jobResultType, jobResultType);
	}

	@Override
	public void setLog(String log) {
		String old_log = this.log;
		this.log = log;
		propertyChangeSupport().firePropertyChange("log", old_log, log);
	}

	@Override
	public void setPercentComplete(double percentComplete) {
		double old_percentComplete = this.percentComplete;
		this.percentComplete = percentComplete;
		propertyChangeSupport().firePropertyChange("percentComplete",
				old_percentComplete, percentComplete);
	}

	@Override
	public void setProgressMessage(String progressMessage) {
		String old_progressMessage = this.progressMessage;
		this.progressMessage = progressMessage;
		propertyChangeSupport().firePropertyChange("progressMessage",
				old_progressMessage, progressMessage);
	}

	public void setSerializedResult(String serializedResult) {
		this.serializedResult = serializedResult;
	}

	@Override
	public void setStartTime(Date startTime) {
		Date old_startTime = this.startTime;
		this.startTime = startTime;
		propertyChangeSupport().firePropertyChange("startTime", old_startTime,
				startTime);
	}

	@Override
	public String toString() {
		return Ax.format("JobTracker: %s\n%s %s", getId(), getJobName(),
				CommonUtils.nullToEmpty(getJobResult()));
	}

	public StatusReason asResultStatusReason() {
		switch (jobResultType) {
		case DID_NOT_COMPLETE:
		case EXCEPTION:
		case FAIL:
			return StatusReason.error(log, jobResult);
		case WARN:
			return StatusReason.warn(log, jobResult);
		case OK:
			return StatusReason.ok(jobResult);
		default:
			throw new UnsupportedOperationException();
		}
	}
}

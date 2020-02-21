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

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.LogLevel;

/**
 *
 * @author Nick Reddel
 */
public class JobTrackerImpl extends BaseSourcesPropertyChangeEvents
        implements Serializable, Cloneable, JobTracker {
    static final transient long serialVersionUID = -3L;

    private Date startTime;

    private Date endTime;

    private String progressMessage = "...pending";

    private String subProgressMessage = "";

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
    public void childComplete(JobTracker tracker) {
        if (parent != null) {
            parent.childComplete(tracker);
        }
    }

    @Override
    public JobTracker exportableForm() {
        return this;
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
    public long getItemCount() {
        return this.itemCount;
    }

    @Override
    public long getItemsCompleted() {
        return this.itemsCompleted;
    }

    @Override
    public double getJobDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (double) getEndTime().getTime() - getStartTime().getTime();
    }

    @Override
    public Exception getJobException() {
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
    public Object getJobResultObject() {
        return this.jobResultObject;
    }

    @Override
    public JobResultType getJobResultType() {
        return this.jobResultType;
    }

    @Override
    public String getLog() {
        return this.log;
    }

    @Override
    public Object getLogger() {
        return this.logger;
    }

    @Override
    public LogLevel getLogLevel() {
        return this.logLevel;
    }

    @Override
    public JobTracker getParent() {
        return this.parent;
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
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public String getSubProgressMessage() {
        return this.subProgressMessage;
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
            boolean old_cancelled = this.cancelled;
            this.cancelled = cancelled;
            propertyChangeSupport().firePropertyChange("cancelled",
                    old_cancelled, cancelled);
        } else {
            root().setCancelled(cancelled);
        }
    }

    @Override
    public void setChildren(List<JobTracker> children) {
        this.children = children;
        // not bound
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
    public void setItemCount(long itemCount) {
        long old_itemCount = this.itemCount;
        this.itemCount = itemCount;
        propertyChangeSupport().firePropertyChange("itemCount", old_itemCount,
                itemCount);
    }

    @Override
    public void setItemsCompleted(long itemsCompleted) {
        long old_itemsCompleted = this.itemsCompleted;
        this.itemsCompleted = itemsCompleted;
        propertyChangeSupport().firePropertyChange("itemsCompleted",
                old_itemsCompleted, itemsCompleted);
        updatePercent();
    }

    @Override
    public void setJobException(Exception jobException) {
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
        String old_jobResult = this.jobResult;
        this.jobResult = jobResult;
        propertyChangeSupport().firePropertyChange("jobResult", old_jobResult,
                jobResult);
    }

    @Override
    public void setJobResultObject(Object jobResultObject) {
        this.jobResultObject = jobResultObject;
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
    public void setLogger(Object logger) {
        this.logger = logger;
    }

    @Override
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public void setParent(JobTracker parent) {
        // System.out.format("setpt: %s %s %s\n",
        // Thread.currentThread().getId(),
        // id, parent == null ? "null" : parent.getId());
        this.parent = parent;
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

    @Override
    public void setStartTime(Date startTime) {
        Date old_startTime = this.startTime;
        this.startTime = startTime;
        propertyChangeSupport().firePropertyChange("startTime", old_startTime,
                startTime);
    }

    @Override
    public void setSubProgressMessage(String subProgressMessage) {
        this.subProgressMessage = subProgressMessage;
    }

    @Override
    public String toString() {
        return Ax.format("JobTracker: %s\n%s %s", getId(),
                getJobName(), CommonUtils.nullToEmpty(getJobResult()));
    }

    @Override
    public void updateJob(int completedDelta) {
        itemsCompleted += completedDelta;
        updatePercent();
    }
}

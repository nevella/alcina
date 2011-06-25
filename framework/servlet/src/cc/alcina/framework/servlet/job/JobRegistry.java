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
package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;

/**
 * 
 * @author Nick Reddel
 */
public class JobRegistry {
	private Map<Long, JobInfo> infoMap;

	private Multimap<Long, List<JobInfo>> infoChildMap;

	private Map<Long, Boolean> cancelledMap;

	private JobRegistry() {
		super();
		infoMap = new HashMap<Long, JobInfo>();
		infoChildMap = new Multimap<Long, List<JobInfo>>();
		cancelledMap = new HashMap<Long, Boolean>();
	}

	private static JobRegistry theInstance;

	public synchronized static JobRegistry get() {
		if (theInstance == null) {
			theInstance = new JobRegistry();
		}
		return theInstance;
	}

	public void updateInfo(JobInfo info) {
		long id = Thread.currentThread().getId();
		info.setThreadId(id);
		if (info.isComplete()) {
			infoChildMap.getAndEnsure(id).remove(info);// if it's a child - keep
														// if top level
		}
	}

	public JobInfo getInfo(long id) {
		JobInfo jobInfo = infoMap.get(id);
		if (jobInfo != null) {
			JobInfo kid = CommonUtils.last(infoChildMap.getAndEnsure(id));
			if (kid != null) {
				return jobInfo.combineWithChild(kid);
			}
		}
		return jobInfo;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public JobInfo startJob(Class jobClass, String jobName, String message) {
		JobInfo info = new JobInfo();
		info.setComplete(false);
		info.setJobName(jobName == null ? jobClass.getSimpleName() : jobName);
		info.setPercentComplete(0);
		info.setProgressMessage(message != null ? message : "Starting job...");
		updateInfo(info);
		info.setStartTime(new Date());
		JobInfo tlInfo = getTopLevelInfoForThread();
		if (tlInfo == null || tlInfo.isComplete()) {
			cancelledMap.remove(info.getThreadId());
			infoMap.put(info.getThreadId(), info);
			infoChildMap.clear();
		} else {
			infoChildMap.add(info.getThreadId(), info);
		}
		return info;
	}

	public void jobProgress(JobInfo info, String progressMessage,
			double percentComplete) {
		if (info == null) {
			return;
		}
		info.setComplete(false);
		info.setPercentComplete(percentComplete);
		info.setProgressMessage(progressMessage);
		updateInfo(info);
	}

	public void jobComplete(JobInfo info) {
		jobComplete(info, "Job complete");
	}

	public void jobError(JobInfo info, String errorMessage) {
		if (info == null) {
			return;
		}
		jobComplete(info);
		info.setErrorMessage("Job failed: " + errorMessage);
		JobRegistry.get().updateInfo(info);
	}

	public boolean isCancelled() {
		long id = Thread.currentThread().getId();
		return CommonUtils.bv(cancelledMap.get(id)) || !infoMap.containsKey(id);
	}

	public void cancel(Long id) {
		cancelledMap.put(id, true);
		JobInfo info = getTopLevelInfoForThread();
		if (info != null && !info.isComplete()) {
			jobError(info, "Job cancelled");
		}
	}

	public List<Long> getRunningJobs() {
		Set<Entry<Long, JobInfo>> entries = infoMap.entrySet();
		List<Long> runningJobids = new ArrayList<Long>();
		for (Entry<Long, JobInfo> entry : entries) {
			if (!entry.getValue().isComplete()) {
				runningJobids.add(entry.getKey());
			}
		}
		return runningJobids;
	}

	public void jobComplete(JobInfo info, String message) {
		if (info == null) {
			return;
		}
		info.setComplete(true);
		info.setPercentComplete(1);
		info.setProgressMessage(message);
		info.setEndTime(new Date());
		updateInfo(info);
	}

	public void jobErrorInThread() {
		JobInfo info = getTopLevelInfoForThread();
		if (info != null && !info.isComplete()) {
			jobError(info, "Unknown error");
		}
	}

	private JobInfo getTopLevelInfoForThread() {
		return infoMap.get(Thread.currentThread().getId());
	}
}

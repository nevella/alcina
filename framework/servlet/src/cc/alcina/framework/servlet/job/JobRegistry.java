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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.HasJobInfo;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

/**
 * 
 * @author Nick Reddel
 */
public class JobRegistry {
	public static final String TOPIC_JOB_FAILURE = JobRegistry.class.getName()
			+ ".TOPIC_JOB_FAILURE";

	public static JobRegistry get() {
		JobRegistry singleton = Registry.checkSingleton(JobRegistry.class);
		if (singleton == null) {
			singleton = new JobRegistry();
			Registry.registerSingleton(JobRegistry.class, singleton);
		}
		return singleton;
	}

	public static void notifyJobFailure(JobInfo info) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_JOB_FAILURE, info);
	}

	public static void notifyJobFailureListenerDelta(
			TopicListener<JobInfo> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_JOB_FAILURE, listener,
				add);
	}

	private Map<Long, JobInfo> infoMap;

	private Map<JobInfo, Class> jobClassMap;

	private Multimap<Long, List<JobInfo>> infoChildMap;

	private Map<Long, Boolean> cancelledMap;

	boolean refuseJobs = false;

	private JobRegistry() {
		super();
		infoMap = new HashMap<Long, JobInfo>();
		infoChildMap = new Multimap<Long, List<JobInfo>>();
		cancelledMap = new HashMap<Long, Boolean>();
		jobClassMap = new WeakHashMap<JobInfo, Class>();
	}

	public void cancel(Long id) {
		cancelledMap.put(id, true);
		JobInfo info = getTopLevelInfoForThread();
		if (info != null && !info.isComplete()) {
			jobError(info, "Job cancelled");
		}
	}

	public List<JobInfo> cancelAll() {
		refuseJobs = true;
		for (Long tid : infoMap.keySet()) {
			if (!cancelledMap.containsKey(tid)) {
				cancel(tid);
			}
		}
		List<JobInfo> running = new ArrayList();
		for (JobInfo jobInfo : infoMap.values()) {
			if (!jobInfo.isCompleteInThread()) {
				running.add(jobInfo);
			}
		}
		return running;
	}

	public void checkCancelled(Logger logger) {
		if (isCancelled()) {
			logger.info("Action cancelled by user");
			throw new CancelledException("Action cancelled by user");
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

	public Set<Class> getRunningJobClasses() {
		Set<Class> result = new LinkedHashSet<Class>();
		List<Long> ids = getRunningJobs();
		for (Long id : ids) {
			result.add(jobClassMap.get(infoMap.get(id)));
		}
		return result;
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

	public boolean isCancelled() {
		long id = Thread.currentThread().getId();
		return CommonUtils.bv(cancelledMap.get(id)) || !infoMap.containsKey(id);
	}

	public boolean isTopLevel(JobInfo jobInfo) {
		long id = Thread.currentThread().getId();
		return jobInfo == infoMap.get(id);
	}

	public void jobComplete(JobInfo info) {
		jobComplete(info, "Job complete");
	}

	public void jobComplete(JobInfo info, String message) {
		if (info == null) {
			return;
		}
		info.setComplete(true);
		info.setPercentComplete(1);
		info.setProgressMessage(message);
		info.setEndTime(new Date());
		AlcinaTopics.jobComplete(info);
		updateInfo(info);
	}

	public void jobCompleteFromThread() {
		JobInfo info = getTopLevelInfoForThread();
		if (info != null) {
			info.setCompleteInThread(true);
		}
	}

	public void jobError(JobInfo info, Exception ex) {
		if (info == null) {
			return;
		}
		jobComplete(info);
		info.setErrorMessage("Job failed: " + ex.toString());
		JobRegistry.get().updateInfo(info);
		info.setJobException(ex);
		notifyJobFailure(info);
	}

	public void jobError(JobInfo info, String message) {
		jobError(info, new RuntimeException(message));
	}

	public void jobErrorInThread() {
		JobInfo info = getTopLevelInfoForThread();
		if (info != null && !info.isComplete()) {
			jobError(info, "Unknown error");
		}
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

	public ActionLogItem performChildJob(RemoteActionPerformer performer,
			RemoteAction action, boolean throwChildExceptions) throws Exception {
		ActionLogItem log = performer.performAction(action);
		if (performer instanceof HasJobInfo) {
			Exception ex = ((HasJobInfo) performer).getJobInfo()
					.getJobException();
			if (ex != null && throwChildExceptions) {
				throw ex;
			}
		}
		return log;
	}

	public JobInfo startJob(Class jobClass, String jobName, String message) {
		if (refuseJobs) {
			throw new RuntimeException("refusing jobs");
		}
		JobInfo info = new JobInfo();
		info.setComplete(false);
		info.setJobName(jobName == null ? jobClass.getSimpleName() : jobName);
		jobClassMap.put(info, jobClass);
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

	public void updateInfo(JobInfo info) {
		long id = Thread.currentThread().getId();
		info.setThreadId(id);
		if (info.isComplete()) {
			infoChildMap.getAndEnsure(id).remove(info);// if it's a child - keep
														// if top level
		}
	}

	private JobInfo getTopLevelInfoForThread() {
		return infoMap.get(Thread.currentThread().getId());
	}
}

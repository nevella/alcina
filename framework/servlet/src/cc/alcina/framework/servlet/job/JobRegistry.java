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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.servlet.RemoteActionLogger;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.ServletLayerUtils;

/**
 * 
 * @author Nick Reddel
 */
public class JobRegistry {
	public static final String TOPIC_JOB_FAILURE = JobRegistry.class.getName()
			+ ".TOPIC_JOB_FAILURE";

	public static final String TOPIC_JOB_STARTED = JobRegistry.class.getName()
			+ ".TOPIC_JOB_STARTED";

	public static final String CONTEXT_TRACKER = JobRegistry.class.getName()
			+ ".CONTEXT_TRACKER";

	public static final String CONTEXT_NEXT_JOB_ID = JobRegistry.class
			.getName() + ".CONTEXT_NEXT_JOB_ID";

	public static final String CONTEXT_NON_PERSISTENT = JobRegistry.class
			.getName() + ".CONTEXT_NON_PERSISTENT";

	public static final String CONTEXT_USE_LOGGER = JobRegistry.class.getName()
			+ ".CONTEXT_USE_LOGGER";

	public static JobRegistry get() {
		JobRegistry singleton = Registry.checkSingleton(JobRegistry.class);
		if (singleton == null) {
			singleton = new JobRegistry();
			Registry.registerSingleton(JobRegistry.class, singleton);
		}
		return singleton;
	}

	public static void notifyJobFailure(JobTracker tracker) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_JOB_FAILURE, tracker);
	}

	public static void notifyJobFailureListenerDelta(
			TopicListener<JobTracker> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_JOB_FAILURE, listener,
				add);
	}

	private Map<String, JobTracker> trackerMap = new LinkedHashMap<String, JobTracker>();

	boolean refuseJobs = false;

	private JobRegistry() {
	}

	public void cancel(String id) {
		JobTracker tracker = trackerMap.get(id);
		if (tracker != null && !tracker.isCancelled()) {
			tracker.setCancelled(true);
			jobError(tracker, new RuntimeException("Job cancelled"), false);
		}
	}

	public List<JobTracker> cancelAll() {
		refuseJobs = true;
		List<JobTracker> running = new ArrayList();
		for (JobTracker root : getRootTrackers()) {
			if (!root.isCancelled()) {
				root.setCancelled(true);
			}
			if (!root.isComplete()) {
				running.add(root);
			}
		}
		return running;
	}

	private List<JobTracker> getRootTrackers() {
		CollectionFilter<JobTracker> filter = new CollectionFilter<JobTracker>() {
			@Override
			public boolean allow(JobTracker o) {
				return o.provideIsRoot();
			}
		};
		return CollectionFilters.filter(trackerMap.values(), filter);
	}

	public void checkCancelled() {
		JobTracker jobTracker = getContextTracker();
		if (jobTracker == null) {
			System.out.println("warn - checking null trracker");
		} else {
			if (jobTracker.isCancelled()) {
				getContextLogger().info("Action cancelled by user");
				throw new CancelledException("Action cancelled by user");
			}
		}
	}

	public JobTracker getInfo(String id) {
		return trackerMap.get(id);
	}

	public List<String> getRunningJobs() {
		Set<Entry<String, JobTracker>> entries = trackerMap.entrySet();
		List<String> runningJobIds = new ArrayList<String>();
		for (Entry<String, JobTracker> entry : entries) {
			if (!entry.getValue().isComplete()) {
				runningJobIds.add(entry.getKey());
			}
		}
		return runningJobIds;
	}

	public void jobOk(String message) {
		jobComplete(JobResultType.OK, message);
	}

	private void jobComplete(JobResultType resultType, String message) {
		final JobTracker tracker = getContextTracker();
		tracker.setComplete(true);
		tracker.setProgressMessage(message);
		tracker.setEndTime(new Date());
		AlcinaTopics.jobComplete(tracker);
		logComplete(tracker, message);
		if (tracker.provideIsRoot()) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(10000);
						trackerMap.remove(tracker.getId());
					} catch (InterruptedException e) {
					}
				};
			}.start();
		} else {
			trackerMap.remove(tracker.getId());
		}
		popContextTracker(tracker);
	}

	private void logComplete(JobTracker tracker, String message) {
		getContextLogger().info(message);
		long itemCount = tracker.getItemCount();
		if (itemCount != 0) {
			double avgTime = tracker.getJobDuration() / itemCount;
			getContextLogger().info(
					String.format(
							"Run time: %.4f s. - avg. time per item: %s ms.",
							tracker.getJobDuration() / 1000, avgTime));
		} else {
			getContextLogger().info(
					String.format("Run time: %.4f s.",
							tracker.getJobDuration() / 1000));
		}
	}

	private void popContextTracker(JobTracker tracker) {
		JobTracker current = getContextTracker();
		if (current != tracker) {
			System.out.format(
					"warn -- popping wrong tracker %s, thread-current %s\n",
					tracker, current);
		} else {
			LooseContext.set(CONTEXT_TRACKER, tracker.getParent());
		}
	}

	public void jobError(Exception ex) {
		JobTracker tracker = getContextTracker();
		jobError(tracker, ex, true);
	}

	private void jobError(JobTracker tracker, Exception ex, boolean logException) {
		if (logException) {
			getContextLogger().warn("", ex);
			ex.printStackTrace();
		}
		String jobResult = "Job failed: " + ex.toString();
		tracker.setjobError(ex);
		jobComplete(JobResultType.FAIL, jobResult);
		notifyJobFailure(tracker);
	}

	public void jobError(String message) {
		jobError(new RuntimeException(message));
	}

	public void jobProgress(String progressMessage, double percentComplete) {
		JobTracker tracker = getContextTracker();
		tracker.setComplete(false);
		tracker.setPercentComplete(percentComplete);
		tracker.setProgressMessage(progressMessage);
	}

	public JobTracker startJob(Class jobClass, String jobName, String message) {
		if (refuseJobs) {
			throw new RuntimeException("refusing jobs");
		}
		JobId jobId = null;
		JobId contextId = LooseContext.get(CONTEXT_NEXT_JOB_ID);
		if (contextId == null) {
			jobId = new JobId(jobClass, ServletLayerUtils.getLocalHostName());
		} else {
			jobId = contextId;
			LooseContext.remove(CONTEXT_NEXT_JOB_ID);
		}
		JobTracker tracker = new JobTracker(jobId.toString());
		tracker.setComplete(false);
		tracker.setJobName(jobName == null ? jobClass.getSimpleName() : jobName);
		tracker.setPercentComplete(0);
		tracker.setProgressMessage(message != null ? message
				: "Starting job...");
		trackerMap.put(tracker.getId(), tracker);
		tracker.setStartTime(new Date());
		Logger custom = LooseContext.get(CONTEXT_USE_LOGGER);
		tracker.setLogger(custom != null ? custom : Registry.impl(
				RemoteActionLoggerProvider.class).createLogger(jobClass));
		pushContextTracker(tracker);
		LooseContext.getContext().publishTopic(TOPIC_JOB_STARTED, tracker);
		return tracker;
	}

	private void pushContextTracker(JobTracker tracker) {
		JobTracker current = getContextTracker();
		if (current != null) {
			tracker.setParent(current);
			current.getChildren().add(tracker);
		}
		LooseContext.set(CONTEXT_TRACKER, tracker);
	}

	public JobTracker getContextTracker() {
		return LooseContext.get(CONTEXT_TRACKER);
	}

	public Logger getContextLogger() {
		JobTracker tracker = getContextTracker();
		return (Logger) tracker.getLogger();
	}

	public String getContextLogBuffer() {
		Logger contextLogger = getContextLogger();
		if (contextLogger instanceof RemoteActionLogger) {
			return ((RemoteActionLogger) contextLogger).closeLogger();
		}
		return null;
	}

	public RemoteActionLogger getAccessLogger() {
		Logger contextLogger = getContextLogger();
		if (contextLogger instanceof RemoteActionLogger) {
			return (RemoteActionLogger) contextLogger;
		}
		return null;
	}

	public void updateJob(String message) {
		updateJob(message, getContextTracker().provideIsRoot() ? 1 : 0);
	}

	public void updateJob(String message, int completedDelta) {
		JobTracker contextTracker = getContextTracker();
		contextTracker.updateJob(completedDelta);
		long itemsCompleted = contextTracker.getItemsCompleted();
		long itemCount = contextTracker.getItemCount();
		double progress = ((double) itemsCompleted) / ((double) itemCount);
		jobProgress(String.format("(%s/%s) -  %s", itemsCompleted, itemCount,
				message), progress);
	}
}

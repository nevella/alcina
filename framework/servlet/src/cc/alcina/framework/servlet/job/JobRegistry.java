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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.JobTrackerImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.RemoteActionLogger;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;

import com.totsp.gwittir.client.beans.Converter;

/**
 *
 * @author Nick Reddel
 */
public class JobRegistry implements RegistrableService {
	public static JobTracker exportableForm(JobTracker in) {
		Converter<JobTracker, JobTracker> converter = new Converter<JobTracker, JobTracker>() {
			IdentityHashMap<JobTracker, JobTracker> seen = new IdentityHashMap<JobTracker, JobTracker>();

			@Override
			public JobTracker convert(JobTracker o) {
				if (o == null) {
					return null;
				}
				if (seen.containsKey(o)) {
					return seen.get(o);
				}
				JobTracker result = o.exportableForm();
				seen.put(o, result);
				result.setParent(convert(result.getParent()));
				result.setChildren(CollectionFilters.convert(
						result.getChildren(), this));
				return result;
			}
		};
		return converter.convert(in);
	}

	public static JobRegistry get() {
		JobRegistry singleton = Registry.checkSingleton(JobRegistry.class);
		if (singleton == null) {
			singleton = new JobRegistry();
			Registry.registerSingleton(JobRegistry.class, singleton);
		}
		return singleton;
	}

	public static String getLauncherName() {
		String launcherName = ResourceUtilities.getBundledString(
				JobRegistry.class, "launcherName");
		launcherName = launcherName.isEmpty() ? EntityLayerUtils
				.getLocalHostName() : launcherName;
		return launcherName;
	}

	public static void notifyJobFailure(JobTracker tracker) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_JOB_FAILURE, tracker);
	}

	public static void notifyJobFailureListenerDelta(
			TopicListener<JobTracker> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_JOB_FAILURE, listener,
				add);
	}

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

	public static final String CONTEXT_PERFORMING_CLUSTERED_JOB = JobRegistry.class
			.getName() + ".CONTEXT_PERFORMING_CLUSTERED_JOB";

	public static final String NO_LOG = "no-log";

	private Timer jobReaperTimer = new Timer();

	private Map<String, JobTracker> trackerMap = new ConcurrentHashMap<String, JobTracker>();

	private Map<String, Long> trackerTimeout = new ConcurrentHashMap<String, Long>();

	boolean refuseJobs = false;

	// hack til the top cluster tracker is solved
	private TimerTask jobReaperTask = new TimerTask() {
		@Override
		public void run() {
			long cutoff = System.currentTimeMillis()
					- TimeConstants.ONE_HOUR_MS * 2;
			for (Iterator<Entry<String, Long>> itr = trackerTimeout.entrySet()
					.iterator(); itr.hasNext();) {
				Entry<String, Long> entry = itr.next();
				if (entry.getValue() < cutoff) {
					itr.remove();
					trackerMap.remove(entry.getKey());
					System.out.format(
							"jobReaperTask - removed job with id %s\n",
							entry.getKey());
				}
			}
		}
	};

	private JobRegistry() {
		jobReaperTimer = new Timer();
		jobReaperTimer.scheduleAtFixedRate(jobReaperTask, 0,
				15 * TimeConstants.ONE_MINUTE_MS);
	}

	@Override
	public void appShutdown() {
		jobReaperTimer.cancel();
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

	public void flushContextLogger() {
		flushTracker(getContextTracker());
	}

	public RemoteActionLogger getAccessLogger() {
		Logger contextLogger = getContextLogger();
		if (contextLogger instanceof RemoteActionLogger) {
			return (RemoteActionLogger) contextLogger;
		}
		return null;
	}

	public String getContextLogBuffer(JobTracker tracker) {
		if (tracker == null) {
			tracker = getContextTracker();
		}
		Logger contextLogger = (Logger) tracker.getLogger();
		return flushLogger(contextLogger);
	}

	public Logger getContextLogger() {
		JobTracker tracker = getContextTracker();
		return tracker == null ? null : (Logger) tracker.getLogger();
	}

	public JobTracker getContextTracker() {
		JobTracker tracker = LooseContext.get(CONTEXT_TRACKER);
		if (tracker != null) {
			JobTracker fromMap = trackerMap.get(tracker.getId());
			if (fromMap != null) {
				return fromMap;
			}
		}
		return tracker;
	}

	public JobId getNextJobId(Class jobClass) {
		String launcherName = getLauncherName();
		return new JobId(jobClass, launcherName);
	}

	public List<String> getRunningJobs() {
		Set<Entry<String, JobTracker>> entries = trackerMap.entrySet();
		List<String> runningJobIds = new ArrayList<String>();
		for (Entry<String, JobTracker> entry : entries) {
			if (!entry.getValue().isComplete()
					&& entry.getValue().getParent() == null) {
				runningJobIds.add(entry.getKey());
			}
		}
		return runningJobIds;
	}

	public JobTracker getTracker(String jobId) {
		return trackerMap.get(jobId);
	}

	public void jobError(Exception ex) {
		JobTracker tracker = getContextTracker();
		jobError(tracker, ex, true);
	}

	public void jobError(String message) {
		jobError(new RuntimeException(message));
	}

	public void jobOk(String message) {
		jobComplete(getContextTracker(), JobResultType.OK, message);
	}

	public void jobProgress(String progressMessage, double percentComplete) {
		JobTracker tracker = getContextTracker();
		tracker.setComplete(false);
		tracker.setPercentComplete(percentComplete);
		tracker.setProgressMessage(progressMessage);
	}

	public void putTracker(JobTracker jobTracker) {
		trackerMap.put(jobTracker.getId(), jobTracker);
		trackerTimeout.put(jobTracker.getId(), System.currentTimeMillis()
				+ TimeConstants.ONE_HOUR_MS);
	}

	public void removeTracker(final JobTracker tracker) {
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
	}

	public JobTracker startJob(Class jobClass, String jobName, String message) {
		if (refuseJobs) {
			throw new RuntimeException("refusing jobs");
		}
		JobId jobId = null;
		JobId contextId = LooseContext.get(CONTEXT_NEXT_JOB_ID);
		if (contextId == null) {
			jobId = getNextJobId(jobClass);
		} else {
			jobId = contextId;
			LooseContext.remove(CONTEXT_NEXT_JOB_ID);
		}
		JobTracker tracker = null;
		if (trackerMap.containsKey(jobId.toString())) {
			tracker = trackerMap.get(jobId.toString());
		} else {
			tracker = new JobTrackerImpl(jobId.toString());
			((JobTrackerImpl) tracker).startup(jobClass, jobName, message);
			putTracker(tracker);
		}
		if (tracker.getLogger() == null) {
			Logger custom = LooseContext.get(CONTEXT_USE_LOGGER);
			tracker.setLogger(custom != null ? custom : Registry.impl(
					RemoteActionLoggerProvider.class).createLogger(jobClass));
		}
		pushContextTracker(tracker);
		LooseContext.getContext().publishTopic(TOPIC_JOB_STARTED, tracker);
		return tracker;
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

	private void flushTracker(JobTracker tracker) {
		tracker.setLog(tracker.getLog() + getContextLogBuffer(tracker));
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

	private void jobComplete(JobTracker tracker, JobResultType resultType,
			String message) {
		tracker.setComplete(true);
		tracker.setProgressMessage(message);
		tracker.setEndTime(new Date());
		AlcinaTopics.jobComplete(tracker);
		logComplete(tracker, message);
		removeTracker(tracker);
		if (tracker.getParent() != null) {
			tracker.getParent().childComplete(tracker);
		}
		popContextTracker(tracker);
	}

	private void jobError(JobTracker tracker, Exception ex, boolean logException) {
		if (logException) {
			getContextLogger().warn("", ex);
			ex.printStackTrace();
		}
		String jobResult = "Job failed: " + ex.toString();
		tracker.setJobException(ex);
		jobComplete(tracker, JobResultType.FAIL, jobResult);
		notifyJobFailure(tracker);
	}

	private void logComplete(JobTracker tracker, String message) {
		if (message == NO_LOG) {
			tracker.setJobResult("");
		} else {
			tracker.setJobResult(message);
			Logger logger = (Logger) tracker.getLogger();
			logger.info(message);
			long itemCount = tracker.getItemCount();
			if (itemCount != 0 && tracker.getParent() == null) {
				double avgTime = tracker.getJobDuration() / itemCount;
				logger.info(String.format(
						"Run time: %.4f s. - avg. time per item: %.0f ms.",
						tracker.getJobDuration() / 1000, avgTime));
			} else {
				logger.info(String.format("Run time: %.4f s.",
						tracker.getJobDuration() / 1000));
			}
		}
		flushTracker(tracker);
	}

	private void popContextTracker(JobTracker tracker) {
		JobTracker current = getContextTracker();
		if (current != tracker) {
			System.out.format(
					"warn -- popping wrong tracker %s, thread-current %s\n",
					tracker, current);
		} else {
			JobTracker parent = tracker.getParent();
			if (parent != null) {
				List<JobTracker> newList = new ArrayList<JobTracker>(
						parent.getChildren());
				newList.remove(tracker);
				parent.setChildren(newList);
			}
			LooseContext.set(CONTEXT_TRACKER, parent);
		}
	}

	private void pushContextTracker(JobTracker tracker) {
		JobTracker current = getContextTracker();
		if (current != null) {
			tracker.setParent(current);
			List<JobTracker> newList = new ArrayList<JobTracker>(
					current.getChildren());
			current.getChildren().add(tracker);
			current.setChildren(newList);
		}
		LooseContext.set(CONTEXT_TRACKER, tracker);
	}

	protected String flushLogger(Logger contextLogger) {
		if (contextLogger instanceof RemoteActionLogger) {
			return ((RemoteActionLogger) contextLogger).flushLogger();
		}
		return null;
	}
}

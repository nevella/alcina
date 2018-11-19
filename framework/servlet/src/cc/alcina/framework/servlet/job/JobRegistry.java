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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.JobTrackerImpl;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.RemoteActionLogger;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.Sx;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocations(value = {
		@RegistryLocation(registryPoint = JobRegistry.class, implementationType = ImplementationType.SINGLETON),
		@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class) })
public class JobRegistry implements RegistrableService {
	static JobRegistry singleton;

	public static final String TOPIC_JOB_FAILURE = JobRegistry.class.getName()
			+ ".TOPIC_JOB_FAILURE";

	public static final String TOPIC_JOB_STARTED = JobRegistry.class.getName()
			+ ".TOPIC_JOB_STARTED";

	public static final String CONTEXT_TRACKER = JobRegistry.class.getName()
			+ ".CONTEXT_TRACKER";

	public static final String CONTEXT_NEXT_JOB_ID = JobRegistry.class.getName()
			+ ".CONTEXT_NEXT_JOB_ID";

	public static final String CONTEXT_NON_PERSISTENT = JobRegistry.class
			.getName() + ".CONTEXT_NON_PERSISTENT";

	public static final String CONTEXT_USE_LOGGER = JobRegistry.class.getName()
			+ ".CONTEXT_USE_LOGGER";

	public static final String CONTEXT_PERFORMING_CLUSTERED_JOB = JobRegistry.class
			.getName() + ".CONTEXT_PERFORMING_CLUSTERED_JOB";

	public static final String CONTEXT_REUSE_CURRENT_TRACKER = JobRegistry.class
			.getName() + ".CONTEXT_REUSE_CURRENT_TRACKER";

	public static final String NO_LOG = "no-log";

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
				result.setChildren(
						CollectionFilters.convert(result.getChildren(), this));
				return result;
			}
		};
		return converter.convert(in);
	}

	public static JobRegistry get() {
		if (singleton == null) {
			// not thread-safe, make sure it's initialised single-threaded on
			// app startup
			singleton = Registry.impl(JobRegistry.class);
		}
		return singleton;
	}

	public static String getLauncherName() {
		String launcherName = ResourceUtilities
				.getBundledString(JobRegistry.class, "launcherName");
		launcherName = launcherName.isEmpty()
				? EntityLayerUtils.getLocalHostName() : launcherName;
		if (Sx.isTest()) {
			launcherName += "-devconsole";
		}
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

	private Map<String, JobTracker> trackerMap = new ConcurrentHashMap<String, JobTracker>();

	boolean refuseJobs = false;

	public JobRegistry() {
	}

	@Override
	public void appShutdown() {
	}

	public void cancel(String id) {
		JobTracker tracker = trackerMap.get(id);
		if (tracker != null && !tracker.isCancelled()) {
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
		if(tracker == null){
			return "<null log buffer>";
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

	public boolean hasTracker() {
		return getContextTracker() != null;
	}

	public boolean isCancelled() {
		JobTracker jobTracker = getContextTracker();
		if (jobTracker == null) {
			return true;
		} else {
			return jobTracker.isCancelled();
		}
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

	public void log(String message, Object... params) {
		Logger logger = getContextLogger();
		if (params.length > 0) {
			message = String.format(message, params);
		}
		if (logger == null) {
			System.out.println(message);
		} else {
			logger.info(message);
		}
	}

	public void putTracker(JobTracker tracker) {
		trackerMap.put(tracker.getId(), tracker);
	}

	public void putTrackerForNextJob(JobTracker tracker) {
		LooseContext.set(CONTEXT_NEXT_JOB_ID, new JobId(tracker.getId()));
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
		JobId contextId = null;
		if (LooseContext.is(CONTEXT_REUSE_CURRENT_TRACKER)) {
			contextId = new JobId(getContextTracker().getId());
		}
		if (contextId == null) {
			contextId = LooseContext.get(CONTEXT_NEXT_JOB_ID);
		}
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
			tracker = createJobTracker(jobId);
			tracker.startup(jobClass, jobName, message);
			putTracker(tracker);
		}
		if (tracker.getLogger() == null) {
			Logger custom = LooseContext.get(CONTEXT_USE_LOGGER);
			tracker.setLogger(custom != null ? custom
					: Registry.impl(RemoteActionLoggerProvider.class)
							.createLogger(jobClass));
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

	public void warn(Exception e) {
		Logger logger = getContextLogger();
		if (logger == null) {
			e.printStackTrace();
		} else {
			logger.warn(e);
		}
	}

	public void warn(String message) {
		Logger logger = getContextLogger();
		if (logger == null) {
			System.err.println(message);
		} else {
			logger.warn(message);
		}
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
		tracker.setProgressMessage(message);
		if (!LooseContext.is(CONTEXT_REUSE_CURRENT_TRACKER)) {
			tracker.setEndTime(new Date());
			tracker.setJobResultType(resultType);
			tracker.setComplete(true);
		}
		AlcinaTopics.jobComplete(tracker);
		logComplete(tracker, message);
		if (!LooseContext.is(CONTEXT_REUSE_CURRENT_TRACKER)) {
			removeTracker(tracker);
			if (tracker.getParent() != null) {
				tracker.getParent().childComplete(tracker);
			}
		}
		popContextTracker(tracker);
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
		if (current == null) {
			return;
		}
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

	protected JobTracker createJobTracker(JobId jobId) {
		return new JobTrackerImpl(jobId.toString());
	}

	protected String flushLogger(Logger contextLogger) {
		if (contextLogger instanceof RemoteActionLogger) {
			return ((RemoteActionLogger) contextLogger).flushLogger();
		}
		return null;
	}

	protected void jobError(JobTracker tracker, Exception ex,
			boolean logException) {
		if (logException) {
			warn(ex);
			if (tracker == null) {
				return;
			}
			ex.printStackTrace();
		}
		String jobResult = "Job failed: " + ex.toString();
		tracker.setJobException(ex);
		jobComplete(tracker, JobResultType.FAIL, jobResult);
		notifyJobFailure(tracker);
	}
}

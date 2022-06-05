package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.csobjects.JobTracker;

public class AlcinaTopics {
	public static final String LOG_CATEGORY_MESSAGE = "message";

	public static final String LOG_CATEGORY_EXCEPTION = "exception";

	public static final String LOG_CATEGORY_TRANSFORM = "transform";

	public static final String LOG_CATEGORY_HISTORY = "history";

	public static final String LOG_CATEGORY_STAT = "stat";

	public static final String LOG_CATEGORY_CLICK = "click";

	public static final String LOG_CATEGORY_METRIC = "metric";

	public static final String LOG_CATEGORY_CHANGE = "change";

	public static final String LOG_CATEGORY_COMMENT = "comment";

	public static final String LOG_CATEGORY_CONTAINER = "container";

	public static final Topic<String> logMessage = Topic.create();

	public static final Topic<StringPair> categorisedLogMessage = Topic
			.create();

	public static final Topic<Throwable> localPersistenceException = Topic
			.create();

	public static final Topic<String> categoryMetric = Topic.create();

	public static final Topic<Boolean> muteStatisticsLogging = Topic.create();

	public static final Topic<Exception> devWarning = Topic.create();

	public static final Topic<Exception> transformCascadeException = Topic
			.create();

	public static final Topic<Boolean> applicationReadonly = Topic.create();

	public static final Topic<Boolean> applicationRestart = Topic.create();

	public static final Topic<JobTracker> jobCompletion = Topic.create();

	public static void log(Object object) {
		logMessage.publish(object.toString());
	}
}

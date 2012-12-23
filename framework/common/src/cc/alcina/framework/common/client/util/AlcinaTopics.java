package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.logic.MessageManager;

public class AlcinaTopics {
	public static final String LOG_CATEGORY_MESSAGE = "message";

	public static final String LOG_CATEGORY_EXCEPTION = "exception";

	public static final String LOG_CATEGORY_TRANSFORM = "transform";

	public static final String LOG_CATEGORY_HISTORY = "history";

	public static final String LOG_CATEGORY_CLICK = "click";

	public static final String TOPIC_LOG_MESSAGE_PUBLISHED = AlcinaTopics.class
			.getName() + ".TOPIC_LOG_MESSAGE_PUBLISHED";

	public static final String TOPIC_LOG_CATEGORISED_MESSAGE_PUBLISHED = AlcinaTopics.class
			.getName() + ".TOPIC_LOG_CATEGORISED_MESSAGE_PUBLISHED";

	public static final String TOPIC_LOCAL_PERSISTENCE_EXCEPTION = AlcinaTopics.class
			.getName() + ".TOPIC_LOCAL_PERSISTENCE_EXCEPTION";

	public static final String LOG_CATEGORY_METRIC = "metric";

	public static final String TOPIC_MUTE_STAT_LOGGING = AlcinaTopics.class
			.getName() + ".TOPIC_MUTE_STAT_LOGGING";;

	// detach logging from presentation (normally ClientNotifications)
	public static void log(Object message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_LOG_MESSAGE_PUBLISHED,
				String.valueOf(message));
	}

	public static void logListenerDelta(TopicListener<String> listener,
			boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_LOG_MESSAGE_PUBLISHED,
				listener, add);
	}

	public static void logCategorisedMessage(StringPair categoryAndMessage) {
		GlobalTopicPublisher.get().publishTopic(
				TOPIC_LOG_CATEGORISED_MESSAGE_PUBLISHED, categoryAndMessage);
	}

	public static void logCategorisedMessageListenerDelta(
			TopicListener<StringPair> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(
				TOPIC_LOG_CATEGORISED_MESSAGE_PUBLISHED, listener, add);
	}

	public static void localPersistenceException(
			Throwable localPersistenceException) {
		GlobalTopicPublisher.get().publishTopic(
				TOPIC_LOCAL_PERSISTENCE_EXCEPTION, localPersistenceException);
	}

	public static void localPersistenceExceptionListenerDelta(
			TopicListener<Throwable> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(
				TOPIC_LOCAL_PERSISTENCE_EXCEPTION, listener, add);
	}
	public static void muteStatisticsLogging(
			boolean mute) {
		GlobalTopicPublisher.get().publishTopic(
				TOPIC_MUTE_STAT_LOGGING, mute);
	}

	public static void muteStatisticsLoggingListenerDelta(
			TopicListener<Boolean> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(
				TOPIC_MUTE_STAT_LOGGING, listener, add);
	}
}

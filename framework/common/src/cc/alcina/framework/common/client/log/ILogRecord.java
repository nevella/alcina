package cc.alcina.framework.common.client.log;

import java.util.Date;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

public interface ILogRecord extends HasId {
	public static final String COMPONENT_KEY = "componentKey";

	public static final String USER_ID = "userId";

	public static final String TOPIC_PERSISTENT_LOG_EVENT_OCCURRED = ILogRecord.class
			.getName() + "." + "TOPIC_PERSISTENT_LOG_EVENT_OCCURRED";

	public static Topic<ILogRecord> topicPersistentLog() {
		return Topic.global(TOPIC_PERSISTENT_LOG_EVENT_OCCURRED);
	}

	Long getClientInstanceId();

	String getComponentKey();

	Date getCreatedOn();

	String getData();

	String getHost();

	String getIpAddress();

	String getText();

	Long getUserId();

	String getUserName();

	void setClientInstanceId(Long clientInstanceId);

	void setComponentKey(String componentKey);

	void setCreatedOn(Date createdOn);

	void setData(String data);

	void setHost(String host);

	void setIpAddress(String ipAddress);

	void setText(String text);

	void setUserId(Long userId);

	void setUserName(String userName);
}
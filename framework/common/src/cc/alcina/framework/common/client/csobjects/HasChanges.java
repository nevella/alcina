package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.util.Topic;

public interface HasChanges {
	Topic<Void> topicChanged();
}

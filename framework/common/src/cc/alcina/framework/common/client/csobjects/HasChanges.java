package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.util.Topic;

// Legacy UI pattern (for marking an object, not a specific property, as
// changed)
public interface HasChanges {
	Topic<Void> topicChanged();
}

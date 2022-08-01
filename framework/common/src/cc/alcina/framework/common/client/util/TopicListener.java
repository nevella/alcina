package cc.alcina.framework.common.client.util;

@FunctionalInterface
public interface TopicListener<T> {
	void topicPublished(T message);
}
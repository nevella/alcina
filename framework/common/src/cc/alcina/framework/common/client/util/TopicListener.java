package cc.alcina.framework.common.client.util;

@FunctionalInterface
public interface TopicListener<T> {
	void topicPublished(T message);

	class Reference {
		private TopicListener listener;

		private Topic topic;

		public Reference(Topic topic, TopicListener listener) {
			this.topic = topic;
			this.listener = listener;
		}

		public void remove() {
			topic.remove(listener);
		}

		public void removeOnFire() {
			topic.remove(listener);
			TopicListener wrapper = new TopicListener() {
				@Override
				public void topicPublished(Object o) {
					listener.topicPublished(o);
					topic.remove(this);
				}
			};
			topic.add(wrapper);
		}
	}
}
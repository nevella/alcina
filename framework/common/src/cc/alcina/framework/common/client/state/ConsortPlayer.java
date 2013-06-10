package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public interface ConsortPlayer {
	public Consort getStateConsort();

	public static class SubconsortSupport {
		public void run(final Consort consort, Consort subConsort,
				final Player player) {
			TopicListener listener = new TopicListener() {
				@Override
				public void topicPublished(String key, Object message) {
					consort.wasPlayed(player);
				}
			};
			subConsort.listenerDelta(Consort.FINISHED, listener, true);
			subConsort.start();
		}
	}
}

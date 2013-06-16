package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public interface ConsortPlayer {
	public Consort getStateConsort();

	public static class SubconsortSupport {
		TopicListener listener = new TopicListener() {
			@Override
			public void topicPublished(final String key, final Object message) {
				subConsort.deferredRemove(Consort.FINISHED, listener);
				subConsort.deferredRemove(Consort.ERROR, listener);
				if (key == Consort.ERROR) {
					player.onFailure((Throwable) message);
				} else {
					player.wasPlayed();
				}
			}
		};


		private Player player;

		private Consort subConsort;

		public void run(Consort consort, Consort subConsort, Player player) {
			this.subConsort = subConsort;
			this.player = player;
			player.setAsynchronous(true);
			subConsort.setParent(consort);
			subConsort.listenerDelta(Consort.FINISHED, listener, true);
			subConsort.listenerDelta(Consort.ERROR, listener, true);
			subConsort.restart();
		}
	}
}

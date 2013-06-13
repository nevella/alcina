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
				// TODO - we might be able to queue this in the parent consort,
				// rather than having to timer it
				Registry.impl(TimerWrapperProvider.class)
						.getTimer(new Runnable() {
							@Override
							public void run() {
								signalFinished(key, message);
							}
						}).scheduleSingle(1);
			}
		};

		void signalFinished(String key, Object message) {
			subConsort.listenerDelta(Consort.FINISHED, listener, false);
			subConsort.listenerDelta(Consort.ERROR, listener, false);
			if (key == Consort.ERROR) {
				player.onFailure((Throwable) message);
			} else {
				player.wasPlayed();
			}
		}

		private Player player;

		private Consort subConsort;

		public void run(Consort consort, Consort subConsort, Player player) {
			this.subConsort = subConsort;
			this.player = player;
			player.setAsynchronous(true);
			subConsort.listenerDelta(Consort.FINISHED, listener, true);
			subConsort.listenerDelta(Consort.ERROR, listener, true);
			subConsort.restart();
		}
	}
}

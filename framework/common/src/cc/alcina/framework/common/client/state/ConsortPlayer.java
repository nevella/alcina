package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public interface ConsortPlayer {
	public Consort getStateConsort();

	public static class SubconsortSupport {
		Runnable remover = new Runnable() {
			@Override
			public void run() {
				player.wasPlayed();
				subConsort.listenerDelta(Consort.FINISHED, listener, false);
			}
		};

		TopicListener listener = new TopicListener() {
			@Override
			public void topicPublished(String key, Object message) {
				// TODO - we might be able to queue this in the parent consort,
				// rather than having to timer it
				Registry.impl(TimerWrapperProvider.class).getTimer(remover)
						.scheduleSingle(1);
			}
		};

		private Player player;

		private Consort subConsort;

		public void run(Consort consort, Consort subConsort, Player player) {
			this.subConsort = subConsort;
			this.player = player;
			player.setAsynchronous(true);
			subConsort.listenerDelta(Consort.FINISHED, listener, true);
			subConsort.restart();
		}
	}
}

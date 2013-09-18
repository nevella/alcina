package cc.alcina.framework.common.client.state;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
					if (fireEndState) {
						if (stateToFireAfterConsortEnd != null) {
							player.wasPlayed(stateToFireAfterConsortEnd);
						} else {
							player.wasPlayed();
						}
					}
				}
			}
		};

		private Player player;

		private Consort subConsort;

		private Object stateToFireAfterConsortEnd;

		private boolean fireEndState = true;

		public void run(Consort consort, Consort subConsort, Player player) {
			run(consort, subConsort, player, null);
		}

		public void run(Consort consort, Consort subConsort, Player player,
				Object stateToFireAfterConsortEnd) {
			this.subConsort = subConsort;
			this.player = player;
			this.stateToFireAfterConsortEnd = stateToFireAfterConsortEnd;
			player.setAsynchronous(true);
			subConsort.setParentConsort(consort);
			consort.passLoggersAndFlagsToChild(subConsort);
			subConsort.listenerDelta(Consort.FINISHED, listener, true);
			subConsort.listenerDelta(Consort.ERROR, listener, true);
			subConsort.restart();
		}

		public void maybeAttach(AsyncCallback callback,
				Consort maybeChildConsort, boolean fireEndStateIfChild) {
			if (callback instanceof Player) {
				Player player = (Player) callback;
				fireEndState = fireEndStateIfChild;
				run(player.getConsort(), maybeChildConsort, player);
			} else {
				maybeChildConsort.start();
			}
		}
	}
}

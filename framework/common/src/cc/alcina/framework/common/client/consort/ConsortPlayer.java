package cc.alcina.framework.common.client.consort;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.TopicListener;

public interface ConsortPlayer {
	public Consort getStateConsort();

	public static class SubconsortSupport {
		TopicListener listener = new TopicListener() {
			@Override
			public void topicPublished(final Object message) {
				Object channel = subConsort.getFiringTopicChannel();
				subConsort.exitListenerDelta(listener, false, false);
				if (channel == Consort.TopicChannel.ERROR) {
					player.onFailure((Throwable) message);
				} else {
					if (fireEndState) {
						try {
							LooseContext.pushWithKey(
									Consort.IGNORE_PLAYED_STATES_IF_NOT_CONTAINED,
									true);
							if (stateToFireAfterConsortEnd != null) {
								player.wasPlayed(stateToFireAfterConsortEnd);
							} else {
								player.wasPlayed();
							}
						} finally {
							LooseContext.pop();
						}
					}
				}
			}
		};

		private Player player;

		private Consort subConsort;

		private Object stateToFireAfterConsortEnd;

		private boolean fireEndState = true;

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
			subConsort.exitListenerDelta(listener, false, true);
			subConsort.restart();
		}
	}
}

package cc.alcina.framework.servlet.environment.replay;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.console.rcs.Feature_RomcomSessionConsole;

@Feature.Ref(Feature_RomcomSessionConsole._Replay.class)
public class SessionReplay {
	public Topic<Status> topicStatusChange = Topic.create();

	public static class Status {
		public int currentEventId;

		public State state = State.pending;
	}

	public enum State {
		pending, replaying, finished;
	}
}

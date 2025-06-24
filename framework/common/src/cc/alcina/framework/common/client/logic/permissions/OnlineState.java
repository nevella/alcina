package cc.alcina.framework.common.client.logic.permissions;

import cc.alcina.framework.common.client.util.Topic;

public enum OnlineState {
	OFFLINE, ONLINE;

	private static OnlineState state;

	public static final Topic<OnlineState> topicOnlineStateChange = Topic
			.create();

	public static OnlineState get() {
		return state;
	}

	public static void set(OnlineState state) {
		OnlineState oldState = OnlineState.state;
		OnlineState.state = state;
		if (state != oldState) {
			topicOnlineStateChange.publish(state);
		}
	}

	public static boolean isOffline() {
		return get() == OFFLINE;
	}

	public static boolean isOnline() {
		return !isOffline();
	}
}
package cc.alcina.framework.common.client.logic.permissions;

public enum OnlineState {
	OFFLINE, ONLINE;

	public static OnlineState get() {
		return Permissions.get().onlineState;
	}

	public static void set(OnlineState state) {
		OnlineState oldState = get();
		Permissions.get().onlineState = state;
		if (oldState != state) {
			Permissions.topicOnlineStateChange().publish(state);
		}
	}

	public static boolean isOffline() {
		return get() == OFFLINE;
	}

	public static boolean isOnline() {
		return !isOffline();
	}
}
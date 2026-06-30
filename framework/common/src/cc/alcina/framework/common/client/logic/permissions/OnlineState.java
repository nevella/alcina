package cc.alcina.framework.common.client.logic.permissions;

public enum OnlineState {
	OFFLINE, ONLINE;

	private static OnlineState state;

	public static OnlineState get() {
		return state;
	}

	public static void set(OnlineState state) {
		OnlineState oldState = OnlineState.state;
		OnlineState.state = state;
	}

	public static boolean isOffline() {
		return get() == OFFLINE;
	}

	public static boolean isOnline() {
		return !isOffline();
	}
}
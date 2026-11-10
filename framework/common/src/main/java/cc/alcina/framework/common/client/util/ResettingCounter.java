package cc.alcina.framework.common.client.util;

public class ResettingCounter {
	private long lastTrigger = 0;

	private long triggerMs;

	public ResettingCounter(long triggerMs) {
		this.triggerMs = triggerMs;
	}

	public boolean check() {
		long now = System.currentTimeMillis();
		if (now - lastTrigger > triggerMs) {
			lastTrigger = now;
			return true;
		} else {
			return false;
		}
	}
}

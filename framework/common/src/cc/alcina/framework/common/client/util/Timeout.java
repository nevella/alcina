package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.context.LooseContext;

public class Timeout {
	static LooseContext.Key<Timeout> CONTEXT_TIMEOUT = LooseContext
			.key(Timeout.class, "CONTEXT_TIMEOUT");

	/**
	 * 
	 * @return true (for use in a process) if timed out
	 */
	public static boolean isContextTimedOut() {
		Timeout timeout = CONTEXT_TIMEOUT.getTyped();
		if (timeout != null) {
			return !timeout.check(false);
		} else {
			return false;
		}
	}

	int timeout;

	long start;

	/**
	 * 
	 * @param timeout
	 *            The timeout in milliseconds
	 */
	public Timeout(int timeout) {
		this.timeout = timeout;
		this.start = System.currentTimeMillis();
	}

	/**
	 * 
	 * @param throwOnTimeout
	 *            if a timeout should throw
	 * @return true (for use in a while loop) to continue, false if timed out
	 *         and not throwontimeout
	 */
	public boolean check(boolean throwOnTimeout) {
		if (remaining() < 0) {
			if (throwOnTimeout) {
				throw new IllegalStateException("Timed out");
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public long remaining() {
		return start + timeout - System.currentTimeMillis();
	}

	/**
	 * Force a timeout (signal) on the next check
	 * 
	 * @return this, a Timeout
	 */
	public Timeout withTimeoutOnNextCheck() {
		this.start = 0;
		return this;
	}

	/**
	 * When using a timeout as a debouncer, reset the timeout (to now) if it
	 * times out
	 * 
	 * @return false if timed out
	 */
	public boolean checkAndReset() {
		boolean result = check(false);
		if (!result) {
			this.start = 0;
		}
		return result;
	}

	public static void startContextTimeout(int timeout) {
		CONTEXT_TIMEOUT.set(new Timeout(timeout));
	}
}
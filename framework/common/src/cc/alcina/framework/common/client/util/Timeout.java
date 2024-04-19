package cc.alcina.framework.common.client.util;

public class Timeout {
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
	 * @param b
	 * @return true (for use in a while loop) if timed out and not
	 *         throwontimeout
	 */
	public boolean check(boolean throwOnTimeout) {
		if (remaining() < 0) {
			if (throwOnTimeout) {
				throw new IllegalStateException("Timed out");
			} else {
				return true;
			}
		} else {
			return false;
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
	 * @return true if timed out
	 */
	public boolean checkAndReset() {
		boolean result = check(false);
		if (result) {
			this.start = 0;
		}
		return result;
	}
}
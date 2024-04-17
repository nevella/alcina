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
	 * @return true (for use in a while loop) if not timed out, otherwise throws
	 *         an exception
	 */
	public boolean check() {
		if (remaining() < 0) {
			throw new IllegalStateException("Timed out");
		}
		return true;
	}

	public long remaining() {
		return start + timeout - System.currentTimeMillis();
	}
}
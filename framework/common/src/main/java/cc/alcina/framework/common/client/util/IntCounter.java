package cc.alcina.framework.common.client.util;

public class IntCounter {
	private int counter;

	private boolean reversed;

	public IntCounter() {
	}

	public IntCounter(boolean reversed) {
		this.reversed = reversed;
	}

	public IntCounter(int initialValue) {
		this.counter = initialValue;
	}

	public synchronized int nextId() {
		return reversed ? --counter : ++counter;
	}
}

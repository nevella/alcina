package cc.alcina.framework.common.client.util;

public class IdCounter {
	private long counter;

	private boolean reversed;

	public IdCounter() {
	}

	public IdCounter(boolean reversed) {
		this.reversed = reversed;
	}

	public IdCounter(long initialValue) {
		this.counter = initialValue;
	}

	public synchronized long nextId() {
		return reversed ? --counter : ++counter;
	}
}

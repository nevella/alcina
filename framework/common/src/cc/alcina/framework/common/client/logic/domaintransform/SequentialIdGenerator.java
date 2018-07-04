package cc.alcina.framework.common.client.logic.domaintransform;

public class SequentialIdGenerator {
	public static long id;

	public synchronized long decrementAndGet() {
		return --id;
	}

	public long get() {
		return id;
	}

	public synchronized long incrementAndGet() {
		return ++id;
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public void set(long value) {
		id = value;
	}
}
package cc.alcina.framework.common.client.logic.domaintransform;

public class SequentialIdGenerator {
	public static long id;

	private long maxValue = Long.MAX_VALUE;

	public SequentialIdGenerator() {
	}

	public SequentialIdGenerator(long maxValue) {
		this.maxValue = maxValue;
	}

	public synchronized long decrementAndGet() {
		return --id;
	}

	public long get() {
		return id;
	}

	public synchronized long incrementAndGet() {
		long value = ++id;
		if (value >= maxValue) {
			throw new IllegalStateException("Counter exceeds maxvalue");
		}
		return value;
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public void set(long value) {
		id = value;
	}
}
package cc.alcina.framework.common.client.logic.domaintransform;

import com.google.common.base.Preconditions;

public class SequentialIdGenerator {
	private long id;

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

	public synchronized int incrementAndGetInt() {
		long value = incrementAndGet();
		Preconditions.checkState(value < Integer.MAX_VALUE);
		return (int) value;
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
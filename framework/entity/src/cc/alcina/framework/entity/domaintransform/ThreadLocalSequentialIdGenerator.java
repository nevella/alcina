package cc.alcina.framework.entity.domaintransform;

import java.util.concurrent.atomic.AtomicLong;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;

public class ThreadLocalSequentialIdGenerator extends SequentialIdGenerator {
	// using AtomicInteger just as a counter
	private static ThreadLocal<AtomicLong> threadCounters = new ThreadLocal() {
		protected synchronized AtomicLong initialValue() {
			return new AtomicLong(0);
		}
	};

	public synchronized long incrementAndGet() {
		return threadCounters.get().incrementAndGet();
	}

	public void reset(AtomicLong counter) {
		threadCounters.set(counter);
	}

	@Override
	public void reset() {
		threadCounters.get().set(0);
	}
}
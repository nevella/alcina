package cc.alcina.framework.entity.transform;

import java.util.concurrent.atomic.AtomicLong;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;

public class AtomicSequentialIdGenerator extends SequentialIdGenerator {
	private AtomicLong counter = new AtomicLong(0);

	@Override
	public synchronized long decrementAndGet() {
		return counter.decrementAndGet();
	}

	@Override
	public synchronized long incrementAndGet() {
		return counter.incrementAndGet();
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}
}
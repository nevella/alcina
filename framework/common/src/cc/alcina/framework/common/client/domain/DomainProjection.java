package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * Domain projections do not project when returning results - detached cloning
 * responsibility of calling code
 * 
 * @author nick@alcina.cc
 * 
 * @param <T>
 */
public interface DomainProjection<T extends Entity>
		extends DomainListener<T>, MemoryStatProvider {
	@Override
	default MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !self.objectMemory.isMemoryStatProvider(o.getClass()));
		return self;
	}

	default boolean isCommitOnly() {
		return false;
	}

	default boolean isDerived() {
		return false;
	}
}

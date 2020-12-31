package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;

/**
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

	/*
	 * entityCollation will be null unless in a post-process index - so should
	 * only used for implementations where isCommitOnly() returns true
	 */
	default boolean isIgnoreForIndexing(EntityCollation entityCollation) {
		return false;
	}
}

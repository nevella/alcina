package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

/**
 * Domain projections do not project when returning results - detached cloning
 * responsibility of calling code
 * 
 * @author nick@alcina.cc
 * 
 * @param <T>
 */
public interface DomainProjection<T extends HasIdAndLocalId>
		extends DomainListener<T>, MemoryStatProvider {
	@Override
	default MemoryStat addMemoryStats(MemoryStat parent, StatType type) {
		throw new UnsupportedOperationException();
	}

	default boolean isDerived() {
		return false;
	}
}

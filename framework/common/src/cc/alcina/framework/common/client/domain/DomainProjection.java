package cc.alcina.framework.common.client.domain;

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
		extends DomainListener<T> {
	default boolean isDerived() {
		return false;
	}
}

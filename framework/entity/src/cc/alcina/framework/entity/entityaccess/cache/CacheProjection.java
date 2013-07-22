package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

/**
 * Cache projections do not project when returning results - detached cloning
 * responsibility of calling code
 * 
 * @author nick@alcina.cc
 * 
 * @param <T>
 */
public interface CacheProjection<T extends HasIdAndLocalId> extends
		CacheListener<T> {
}

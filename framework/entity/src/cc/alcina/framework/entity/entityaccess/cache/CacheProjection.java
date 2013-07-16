package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface CacheProjection<T extends HasIdAndLocalId> extends CacheListener<T> {
}

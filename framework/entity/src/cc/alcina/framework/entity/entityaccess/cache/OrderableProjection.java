package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface OrderableProjection<T extends HasIdAndLocalId> {
	Collection<T> order(int count, CollectionFilter<T> filter,
			boolean targetsOfFinalKey, boolean reverse,
			boolean finishAfterFirstFilterFail, Object... objects);
}

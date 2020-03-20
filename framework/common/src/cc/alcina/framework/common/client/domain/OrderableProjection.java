package cc.alcina.framework.common.client.domain;

import java.util.Collection;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;

public interface OrderableProjection<T extends Entity> {
	Collection<T> order(int count, CollectionFilter<T> filter,
			boolean targetsOfFinalKey, boolean reverse,
			boolean finishAfterFirstFilterFail, Object... objects);
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

public class MemCacheTransformFilter
		implements Predicate<DomainTransformEvent> {
	@Override
	public boolean test(DomainTransformEvent t) {
		// TODO Auto-generated method stub
		return false;
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.KeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;

public class DteToLocatorMapper implements
		KeyValueMapper<HiliLocator, DomainTransformEvent, DomainTransformEvent> {
	@Override
	public HiliLocator getKey(DomainTransformEvent dte) {
		return HiliLocator.objectLocator(dte);
	}

	@Override
	public DomainTransformEvent getValue(DomainTransformEvent dte) {
		return dte;
	}
}

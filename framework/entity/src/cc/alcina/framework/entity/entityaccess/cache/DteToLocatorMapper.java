package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.KeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager.HiliLocator;

public class DteToLocatorMapper implements
		KeyValueMapper<HiliLocator, DomainTransformEvent, DomainTransformEvent> {
	@Override
	public HiliLocator getKey(DomainTransformEvent dte) {
		return HiliLocator.fromDte(dte);
	}

	@Override
	public DomainTransformEvent getValue(DomainTransformEvent dte) {
		return dte;
	}
}

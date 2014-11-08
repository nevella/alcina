package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.KeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;

public class DteToLocatorMapper implements
		KeyValueMapper<HiliLocator, DomainTransformEvent, DomainTransformEvent> {
	private boolean valueLocator;

	public DteToLocatorMapper() {
		this(false);
	}
	public DteToLocatorMapper(boolean valueLocator) {
		this.valueLocator = valueLocator;
		
	}
	@Override
	public HiliLocator getKey(DomainTransformEvent dte) {
		return valueLocator?HiliLocator.valueLocator(dte):HiliLocator.objectLocator(dte);
	}

	@Override
	public DomainTransformEvent getValue(DomainTransformEvent dte) {
		return dte;
	}
}

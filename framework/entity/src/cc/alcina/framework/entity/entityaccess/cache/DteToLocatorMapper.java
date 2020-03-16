package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.KeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;

public class DteToLocatorMapper implements
		KeyValueMapper<EntityLocator, DomainTransformEvent, DomainTransformEvent> {
	private boolean valueLocator;

	public DteToLocatorMapper() {
		this(false);
	}

	public DteToLocatorMapper(boolean valueLocator) {
		this.valueLocator = valueLocator;
	}

	@Override
	public EntityLocator getKey(DomainTransformEvent dte) {
		return valueLocator ? EntityLocator.valueLocator(dte)
				: EntityLocator.objectLocator(dte);
	}

	@Override
	public DomainTransformEvent getValue(DomainTransformEvent dte) {
		return dte;
	}
}

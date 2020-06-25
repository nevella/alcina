package cc.alcina.framework.entity.entityaccess.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;

//FIXME - mvcc.3 - remove
class SourceMetadataProvider extends DomainModificationMetadataProvider {
	Map<EntityLocator, Entity> locatorOriginalSourceMap = new LinkedHashMap<EntityLocator, Entity>();

	public SourceMetadataProvider(DomainStore store) {
		super(store);
	}

	@Override
	public void registerTransforms(List<DomainTransformEvent> transforms) {
		for (DomainTransformEvent dte : transforms) {
			EntityLocator locator = EntityLocator.objectLocator(dte);
			locatorOriginalSourceMap.put(locator, dte.getSource());
		}
	}

	@Override
	public void updateMetadata(DomainTransformEvent dte,
			Entity domainStoreObject) {
		// dte source is clobbered by early precache/preload - this'll probably
		// go away (why does preload need source?) but using this model for now
		Entity dbObj = locatorOriginalSourceMap
				.get(EntityLocator.objectLocator(dte));
		if (domainStoreObject instanceof HasVersionNumber) {
			updateVersionNumber(domainStoreObject, dbObj);
		}
		if (domainStoreObject instanceof IVersionable) {
			updateIVersionable(domainStoreObject, dbObj);
		}
	}
}
